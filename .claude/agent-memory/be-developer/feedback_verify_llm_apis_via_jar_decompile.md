---
name: verify-llm-apis-via-jar-decompile
description: When Context7 fails and a fast-moving LLM SDK's exact API surface is uncertain, decompile the actual jar on the classpath (javap) rather than trusting only WebFetch/README summaries.
metadata:
  type: feedback
---

For B4 (LLM integration, openai-java 4.43.0 via OpenRouter), Context7's `resolve-library-id`/`query-docs` failed with an invalid-API-key error (known issue in this project, noted in the task prompt). WebFetch of the GitHub README gave plausible-looking code, but the most reliable ground truth was decompiling the actual jars already resolved into `~/.m2/repository/com/openai/openai-java-core/<version>/*.jar` with `javap -classpath <jar> <FQCN>` — this gives the exact real method signatures (builder methods, overloads, return types) for the exact version pinned in the pom, not whatever version the README/docs site currently shows.

**Why:** WebFetch summarizes large pages through a smaller model and can miss overloads or hallucinate plausible-but-wrong method names, especially for Kotlin-interop SDKs with generated builder classes. `javap` on the resolved jar is ground truth for the exact dependency version in use.

**How to apply:** When Context7 is unavailable/failing and a task depends on getting a fast-moving SDK's exact method signatures right (client builder methods, structured-output/response-format APIs, streaming APIs), locate the jar under `~/.m2/repository/...` and run `javap -classpath <jar> <fully.qualified.ClassName>` for the key classes before writing code against them. This also works well for verifying enum constants baked into a library at build time (e.g. `com.openai.models.ChatModel`'s `GPT_5_6_*` constants independently corroborated an OpenRouter catalog fetch about which models were current vs. deprecated, cross-checking a WebFetch result that could otherwise have been a hallucination). Cross-checking two independent sources (live catalog fetch + jar decompile) is worth the extra step when defaults get baked into an ADR.

Related: [[project_hsdc_backend_stack]].
