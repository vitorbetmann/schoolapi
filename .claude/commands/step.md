---
description: Show or implement a specific plan step (usage /step 5 or /step 5 explain)
argument-hint: <step-number> [explain]
---
Step requested: $ARGUMENTS

- If the arguments include `explain`: explain that step from `docs/PLAN.md` in plain language.
  Cover what AWS services are involved, why (cite ADRs), what will be built, and which
  requirement from `docs/BRIEF.md` it satisfies. Don't write code.
- Otherwise: check `docs/PROGRESS.md`. If earlier steps are unchecked, warn me and ask whether to
  continue anyway. Then follow the same procedure as `/next-step` for this step.
