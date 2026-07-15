# Validate if OpenRouter has data retention & data residency policy and some privacy policies for a bigger Companies in the EU and for government, and if it's possible to sign some contracts for European companies. From UE. Check also if they have some possibility to connect only with European Union local providers and to host all data in European Union.

OpenRouter does have documented data‑retention and privacy controls, plus enterprise features (including DPA and SCCs) that EU companies can contract against, and it offers EU‑only in‑region routing with zero‑data‑retention for enterprise customers—but full "all data stays in EU" guarantees depend on which downstream models/providers you choose and on an enterprise setup.[^1_1][^1_2][^1_3][^1_4]

## What OpenRouter actually stores

OpenRouter’s own layer:

- By default, it does **not store prompts or responses**, only request metadata (token counts, latency, model ID, timestamps, etc.), used for billing, reporting and model ranking.[^1_5][^1_2][^1_1]
- Prompt/content logging is strictly **opt‑in**; if you don’t enable logging, your conversation content is not retained at OpenRouter itself.[^1_1][^1_5]
- Metadata retention length is not capped in days in the docs; it is kept “as long as necessary” for operational purposes, and deletion currently requires a request (e.g. account deletion), which some third‑party reviewers flag as a risk for strict minimization audits.[^1_6][^1_7][^1_2]

Downstream providers:

- OpenRouter is a **routing layer**, so your prompt is also sent to the model provider (OpenAI, Anthropic, Google, Mistral, etc.), and **their** retention and training policies apply independently.[^1_2][^1_8][^1_5]
- OpenRouter exposes each provider’s logging/training posture in its UI and docs, and offers account‑level toggles to block models that train on customer data.[^1_9][^1_5][^1_1]


## Zero Data Retention (ZDR) and training controls

OpenRouter now offers a formal **Zero Data Retention (ZDR)** mechanism and data‑collection controls:

- ZDR ensures requests are routed only to endpoints whose providers do **not store prompts/responses** and do not train on those requests. It can be enforced account‑wide, by model group, and per‑request via the `zdr` parameter.[^1_10][^1_4][^1_5][^1_9]
- In addition, a `data_collection: "deny"` setting restricts routing to providers that do not collect user data for training/analytics, and can also be enforced account‑wide.[^1_4][^1_1]
- Combined, ZDR + `data_collection: "deny"` give you a routing constraint of “no retention and no training” at the provider layer, as long as you choose endpoints that support these guarantees.[^1_10][^1_5][^1_4]

Important nuance: ZDR is a **routing constraint**, not a magic override—if you call a provider/endpoint that does not support ZDR, the effective retention is governed by that provider’s own policy.[^1_11][^1_5][^1_2]

## Privacy policy, DPA and GDPR posture

Policies and contracts:

- OpenRouter publishes a **privacy policy** and **terms of service** that cover data collection, logging options, analytics, and user rights.[^1_6][^1_1]
- Third‑party GDPR profile sites note that OpenRouter offers an **online Data Processing Agreement (DPA)** and uses **Standard Contractual Clauses (SCCs)** for cross‑border transfers, with “no training on customer data” by default at the OpenRouter layer.[^1_3]
- OpenRouter itself does not train foundation models; any model training on prompts is purely at the downstream provider, and OpenRouter explicitly disclaims liability for those providers in its terms.[^1_7][^1_5]

For EU/large‑organization use:

- InferCheck’s GDPR profile summarizes OpenRouter as: DPA available, standard SCCs for non‑EU infrastructure, no training on customer data at the gateway level, but upstream providers are effectively sub‑processors with their own policies and are not exhaustively listed.[^1_3]
- Some security consultancies point out that OpenRouter runs primarily on US infrastructure by default and that this gateway architecture makes the compliance posture more complex than calling a single EU provider directly.[^1_12][^1_8][^1_2]

Bottom line: for an EU company, OpenRouter can be used under GDPR with a DPA + SCCs, but strict public‑sector/government workloads will need legal review of (1) the gateway’s policies and (2) every upstream provider you route to.[^1_8][^1_12][^1_3]

## EU‑only routing and data residency

OpenRouter has recently introduced **“Sovereign AI” / EU in‑region routing**:

- Official docs describe **EU in‑region routing** for enterprise customers via a dedicated base URL `https://eu.openrouter.ai`, guaranteeing that prompts and completions are “processed entirely within the European Union” and “never leave the EU at any point in the request lifecycle.”[^1_4]
- When enabled, your requests are only decrypted in the designated EU region and only routed to providers operating in that region; available EU‑eligible models can be listed via `/api/v1/models` on the EU domain or via an “In‑Region Routing” filter in the UI.[^1_4]
- These sovereign‑AI features combine:
    - EU in‑region routing (regional gateway),
    - ZDR enforcement (no provider retention),
    - `data_collection: "deny"` (no provider training/collection).[^1_4]

However, independent audits earlier in 2026 noted that:

- EU routing was **enterprise‑only** and not enabled by default.[^1_2][^1_3][^1_4]
- Standard accounts routed through US infrastructure with no residency guarantee, and one profile claimed that even with EU routing there was still a US gateway component causing cross‑border transfer—likely reflecting older or more conservative assumptions than the newer “Sovereign AI” docs.[^1_12][^1_2][^1_3]

