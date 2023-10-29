module.exports = {
  rules: {
    'branch-name': [
      2,
      {
        format: /^(feature|release|hotfix|support|develop|master)\/[a-z0-9._-]+$/,
      }
    ]
  }
};
