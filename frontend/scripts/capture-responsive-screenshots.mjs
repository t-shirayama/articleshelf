import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const captureScript = path.join(__dirname, "capture-design-screenshots.mjs");

const viewportGroups = {
  all: [
    "desktop",
    "macbook",
    "laptop",
    "tablet",
    "mobile",
    "mobile-md",
    "mobile-sm",
  ],
  desktop: ["desktop"],
  macbook: ["macbook"],
  laptop: ["laptop"],
  tablet: ["tablet"],
  mobile: ["mobile", "mobile-md", "mobile-sm"],
  "mobile-md": ["mobile-md"],
  "mobile-sm": ["mobile-sm"],
};

const requestedGroup = process.argv[2] || "all";
const requestedTarget = process.argv[3] || process.env.ARTICLESHELF_SCREENSHOT_TARGET || "all";
const viewports = viewportGroups[requestedGroup];
const commandName = `capture-responsive-${requestedGroup}`;

if (!viewports) {
  console.error(
    `Unknown responsive screenshot group "${requestedGroup}". Available groups: ${Object.keys(viewportGroups).join(", ")}`,
  );
  process.exit(1);
}

for (const viewport of viewports) {
  console.log(`Capturing responsive screenshots for ${viewport}...`);
  const result = spawnSync(process.execPath, [captureScript], {
    env: {
      ...process.env,
      ARTICLESHELF_SCREENSHOT_COMMAND: commandName,
      ARTICLESHELF_SCREENSHOT_VIEWPORT: viewport,
      ARTICLESHELF_SCREENSHOT_TARGET: requestedTarget,
    },
    stdio: "inherit",
  });

  if (result.status !== 0) {
    process.exit(result.status || 1);
  }
}
