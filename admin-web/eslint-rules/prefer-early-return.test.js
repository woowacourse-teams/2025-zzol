import { RuleTester } from 'eslint';
import { describe, it } from 'vitest';
import rule from './prefer-early-return.js';

/**
 * 규칙이 실제로 무엇을 잡고 무엇을 넘기는지 고정한다.
 *
 * <p>린트 규칙은 조용히 아무것도 안 잡는 상태가 되기 쉽다. 켜 두었다는 사실만 남고
 * 실제로는 안 도는 것을 알아챌 방법이 없다. 잡는 예와 안 잡는 예를 함께 박아 둔다.
 */
RuleTester.describe = describe;
RuleTester.it = it;

const ruleTester = new RuleTester({
  languageOptions: { ecmaVersion: 2022, sourceType: 'module' },
});

ruleTester.run('prefer-early-return', rule, {
  valid: [
    // 이미 일찍 반환한다.
    'function f(ok) { if (!ok) { return; } a(); b(); }',
    // 한 줄짜리는 뒤집어도 이득이 없다.
    'function f(ok) { if (ok) { a(); } }',
    // else 가 있으면 뒤집어도 가지가 그대로 둘이다.
    'function f(ok) { if (ok) { a(); b(); } else { c(); } }',
    // 감싼 if 앞뒤로 다른 문장이 있으면 본문을 통째로 감싼 것이 아니다.
    'function f(ok) { a(); if (ok) { b(); c(); } }',
    // 화살표 함수의 식 본문.
    'const f = (ok) => ok && a();',
  ],
  invalid: [
    {
      code: 'function f(ok) { if (ok) { a(); b(); } }',
      errors: [{ messageId: 'wrapped' }],
    },
    {
      code: 'const f = function (ok) { if (ok) { a(); b(); } };',
      errors: [{ messageId: 'wrapped' }],
    },
    {
      code: 'const f = (ok) => { if (ok) { a(); b(); } };',
      errors: [{ messageId: 'wrapped' }],
    },
    {
      // 옵션으로 기준을 낮추면 한 줄짜리도 잡는다.
      code: 'function f(ok) { if (ok) { a(); } }',
      options: [{ minimumStatements: 1 }],
      errors: [{ messageId: 'wrapped' }],
    },
  ],
});
