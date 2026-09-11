### ORDER-P0-002-FIX1
ASSIGNED TO : opencode-cli
MODEL       : Meta Muse Spark 1.3 (opencode/muse-spark-1.3-contributor-free)
PRIORITY    : P0
DEPENDS ON  : ORDER-P0-002 (rev. C) — QUALITY GATE: FAIL, 2026-09-11

OBJECTIVE
  Make shared/src/commonTest compile and make the retry-failure path throw, so Claude Code
  can execute HttpClientFactoryTest.

CONTEXT
  Claude Code ran `./gradlew :shared:testAndroidHostTest`. Test compilation failed:
    HttpClientFactoryTest.kt:6:23  Unresolved reference 'pluginOrNull'
    HttpClientFactoryTest.kt:133, 134  same
  In Ktor 3.x `pluginOrNull` lives in `io.ktor.client.plugins`, not `io.ktor.client`.
  Second defect, found by review: the factory never sets `expectSuccess`, so a final 500
  response is returned as a normal HttpResponse and `.body<ProbeResponse>()` fails with
  NoTransformationFoundException — T4's `assertFailsWith<ServerResponseException>` cannot
  pass as written. NetworkModule's OkHttp interceptor THROWS after the third failure, so the
  parity-correct behaviour is to throw.

FILES YOU OWN FOR THIS ORDER (write access)
  - shared/src/commonMain/kotlin/com/aistudio/quranblind/network/HttpClientFactory.kt
  - shared/src/commonTest/kotlin/com/aistudio/quranblind/network/HttpClientFactoryTest.kt

FILES YOU MUST NOT TOUCH
  - everything else (no Gradle edits, no app/**)

STEPS
  1. In HttpClientFactoryTest.kt change the import to `io.ktor.client.plugins.pluginOrNull`.
  2. In HttpClientFactory.kt, inside the HttpClientConfig block, add `expectSuccess = true`
     with a one-line comment: mirrors NetworkModule, which throws after the last failed
     attempt instead of returning a 5xx response.
  3. Re-read T1–T5 against Ktor 3.2.0 APIs and fix anything else that will not compile.
  4. Run the compile probes below. Both must print BUILD SUCCESSFUL.

ACCEPTANCE CRITERIA
  [ ] `./gradlew :shared:compileAndroidHostTest` BUILD SUCCESSFUL
  [ ] `./gradlew :shared:compileAndroidMain` BUILD SUCCESSFUL
  [ ] `expectSuccess = true` present in the factory
  [ ] No file outside the two owned files changed (git status)

BUILD VERIFICATION YOU MAY RUN
  ./gradlew :shared:compileAndroidMain
  ./gradlew :shared:compileAndroidHostTest
  (NOT :app:assembleDebug — it OOMs on this host and is Claude Code's problem, not yours)

FORBIDDEN
  - Running any test (./gradlew test, testAndroidHostTest, allTests, connectedAndroidTest).
    Test execution is the exclusive right of Claude Code CLI. Compiling tests is allowed.
  - Editing any path outside "FILES YOU OWN FOR THIS ORDER".
  - Deleting hs_err_pid*.log or any other file. Report litter; do not remove it.
  - git add / commit / push.
  - Writing Arabic in code, comments, commit messages, or terminal output.

REPORT BACK
  Append a section "FIX1 REPORT" to fleet_orders/reports/ORDER_P0_002_REPORT_OPENCODE.md:
  unified diff of both files, verbatim output of both probes, deviations. STOP after reporting.
