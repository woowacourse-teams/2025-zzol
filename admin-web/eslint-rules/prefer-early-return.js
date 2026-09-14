/**
 * 함수 본문 전체를 `if` 하나로 감싸지 못하게 한다.
 *
 * <p>조건을 뒤집어 일찍 나가면 본문이 한 단계 왼쪽으로 나오고, 조건이 늘어날 때 중첩이
 * 아니라 줄로 쌓인다. 읽는 사람이 "이 코드가 도는 조건"을 머릿속에 쌓아 두지 않아도 된다.
 *
 * <pre>
 * // 이렇게 말고
 * function f() {
 *   if (ok) {
 *     a();
 *     b();
 *   }
 * }
 *
 * // 이렇게
 * function f() {
 *   if (!ok) {
 *     return;
 *   }
 *   a();
 *   b();
 * }
 * </pre>
 *
 * <h2>왜 직접 썼나</h2>
 *
 * <p>같은 규칙이 {@code eslint-plugin-unicorn} 에 있는데 이 저장소의 ESLint 10 과 맞지 않아
 * 로드부터 실패한다(v73, v74 둘 다 {@code mapTypes.union is not a function}). 규칙 하나를
 * 위해 ESLint 버전을 내리지 않는다. 그리고 그 플러그인은 규칙이 339개라, 하나를 쓰자고
 * 통째로 들이면 나머지가 언제든 켜질 수 있는 상태가 된다.
 *
 * <h2>감싼 블록이 두 줄 이상일 때만 잡는다</h2>
 *
 * <p>한 줄짜리를 뒤집으면 오히려 길어진다. {@code if (ok) { a(); }} 를 되돌리는 것은
 * 이득이 없다. 중첩이 실제로 읽기를 방해하는 지점부터 잡는다.
 */
const rule = {
  meta: {
    type: 'suggestion',
    docs: { description: '함수 본문을 통째로 감싼 if 대신 조건을 뒤집어 일찍 반환한다.' },
    schema: [
      {
        type: 'object',
        properties: { minimumStatements: { type: 'integer', minimum: 1 } },
        additionalProperties: false,
      },
    ],
    messages: {
      wrapped: '함수 본문을 if 로 감싸지 말고 조건을 뒤집어 먼저 반환하세요.',
    },
  },

  create(context) {
    const minimumStatements = context.options[0]?.minimumStatements ?? 2;

    const check = (node) => {
      const body = node.body;
      if (body?.type !== 'BlockStatement' || body.body.length !== 1) {
        return;
      }

      const only = body.body[0];
      // else 가 있으면 뒤집어도 가지가 그대로 둘이라 얻는 것이 없다.
      if (only.type !== 'IfStatement' || only.alternate) {
        return;
      }

      const wrapped = only.consequent.type === 'BlockStatement' ? only.consequent.body.length : 1;
      if (wrapped < minimumStatements) {
        return;
      }

      context.report({ node: only, messageId: 'wrapped' });
    };

    return {
      FunctionDeclaration: check,
      FunctionExpression: check,
      ArrowFunctionExpression: check,
    };
  },
};

export default rule;
