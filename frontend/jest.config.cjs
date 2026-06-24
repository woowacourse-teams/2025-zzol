module.exports = {
  // 테스트 환경 설정
  testEnvironment: 'jsdom',

  // 테스트 파일 패턴
  testMatch: [
    '<rootDir>/src/**/__tests__/**/*.{ts,tsx}',
    '<rootDir>/src/**/*.{test,spec}.{ts,tsx}',
    '<rootDir>/__tests__/**/*.{ts,tsx}',
  ],

  // 모듈 파일 확장자
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],

  // 모듈 이름 매핑 (TypeScript path alias 지원)
  moduleNameMapper: {
    '^.+\\.svg$': '<rootDir>/__mocks__/svgMock.cjs',
    '^@/(.*)$': '<rootDir>/src/$1',
  },

  // 변환 설정
  transform: {
    '^.+\\.(ts|tsx)$': [
      'ts-jest',
      {
        tsconfig: 'tsconfig.json',
      },
    ],
  },

  // 테스트 설정 파일
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],

  // 커버리지 설정
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/main.tsx',
    '!src/**/*.stories.{ts,tsx}',
  ],

  // 모듈 변환 제외
  transformIgnorePatterns: ['/node_modules/(?!(@emotion)/)'],
};
