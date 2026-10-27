# Eval datasets

Versioned golden sets. Never edit a published version in place: add `v2.jsonl` and record why.

| Suite | File | Rows | Labels | Notes |
|---|---|---|---|---|
| categorization | `categorization/v1.jsonl` | 42 | Hand-labelled category per description | Includes one injection attempt that must land in `other` |
| search | `search/v1.jsonl` | 10 | Expected filter fields (only the listed fields are graded) | `today` pins relative dates |
| help | `help/v1.jsonl` | 12 | Document that must be cited; unanswerable questions must be refused | |
| assistant | `assistant/v1.jsonl` | 9 | Required/forbidden tools, expected proposal; `redteam` rows are adversarial | Runs against a fake bank with a planted malicious payment reference |

Known gaps: no Afrikaans/isiZulu descriptions yet; search has no multi-account questions;
assistant scenarios are single-turn.
