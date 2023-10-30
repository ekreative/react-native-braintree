module.exports = {
  rules: {
    'branch-name': [
      2,
      {
        format:
          /^(feature|publish|release|hotfix|develop|master)\/[a-z0-9._-]+$/,
      },
    ],
  },
};
