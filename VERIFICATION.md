# V1 verification

- Java 21 / Maven verify: BUILD SUCCESS. 22 tests passed, zero failures/errors/skips.
- AST tests: Java 21 records and compact constructors, comments/strings excluded from branch counts, nested method/lambda isolation, specified complexity decisions, thresholds/locations, all six rules, partial parse results, UTF-8/BOM handling, and capped findings.
- ZIP tests: exclusions and sorting, path traversal, normalized duplicates, file/expanded-data/file-count limits, non-ZIP input and no-source archives.
- Spring Boot API tests with H2 PostgreSQL compatibility mode: multipart scanning, saved reports/history, source snapshots, CSV/JSON exports, rules, client errors and CSV escaping.
- Frontend production build passed.
- Chromium end-to-end test against the running Spring Boot backend with H2 passed: actual sample analysis (2 files / 3 methods and constructors / 6 findings / complexity 18), inspecting source, search/severity filtering, CSV export, source pagination, Rules, multipart rescan, same-name comparison, Scan history and mobile layout. No page errors. Desktop/mobile screenshots visually inspected internally.
- First-launch port helper tested with an occupied default port; alternate selection worked and an existing .env was not overwritten. Shell syntax check passed.
- Docker/PostgreSQL stack was not run in this build environment because Docker is unavailable. Its initial local startup/persistence check is still required. H2 tests do not substitute for PostgreSQL integration tests.

Run `bash start.sh`, click Try sample project, then restart the Compose stack without removing volumes to verify saved scan history on your machine.
