module.exports = {
  extends: 'eslint:recommended',
  env: {
    es2023: true,
    node: true,
    'jest/globals': true,
  },
  plugins: ['jest'],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
    ecmaFeatures: {
      jsx: true,
    },
  },
  settings: {
    react: {
      version: 'detect',
    },
  },
};