Given this tension between official docs and third‑party write‑ups, for government‑grade residency you’d want written confirmation in your enterprise contract that:

1. The EU routing tier you are using is **physically hosted in the EU**,
2. Requests are **not decrypted outside the EU**, and
3. All chosen providers/models are **deployed in EU regions only** (e.g. Mistral, Azure OpenAI EU regions, Bedrock eu‑central/eu‑west).[^1_8][^1_12][^1_4]

## Only EU providers and “all data in EU” scenario

Technically, OpenRouter can be configured to route **only to EU‑region providers**:

- By using `https://eu.openrouter.ai` as the base URL (enterprise feature), filtering models to EU‑eligible ones, and enforcing ZDR + `data_collection: "deny"`, you can keep routing and inference constrained to EU providers that don’t retain or train on your data.[^1_4]
- Official docs show example usage with EU routing + Meta Llama models where everything stays in‑region, and explain how to programmatically query the EU model list.[^1_4]

But “all data in EU” in a compliance sense still depends on:

- Picking only providers whose **inference endpoints are hosted in EU regions** (e.g. AWS Bedrock eu‑central‑1, Azure France Central), not global endpoints that may process in the US.[^1_12][^1_8]
- Ensuring you don’t enable any non‑EU logging, analytics or session‑replay tools (e.g. external UX analytics) in your own stack or inside OpenRouter’s dashboard, which some reviewers have flagged as potential extra data paths.[^1_7][^1_1][^1_12]

So, yes: from a technical perspective you can build an **EU‑only, zero‑retention, no‑training setup** with OpenRouter—but it requires:

- An **enterprise account** with EU routing enabled,
- Careful **model selection** (only EU‑resident models), and
- Legal review confirming that the gateway and all sub‑processors meet your residency and GDPR requirements.[^1_3][^1_12][^1_4]


## Practical implications for a larger EU company or government client

For your context (Edukey, and potential government/regulated clients):

- **Data‑retention policy**: You can document that OpenRouter does not log prompts by default, offers ZDR, and allows you to deny data collection, but you must pair this with per‑provider policies (OpenAI, Anthropic, etc.).[^1_5][^1_1][^1_2][^1_4]
- **Contracts for EU companies**: There is an online DPA, SCCs, and enterprise features (SSO, workspaces, privacy controls) that most mid‑size/large EU companies can accept, but public‑sector/Gov often require more specific assurances or EU‑only gateways—something to negotiate with OpenRouter’s enterprise team.[^1_8][^1_3][^1_4]
- **Strict EU‑only hosting**: If a client demands clear, end‑to‑end EU residency with minimal legal complexity, some advisors still recommend EU‑hosted AI gateways that are natively in Frankfurt/other EU regions and avoid US‑based routing entirely; these market themselves as “OpenRouter‑compatible” so migration is basically base‑URL + API‑key change.[^1_12][^1_8]

If you plan to use OpenRouter for sensitive Polish/EU workloads, a reasonable path is:

1. Start with **no prompt logging**, ZDR enabled, and `data_collection: "deny"` for your workspace.[^1_1][^1_5][^1_4]
2. Limit routing only to providers with **clear ZDR and EU‑region deployments**.[^1_13][^1_10][^1_12][^1_4]
3. Engage OpenRouter’s **enterprise team** to get EU in‑region routing turned on and negotiate a DPA that explicitly covers EU residency and sub‑processor responsibilities.[^1_3][^1_4]

That will put you in a position where you can credibly say “we use an AI routing layer with zero data retention, no training on customer data, and EU‑only residency, backed by an enterprise DPA,” which is often the minimum bar for serious EU clients and many government tenders.[^1_8][^1_3][^1_12][^1_4]
<span style="display:none">[^1_14][^1_15]</span>

<div align="center">⁂</div>

[^1_1]: https://openrouter.ai/docs/guides/privacy/data-collection

[^1_2]: https://anarlog.so/blog/openrouter-data-retention-policy/

[^1_3]: https://infercheck.eu/en/provider/openrouter

[^1_4]: https://openrouter.helicone.ai/docs/guides/features/sovereign-ai

[^1_5]: https://meetily.ai/llm-privacy/openrouter

[^1_6]: https://openrouter.ai/privacy

[^1_7]: https://www.skool.com/ai-automation-society/have-you-noticed-yesterdays-update-to-openrouters-terms-of-service-and-privacy-policy

[^1_8]: https://vensas.de/en/blog/openrouter-llm-gateway-eu

[^1_9]: https://x.com/OpenRouter/status/2060759831674274108

[^1_10]: https://openrouter.ai/docs/guides/features/zdr

[^1_11]: https://openrouter.ai/blog/insights/is-implicit-caching-prompt-retention/

[^1_12]: https://www.requesty.ai/blog/openrouter-eu-alternative-european-ai-gateway

[^1_13]: https://openrouter.ai/blog/insights/ai-data-residency/

[^1_14]: https://docs.openrouter.co/privacy-policy

[^1_15]: https://openrouter.gr.com/privacy-policy.html
