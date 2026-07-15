---
name: debug-log-capture-test-must-exclude-test-http-server-loggers
description: When asserting a secret never appears in DEBUG logs via a Logback ListAppender at ROOT level, silence the local test HTTP server's own loggers (Jetty/WireMock) first, or its request-echo debug logging produces a false positive.
metadata:
  type: feedback
---

Writing a "secret never appears in DEBUG logs" test (TAC-002-04 style: attach a Logback `ListAppender` to the ROOT logger at `Level.DEBUG`, run a real call through a WireMock-stubbed `OpenAIClient`, assert no captured message contains the API key) initially failed — but not because application code logged the key. WireMock's embedded Jetty server logs the raw inbound HTTP request line-by-line at DEBUG (`HEADER:IN_VALUE --> FIELD(Authorization: Bearer <key>)`) as normal HTTP-server debug output, since it's genuinely the server terminating that connection.

**Why:** This is the test harness echoing what it received (any HTTP server's DEBUG wire log will show the Authorization header, real client requests always include it) — not a leak in the application code under test. A test that fails on this is testing the wrong thing.

**How to apply:** Before setting ROOT to DEBUG in this kind of test, also explicitly set `org.eclipse.jetty` and `com.github.tomakehurst.wiremock` (or whatever the local mock server's logger namespaces are) to `Level.OFF`, restoring all levels in a `finally` block. Keep ROOT at DEBUG for defense-in-depth against unexpected leaks in application/SDK/HTTP-client code — just carve out the mock server's own request-echo logging as known-irrelevant.
