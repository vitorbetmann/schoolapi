# Getting started with this Claude project

1. Unzip, `cd schoolapi`, `git init`, push to a new **public** GitHub repo.
2. Open the folder in Claude Code (`claude` in the terminal, or the VS Code / JetBrains extension).
   Claude reads `CLAUDE.md` automatically.
3. Fill in the "Environment facts" in `docs/PROGRESS.md` (personal AWS account or Learner Lab?
   admin email?).
4. Run the steps:
   - `/step 0 explain` → do the manual setup (tools, AWS account, budget).
   - `/next-step` → Claude implements the next step, verifies it and ticks `PROGRESS.md`.
   - `/review-step` → check it against acceptance criteria + rubric before committing.
   - `/status` → see what's left for delivery at any time.
5. Claude will **ask before** any `sam deploy`, `sam delete`, commit or push (`.claude/settings.json`).

Tip: start a fresh Claude Code session per step (`/clear`). `PROGRESS.md` carries the state over.
