import { readdirSync, readFileSync, statSync } from 'fs';
import { join, relative, resolve } from 'path';
import ts from 'typescript';
import { isBrokerDestination, WEBSOCKET_CONFIG } from '../constants/constants';

/**
 * FE 가 쓰는 destination 이 BE 카탈로그에 실재하는지 검사한다.
 *
 * STOMP 는 존재하지 않는 토픽도 구독에 성공하고, publish 는 ack 이 없어 브로커가 조용히 버린다.
 * 그래서 오타나 삭제된 destination 이 빌드·린트·런타임 어디에도 안 걸리고 사용자만 화면이
 * 안 넘어가는 걸 본다. 이 테스트가 그 구멍을 막는다.
 *
 * SSOT 는 BE 가 생성해 커밋하는 ws-catalog.json 이다. 신선도는 backend-ci 가 강제한다.
 */

const REPO_ROOT = resolve(__dirname, '../../../../..');
const CATALOG_PATH = join(REPO_ROOT, 'backend/app/src/test/resources/__fixtures__/ws-catalog.json');
const SRC_ROOT = resolve(__dirname, '../../..');

/** 이 이름으로 호출되면 첫 인자를 destination 으로 본다. */
const SUBSCRIBE_HOOKS = ['useWebSocketSubscription', 'useUserSocketSubscription'];
const SEND_FN = 'send';

/** 판정할 수 없는 호출부를 의도적으로 넘길 때 붙이는 표식. */
const IGNORE_MARK = 'ws-contract-ignore';

type Catalog = {
  topics: { path: string }[];
  queues: { path: string }[];
  sends: { destination: string }[];
  errors: { topic: string };
};

type Usage = {
  kind: 'subscribe' | 'send';
  raw: string | null;
  where: string;
  ignored: boolean;
};

/**
 * 경로 변수 표기를 한 자리표시자로 접는다. FE 의 `${joinCode}` 와 BE 의 `{joinCode}` 가 같은
 * `{}` 가 되어 이름이 달라도 비교할 수 있다. 파라미터 이름은 로컬 바인딩일 뿐이라 대조하지 않는다.
 * BE 의 WsCatalogContractTest.normalize 와 같은 규칙이다.
 */
const normalize = (path: string): string =>
  path
    .split('/')
    .map((segment) => (segment.includes('{') ? '{}' : segment))
    .join('/');

const collectSourceFiles = (dir: string): string[] =>
  readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) return collectSourceFiles(full);
    return /\.tsx?$/.test(entry) ? [full] : [];
  });

/**
 * 첫 인자를 정적 문자열로 접는다. 보간은 값을 알 수 없으므로 `{}` 자리표시자로 남긴다.
 * 식별자나 함수 호출처럼 접을 수 없으면 null 을 돌려 "판정 불가"로 보고한다.
 */
const literalize = (node: ts.Expression): string | null => {
  if (ts.isStringLiteral(node) || ts.isNoSubstitutionTemplateLiteral(node)) return node.text;
  if (ts.isTemplateExpression(node)) {
    return node.head.text + node.templateSpans.map((span) => `{}${span.literal.text}`).join('');
  }
  return null;
};

const collectUsages = (): Usage[] => {
  const usages: Usage[] = [];

  for (const file of collectSourceFiles(SRC_ROOT)) {
    const text = readFileSync(file, 'utf-8');
    const source = ts.createSourceFile(file, text, ts.ScriptTarget.Latest, true);
    const lines = text.split('\n');

    const visit = (node: ts.Node): void => {
      if (ts.isCallExpression(node) && ts.isIdentifier(node.expression)) {
        const name = node.expression.text;
        const kind = SUBSCRIBE_HOOKS.includes(name)
          ? ('subscribe' as const)
          : name === SEND_FN
            ? ('send' as const)
            : null;

        if (kind && node.arguments.length > 0) {
          const line = source.getLineAndCharacterOfPosition(node.getStart()).line;
          usages.push({
            kind,
            raw: literalize(node.arguments[0]),
            where: `${relative(REPO_ROOT, file)}:${line + 1}`,
            ignored: [lines[line], lines[line - 1] ?? ''].some((l) => l.includes(IGNORE_MARK)),
          });
        }
      }
      ts.forEachChild(node, visit);
    };

    visit(source);
  }

  return usages;
};

/** 런타임 wrapper 가 붙이는 prefix 를 그대로 재현한다. 규칙이 바뀌면 검사도 따라가도록 import 해서 쓴다. */
const toFullPath = (usage: Usage & { raw: string }): string => {
  if (usage.kind === 'send') return WEBSOCKET_CONFIG.APP_PREFIX + usage.raw;
  return isBrokerDestination(usage.raw) ? usage.raw : WEBSOCKET_CONFIG.TOPIC_PREFIX + usage.raw;
};

describe('WebSocket destination 컨트랙트', () => {
  const catalog: Catalog = JSON.parse(readFileSync(CATALOG_PATH, 'utf-8'));
  const usages = collectUsages();

  const subscribable = new Set(
    [
      ...catalog.topics.map((topic) => topic.path),
      ...catalog.queues.map((queue) => queue.path),
      // 카탈로그는 프로퍼티에서 온 /queue/errors 를 적고 FE 는 /user/queue/errors 로 구독한다.
      // Spring user-destination 규약상 둘 다 같은 큐를 가리킨다.
      catalog.errors.topic,
      `/user${catalog.errors.topic}`,
    ].map(normalize)
  );
  const sendable = new Set(catalog.sends.map((entry) => normalize(entry.destination)));

  const resolved = usages.filter((usage) => usage.raw !== null && usage.raw.startsWith('/'));

  it('카탈로그를 읽고 호출부를 찾는다', () => {
    expect(subscribable.size).toBeGreaterThan(0);
    expect(sendable.size).toBeGreaterThan(0);
    // 파서가 조용히 아무것도 못 찾으면 검사 전체가 무의미해진다.
    expect(resolved.length).toBeGreaterThan(20);
  });

  it('구독 destination 이 카탈로그에 있다', () => {
    const missing = resolved
      .filter((usage) => usage.kind === 'subscribe')
      .filter((usage) => !subscribable.has(normalize(toFullPath(usage as Usage & { raw: string }))))
      .map((usage) => `${usage.where} → ${toFullPath(usage as Usage & { raw: string })}`);

    expect(missing).toEqual([]);
  });

  it('발행 destination 이 카탈로그에 있다', () => {
    const missing = resolved
      .filter((usage) => usage.kind === 'send')
      .filter((usage) => !sendable.has(normalize(toFullPath(usage as Usage & { raw: string }))))
      .map((usage) => `${usage.where} → ${toFullPath(usage as Usage & { raw: string })}`);

    expect(missing).toEqual([]);
  });

  it('구독 훅의 destination 은 정적으로 판정할 수 있다', () => {
    // 변수로 조립하면 검사가 눈을 감는다. 조용히 넘기지 않고 의식적인 예외 표시를 요구한다.
    const unresolvable = usages
      .filter((usage) => usage.kind === 'subscribe' && usage.raw === null && !usage.ignored)
      .map((usage) => usage.where);

    expect(unresolvable).toEqual([]);
  });
});
