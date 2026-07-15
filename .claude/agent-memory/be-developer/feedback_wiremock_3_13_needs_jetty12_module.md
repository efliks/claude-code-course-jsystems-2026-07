---
name: wiremock-3-13-needs-jetty12-module
description: org.wiremock:wiremock:3.13.x no longer bundles a working servlet container; add wiremock-jetty12 and exclude wiremock's own transitive Jetty 11 deps to avoid an IncompatibleClassChangeError.
metadata:
  type: feedback
---

`org.wiremock:wiremock:3.13.2` alone throws `FatalStartupException: Jetty 11 is not present and no suitable HttpServerFactory extension was found` at `WireMockExtension` startup — the core artifact no longer bundles a functional embedded server.

**Why:** The core `wiremock` artifact still transitively pulls old Jetty 11 pieces (`jetty-servlet`, `jetty-webapp`, `jetty-servlets`, and the old-groupId `org.eclipse.jetty.http2:http2-server`) as inert/optional leftovers, but has no working `HttpServerFactory`. Adding `org.wiremock:wiremock-jetty12` (same version) supplies the real Jetty 12 `HttpServerFactory`, but its Jetty 12 classes then collide on the classpath with the stale Jetty 11 ones from wiremock core (different artifactIds so Maven doesn't dedupe them) — this throws `IncompatibleClassChangeError: class ...HttpChannelOverHTTP2 has interface ...HttpChannel as super class` at server startup.

**How to apply:** When adding WireMock 3.13.x as a test dependency, always pair it with `org.wiremock:wiremock-jetty12` (matching version) AND exclude `org.eclipse.jetty` and `org.eclipse.jetty.http2` groupIds (wildcard artifactId) from the `wiremock` core dependency in the pom. Verified working combination for `org.wiremock:wiremock:3.13.2` + `org.wiremock:wiremock-jetty12:3.13.2`.

Also note: WireMock's static-import verification helper is `postRequestedFor(UrlPattern)`, not `postRequestedTo` (only the stub-registration side uses `post(urlPattern)`/`postRequestedTo` doesn't exist).
