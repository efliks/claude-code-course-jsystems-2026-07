---
name: env-node-angular22-engine-mismatch
description: On this training VM, the pre-imaged Node v24.14.0 fails Angular CLI 22's engine check — fix via winget install of Node 26.x, not a downgrade.
metadata:
  type: project
---

The course VM ships Node **v24.14.0** by default, but Angular CLI 22.x (`@angular/cli@22.0.6`) requires `^22.22.3 || ^24.15.0 || >=26.0.0` in its engines check and hard-exits (exit code 3, "Please update your Node.js version") rather than just warning — `npm` EBADENGINE warnings alone don't block, but the CLI's own runtime check does.

**Why:** Node 24.15.0 does not exist as a released version (24.x line stops at 24.10.0 per the winget catalog; next is 25.x which is excluded by the semver range, then 26.x). So the only two real options are Node 22.22.3+ (LTS) or Node 26.0.0+.

**How to apply:** Fixed via `winget install --id OpenJS.NodeJS --version 26.4.0 --accept-package-agreements --accept-source-agreements` (run via the full winget path `C:\Program Files\WindowsApps\Microsoft.DesktopAppInstaller_...\winget.exe` since `winget` isn't on the Git Bash `PATH`). Note the installed package is registered under winget id `OpenJS.NodeJS.22` (an LTS-track id capped at 22.x versions) even though the actual installed binary was 24.14.0 — `winget upgrade --id OpenJS.NodeJS.22` will refuse a "downgrade" to 22.23.1 and there's no clean in-place patch within that id; installing the separate `OpenJS.NodeJS` id at a 26.x version overwrites `C:\Program Files\nodejs` in place and works. After upgrading, `node -v` / `npx @angular/cli@latest version` confirmed 26.4.0 + CLI 22.0.6 working. This is a global machine change (affects the whole VM) — flag it in the task report per AGENTS.md's global-change caution, but it's a reasonable one-time fix since every participant attempting Angular 22 scaffolding on this VM image will hit the same blocker.

Separately, this VM's npm (11.17.0) gates postinstall scripts for `esbuild`, `lmdb`, `msgpackr-extract`, `@parcel/watcher` behind `npm warn allow-scripts` / `npm approve-scripts`. **Do not bother approving them** — `ng build` and `ng test` (vitest) both worked fine without approval; these tools ship prebuilt platform binaries as optional deps and don't need the gated postinstall script.
