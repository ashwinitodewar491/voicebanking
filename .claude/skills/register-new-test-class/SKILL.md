---
name: register-new-test-class
description: Use whenever a new TestNG test class is created (or an existing one is renamed/moved) anywhere under src/test/java/com/voicebanking/tests/ in this repo — API or UI, voice or non-voice. Wires the class into testng.xml and verifies it actually shows up in the dashboard/CI path, not just under a -Dtest= scoped run. Trigger phrases: "new test class", "add a test class", "why isn't my test showing in the dashboard", "why didn't this run in CI".
---

# Register a new test class

`target/dashboard-report/index.html` (`DashboardGenerator`) and `SurefireReportParser` read
**whatever XML files Surefire happens to have written to `target/surefire-reports`** — they have
no class whitelist of their own. That means a brand-new test class silently "not appearing in the
dashboard" is almost never a dashboard bug. It means Surefire never ran the class in the first
place, and there is exactly one place that decides which classes Surefire runs for anything other
than a manually-scoped invocation: **`testng.xml`**.

## Why this bites people

`mvn test` with no `-Dtest=` flag drives Surefire off `<suiteXmlFile>testng.xml</suiteXmlFile>`
(`pom.xml`). Both the GitHub Actions workflow (`mvn -B clean test -DtestGroups=...`) and the
Jenkins pipeline run this way, filtered further by TestNG group (`sanity`/`smoke`/`regression`/
etc.). A class that exists on disk, compiles, and even passes when you run it directly with
`-Dtest=YourNewClass` **never executes at all** in that path if it isn't also listed in
`testng.xml` — no error, no warning, it's just absent from `target/surefire-reports`, and so
absent from the dashboard, the Extent report, and CI's published results.

This is exactly the trap: testing a new class only via `-Dtest=YourNewClass` (the fast, obvious
way to iterate on it) *looks* like full verification because it runs and passes, but that flag
bypasses `testng.xml` entirely (see the `maven-clean-plugin` comment in `pom.xml` and the
`@Listeners` javadoc on `BasePage`/`BaseVoiceTest` for the same gotcha from another angle) — so
the one thing it can never catch is "did I forget to register this class."

## Checklist for a new test class

1. **Add a `<class name="...">` entry to `testng.xml`.** Put it in the `<test>` block that
   matches its theme (e.g. every voice-registration/voice-print class lives together) or start a
   new `<test name="...">` block if it's a genuinely new theme — follow the existing block
   granularity, don't dump everything into one giant block.
2. **Confirm `@Listeners(TestListener.class)` is on the class itself**, either directly or via a
   base class (`BasePage`, `BaseVoiceTest`) that already carries it — not just relying on
   `testng.xml`'s suite-level `<listener>` tag, which Surefire silently skips for any
   `-Dtest=`-scoped run. Without this, `-Dtest=YourNewClass` runs never populate
   `SessionEndedTracker`/`NoResponseTracker`/ExtentReports even though the full-suite run would.
3. **Give every `@Test` a `description = "..."`.** `SurefireReportParser.lookupTestDescription`
   reads it via reflection for the dashboard row; a method with no description just falls back to
   its bare method name there.
4. **If the class enrolls a real voiceprint** (anything modeled on
   `UI11_VoiceRegistrationAuthTest`/`UI13_VoiceRegistrationNaturalVariationTest`), remove it again
   in a `finally`/`@AfterClass` block, and prefer the already-sanctioned clean accounts (Leena
   Kamat `CUSTOMER_B_*`, Aniket More `CUSTOMER_C_*` in `Constants.java`) over an account other
   suites assume is *unregistered* (e.g. Rohit Mehta, used unregistered by
   `BaseVoiceTest`-derived suites). A voiceprint left behind on the wrong account makes an
   unrelated suite's queries start failing with a spurious "Not Authorised" the next time it runs.
5. **Verify two ways, not one:**
   - `mvn test -Dtest=YourNewClass -Denv=stage` — fast iteration, proves the test logic itself
     works.
   - `mvn test -DtestGroups=<one of its groups> -Denv=stage` (no `-Dtest=`) — proves `testng.xml`
     wiring is actually correct, since this is the same code path CI and a full local run use.
     Skipping this step is exactly how a class ships "working" but invisible to the dashboard/CI.
6. Re-run `mvn test` (or check the last line of its output) and confirm
   `target/dashboard-report/index.html`'s `[Dashboard] Total:` count includes the new class's test
   methods.
