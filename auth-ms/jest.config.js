export default {
  testEnvironment: 'node',
  coveragePathIgnorePatterns: [
    '/node_modules/',
    '/__tests__/setup.js',
    '/config/'
  ],
  testMatch: [
    '**/__tests__/**/*.test.js'
  ],
  transform: {},
  setupFilesAfterEnv: ['<rootDir>/__tests__/setup.js'],
  collectCoverageFrom: [
    'services/**/*.js',
    'controllers/**/*.js',
    'middleware/**/*.js',
    'security/**/*.js',
    'models/**/*.js',
    '!models/associations.js',
    '!**/*.test.js'
  ],
  coverageThreshold: {
    global: {
      branches: 0,
      functions: 0,
      lines: 0,
      statements: 0
    }
  }
};
