---
name: feedback-programmatic-tx-for-testable-facades
description: Prefer Spring's programmatic TransactionTemplate/PlatformTransactionManager over @Transactional when a component needs to be unit-tested by direct `new` construction (no Spring context, no AOP proxy).
metadata:
  type: feedback
---

`@Transactional` only takes effect through a Spring AOP proxy. A unit test
that does `new SomeService(mockRepo, ...)` and calls a method directly
bypasses the proxy entirely — the annotation is silently a no-op, so any
test asserting rollback-on-failure behavior against a directly-constructed
instance would be testing nothing.

Built `SessionRecorder` (ADR-004, HSDC persistence layer) needed: (1) an
atomic multi-repository write that rolls back cleanly on partial failure,
and (2) a swallow-and-log contract (AC-29: persistence errors never
propagate). Catching an exception *inside* an `@Transactional` method body
would also silently defeat rollback — Spring only rolls back on an
exception that *propagates out* of the proxied method, so a caught
exception commits whatever ran before the catch.

**Why:** using `PlatformTransactionManager` + `new TransactionTemplate(txManager)`
in the constructor instead sidesteps both problems: rollback happens
because `TransactionTemplate.executeWithoutResult` catches, rolls back, and
rethrows internally *before* the surrounding try/catch swallows it, and the
whole thing is testable with a real `DataSourceTransactionManager` (real
DB, real rollback, no Spring context) or even a bare Mockito mock of
`PlatformTransactionManager` for the pure "does it swallow + log" unit test
(Mockito's default null `TransactionStatus` doesn't break
`TransactionTemplate`'s control flow since the callback and the rollback
path don't null-check it).

**How to apply:** whenever a component both (a) needs multi-step atomicity
and (b) must be constructible/testable without spinning up a full Spring
`ApplicationContext`, default to programmatic transactions over
`@Transactional`. Verify rollback with a real transaction manager + real
DB in an integration-style test (spy one repository to throw), and verify
the swallow/log contract in a separate mock-only unit test.
