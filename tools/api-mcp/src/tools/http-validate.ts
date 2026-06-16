import { z } from 'zod';
import type { ToolDefinition, ToolContext } from './types.js';
import { ok, fail } from './types.js';
import type { OperationObject, SchemaObject } from '../openapi/types.js';

const HTTP_METHODS = ['get', 'post', 'put', 'delete', 'patch', 'head', 'options'] as const;

const InputSchema = z.object({
  method: z.string(),
  path: z.string(),
  requestBody: z.unknown().optional(),
  responseBody: z.unknown().optional(),
  responseStatus: z.number().default(200),
});

interface ValidationError {
  path: string;
  message: string;
}

// 체인된 $ref도 완전히 해소 (순환 참조 가드 포함)
function resolveSchema(
  schema: SchemaObject,
  components: Record<string, unknown> | undefined
): SchemaObject {
  let current = schema;
  const seen = new Set<string>();
  while (current.$ref && components) {
    if (seen.has(current.$ref)) break;
    seen.add(current.$ref);
    const match = /^#\/components\/schemas\/(.+)$/.exec(current.$ref);
    if (!match?.[1]) break;
    current = (components[match[1]] as SchemaObject | undefined) ?? current;
  }
  return current;
}

function validate(
  value: unknown,
  schema: SchemaObject,
  path: string,
  components: Record<string, unknown> | undefined,
  errors: ValidationError[]
): void {
  const resolved = resolveSchema(schema, components);

  // nullable 처리 (OpenAPI 3.0: nullable:true / 3.1: type 배열에 "null" 포함)
  const types = Array.isArray(resolved.type) ? resolved.type : resolved.type ? [resolved.type] : [];
  const allowsNull = resolved.nullable === true || types.includes('null');
  if (allowsNull && value === null) return;

  if (value === null || value === undefined) {
    errors.push({ path, message: 'null/undefined 입니다' });
    return;
  }

  if (resolved.allOf) {
    for (const sub of resolved.allOf) validate(value, sub, path, components, errors);
    return;
  }

  // 빈 배열은 유효한 분기 없음으로 간주하지 않고 건너뜀
  const variants = resolved.oneOf ?? resolved.anyOf;
  if (variants && variants.length > 0) {
    const anyValid = variants.some((sub) => {
      const tmp: ValidationError[] = [];
      validate(value, sub, path, components, tmp);
      return tmp.length === 0;
    });
    if (!anyValid) errors.push({ path, message: 'oneOf/anyOf 중 매칭되는 스키마가 없습니다' });
    return;
  }

  const actualType = Array.isArray(value) ? 'array' : typeof value;

  if (types.length > 0) {
    // null 제외한 타입으로 비교, integer는 number로 정규화
    const nonNullTypes = types
      .filter((t) => t !== 'null')
      .map((t) => (t === 'integer' ? 'number' : t));
    if (nonNullTypes.length > 0 && !nonNullTypes.includes(actualType)) {
      errors.push({
        path,
        message: `타입 불일치: 기대=${types.join('|')}, 실제=${actualType}`,
      });
      return;
    }
  }

  if (resolved.enum && !resolved.enum.includes(value)) {
    errors.push({
      path,
      message: `허용되지 않는 값: ${JSON.stringify(value)}, 허용값=[${resolved.enum.map((v) => JSON.stringify(v)).join(', ')}]`,
    });
  }

  if (resolved.properties || types.includes('object') || resolved.type === 'object') {
    const obj = value as Record<string, unknown>;
    for (const req of resolved.required ?? []) {
      if (!(req in obj)) errors.push({ path: `${path}.${req}`, message: 'required 필드 누락' });
    }
    if (resolved.properties) {
      for (const [k, subSchema] of Object.entries(resolved.properties)) {
        if (k in obj) validate(obj[k], subSchema, `${path}.${k}`, components, errors);
      }
      // additionalProperties: false 이면 스키마에 없는 키 리포트
      if (resolved.additionalProperties === false) {
        for (const k of Object.keys(obj)) {
          if (!(k in resolved.properties)) {
            errors.push({ path: `${path}.${k}`, message: '스키마에 정의되지 않은 필드' });
          }
        }
      }
    }
  }

  if (
    (types.includes('array') || resolved.type === 'array' || Array.isArray(value)) &&
    resolved.items
  ) {
    const items = resolved.items;
    const arr = value as unknown[];
    arr.forEach((item, i) => {
      validate(item, items, `${path}[${String(i)}]`, components, errors);
    });
  }
}

export const httpValidateTool: ToolDefinition = {
  name: 'http_validate',
  description:
    'HTTP 요청/응답 바디를 OpenAPI 스키마와 대조하여 누락 필드·타입 불일치를 리포트합니다.',
  inputSchema: {
    type: 'object',
    properties: {
      method: { type: 'string', description: 'HTTP 메서드 (예: POST)' },
      path: { type: 'string', description: 'API 경로 (예: /api/rooms)' },
      requestBody: { description: '검증할 요청 바디' },
      responseBody: { description: '검증할 응답 바디' },
      responseStatus: { type: 'number', default: 200, description: '응답 상태코드' },
    },
    required: ['method', 'path'],
  },
  async handler(args: Record<string, unknown>, ctx: ToolContext) {
    const parsed = InputSchema.safeParse(args);
    if (!parsed.success) return fail(parsed.error.message);

    let spec;
    try {
      spec = await ctx.openapi.load();
    } catch (error) {
      return fail(
        `OpenAPI spec 로드 실패: ${error instanceof Error ? error.message : String(error)}`
      );
    }

    const { method, path, requestBody, responseBody, responseStatus } = parsed.data;

    const m = method.toLowerCase();
    if (!(HTTP_METHODS as readonly string[]).includes(m)) {
      return fail(`지원하지 않는 메서드: ${method.toUpperCase()}`);
    }

    const pathItem = spec.paths[path];
    if (!pathItem) return fail(`경로를 찾을 수 없습니다: ${path}`);

    const op = pathItem[m] as OperationObject | undefined;
    if (!op) return fail(`${method.toUpperCase()} ${path} 를 찾을 수 없습니다`);

    const components = spec.components?.schemas;
    const requestErrors: ValidationError[] = [];
    const responseErrors: ValidationError[] = [];

    if (requestBody !== undefined && op.requestBody) {
      interface RequestBodyDef {
        content?: Record<string, { schema?: SchemaObject }>;
      }
      const rb = op.requestBody as RequestBodyDef;
      const jsonSchema = rb.content?.['application/json']?.schema;
      if (jsonSchema) validate(requestBody, jsonSchema, 'requestBody', components, requestErrors);
    }

    if (responseBody !== undefined && op.responses) {
      interface ResponseDef {
        content?: Record<string, { schema?: SchemaObject }>;
      }
      const responses = op.responses as Record<string, ResponseDef>;
      const respDef = responses[String(responseStatus)] ?? responses.default;
      const jsonSchema = respDef?.content?.['application/json']?.schema;
      if (jsonSchema)
        validate(responseBody, jsonSchema, 'responseBody', components, responseErrors);
    }

    return ok({
      valid: requestErrors.length === 0 && responseErrors.length === 0,
      requestErrors,
      responseErrors,
    });
  },
};
