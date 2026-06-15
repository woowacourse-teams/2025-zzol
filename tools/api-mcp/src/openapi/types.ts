import { z } from 'zod';

export const OpenApiSpecSchema = z.object({
  openapi: z.string().optional(),
  info: z.object({
    title: z.string(),
    version: z.string(),
    description: z.string().optional(),
  }),
  paths: z.record(z.record(z.unknown())).default({}),
  components: z
    .object({
      schemas: z.record(z.unknown()).optional(),
    })
    .optional(),
});

export type OpenApiSpec = z.infer<typeof OpenApiSpecSchema>;

export interface OperationObject {
  operationId?: string;
  summary?: string;
  description?: string;
  tags?: string[];
  parameters?: unknown[];
  requestBody?: unknown;
  responses?: Record<string, unknown>;
  deprecated?: boolean;
}

export interface SchemaObject {
  type?: string | string[]; // OpenAPI 3.1: type 배열 가능 (예: ["string", "null"])
  format?: string;
  description?: string;
  properties?: Record<string, SchemaObject>;
  required?: string[];
  items?: SchemaObject;
  $ref?: string;
  enum?: unknown[];
  nullable?: boolean;
  allOf?: SchemaObject[];
  oneOf?: SchemaObject[];
  anyOf?: SchemaObject[];
  additionalProperties?: boolean | SchemaObject;
}
