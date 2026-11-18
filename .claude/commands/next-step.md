---
description: Implement the next unchecked step from docs/PLAN.md
---
1. Read `docs/PROGRESS.md` and find the first unchecked step. Read that step in `docs/PLAN.md`
   and any ADRs it references in `docs/adr/DECISIONS.md`.
2. Briefly tell me: the step goal, the files you'll create/change, and anything you need from me
   (e.g. AWS account type, admin email). If you need input, ask before writing code.
3. Create a task list from the step's bullets plus a final "verify acceptance criteria" task.
4. Implement it. Tests first for pure logic. Follow every rule in `CLAUDE.md`.
5. Verify: `mvn -q verify`, and `sam validate --lint -t infra/template.yaml` if infra exists.
   **Do not deploy or spend cloud credits without my explicit OK.**
6. Update `docs/PROGRESS.md` (tick + one-line note), then give me:
   - what changed (short)
   - how I can see it working
   - a Conventional Commit message
   - what the next step will be
