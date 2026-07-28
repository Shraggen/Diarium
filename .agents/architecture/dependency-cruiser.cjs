/**
 * Starter dependency-cruiser rules.
 * Update paths to match the repository before enabling blocking mode.
 */
module.exports = {
  forbidden: [
    {
      name: "no-circular-dependencies",
      severity: "error",
      from: {},
      to: { circular: true }
    },
    {
      name: "domain-must-not-depend-on-infrastructure",
      severity: "error",
      from: { path: "services/.*/src/domain" },
      to: { path: "services/.*/src/(infrastructure|adapters|web|controllers|persistence)" }
    },
    {
      name: "cross-context-imports-use-contracts",
      severity: "error",
      from: { path: "services/([^/]+)/" },
      to: {
        path: "services/([^/]+)/",
        pathNot: "packages/contracts/"
      }
    }
  ],
  options: {
    doNotFollow: { path: "node_modules" },
    exclude: "(^|/)(node_modules|dist|build|coverage|vendor)/"
  }
};
