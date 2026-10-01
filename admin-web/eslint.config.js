import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';
import preferEarlyReturn from './eslint-rules/prefer-early-return.js';

/**
 * 규칙을 많이 켜지 않는다.
 *
 * <p>포맷은 prettier 가 보고, 타입은 tsc 가 본다. 여기서 잡을 것은 <b>그 둘이 못 잡는데
 * 런타임에 조용히 틀리는 것</b>뿐이다. 실제로 이 프로젝트에서 그런 것은 훅 의존성
 * 배열 하나였다(HomePage 의 폼 초기화가 매 렌더 덮어쓰는 버그).
 *
 * <p>스타일 규칙을 켜면 리뷰가 그쪽으로 쏠린다. 들여쓰기를 지적하는 CI 는 로직을
 * 보지 않게 만든다.
 */
export default tseslint.config(
  { ignores: ['dist', 'node_modules', 'coverage'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      local: { rules: { 'prefer-early-return': preferEarlyReturn } },
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      // 쓰지 않는 매개변수는 _ 로 시작하면 통과시킨다. 콜백 시그니처를 맞추느라
      // 안 쓰는 인자를 받아야 하는 자리가 있다.
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      // 함수 본문 전체를 감싸는 if 를 금지한다. 근거와 직접 쓴 이유는 규칙 파일에 있다.
      'local/prefer-early-return': 'error',
    },
  },
);
