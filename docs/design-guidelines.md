# Design Guidelines — Play (play.pl)

Design system extracted from the live [play.pl](https://www.play.pl) homepage on 2026-07-14 (computed styles via Playwright). Use these tokens when building UIs that should match the Play brand.

## Assets

| Asset | Path (relative to `docs/`) | Notes |
|---|---|---|
| Design tokens (JSON) | [`../assets/design-tokens.json`](../assets/design-tokens.json) | Machine-readable source of truth |
| Logo (SVG) | [`../assets/logo.svg`](../assets/logo.svg) | White "PLAY" wordmark on `#2D0066` dark-purple plate, 125×40 viewBox |
| Favicon (ICO) | [`../assets/favicon.ico`](../assets/favicon.ico) | 3 embedded sizes (48px source) |
| Homepage screenshot | [`../assets/homepage.png`](../assets/homepage.png) | Visual reference of the live site |

## Colors

| Token | Hex | Usage |
|---|---|---|
| `brand.primary` | `#6C43BF` | Primary purple — CTA buttons, active nav item, brand text accents |
| `brand.primaryDark` | `#48227C` | Deep purple — dark hero/section backgrounds |
| `brand.logoBackground` | `#2D0066` | Darkest purple — logo plate; use behind the white wordmark |
| `brand.accent` | `#E6144B` | Play magenta — promo badges, attention highlights (use sparingly) |
| `brand.highlight` | `#FFF200` | Yellow — rare highlight accent (price flashes, stickers) |
| `background.default` | `#FFFFFF` | Page background |
| `background.light` | `#FAFAFA` | Slightly raised sections |
| `background.subtle` | `#F5F5F5` | Cards, alternating sections |
| `text.primary` | `#1F1F1F` | Default text |
| `text.emphasis` | `#26232C` | Slightly warmer near-black for emphasized copy |
| `text.secondary` | `#707070` | Secondary copy |
| `text.muted` | `#999999` | Hints, captions, disabled |
| `text.onDark` | `#FFFFFF` | Text on purple/dark backgrounds |
| `link.default` | `#266DD9` | Inline links |
| `border.default` | `#D6D6D6` | Hairline borders (icon buttons, cards) |

Contrast note: `#6C43BF` on white passes WCAG AA for normal text; `#E6144B` and `#FFF200` are decorative — don't use them for body text.

## Typography

**Font family:** `Manrope, Arial, sans-serif`

Manrope is self-hosted by Play as woff2. Notably, the file named *Regular* is registered at **weight 500**, so the site's "normal" text is 500 — replicate this mapping:

| Weight token | Value | @font-face source |
|---|---|---|
| regular | 500 | `Manrope-Regular.woff2` |
| semibold | 600 | `Manrope-SemiBold.woff2` |
| bold | 700 | `Manrope-Bold.woff2` |

For our own apps, load Manrope from Google Fonts (weights 500/600/700) rather than hotlinking Play's CDN.

**Size scale** (root rhythm is 14px):

| Token | Size | Observed usage |
|---|---|---|
| xs | 10.5px | Top navigation links |
| sm | 12.25px | Utility nav ("Kontakt") |
| base | 14px | Body, buttons |
| lg | 21px | H3 headings (line-height 1.3) |
| xxl | 40px | H2 hero headings (line-height 1.5) |

Line heights: body 1.5, headings 1.3–1.5.

## Spacing

Play works on a **3.5px base rhythm** (quarter of the 14px root): observed values 3.5, 7, 10.5, 14, 21, 28, 42px. Buttons use `0 21px` horizontal padding. When in doubt, pick a multiple of 3.5px (or round to a 4px grid if your framework insists — visually indistinguishable).

## Border Radius

| Token | Value | Usage |
|---|---|---|
| xs | 2px | Tiny chips |
| sm | 3.5px | Inputs, small tags |
| base | **7px** | **Buttons — the signature radius** |
| md | 10.5px | Cards |
| lg | 14px | Large cards / modals |
| circle | 50% | Icon buttons, avatars |

## Components

- **Primary button:** `#6C43BF` background, white text, radius 7px, padding 0 21px, font 14px/500, no border. Hover: darken toward `#48227C`.
- **Secondary button:** white background, `#6C43BF` text, radius 7px, same metrics; on colored backgrounds add a subtle border or shadow for separation.
- **Circular icon button:** white, `0.8px solid #D6D6D6` border, 50% radius, black glyph.
- **Header/nav:** transparent header on white; nav links 10.5px, weight 500, `#1F1F1F`; the active section is `#6C43BF` at weight 700. No uppercase transforms.
- **Sections:** alternate `#FFFFFF` and `#F5F5F5`; hero/promo blocks may use the deep purples with white text.
- **Promo accents:** magenta `#E6144B` chips/badges; yellow `#FFF200` only as a small highlight, never as a text color.

## Logo Usage

- `assets/logo.svg` is the white PLAY wordmark on a dark-purple (`#2D0066`, 90% opacity) rectangle — it works on any background as-is.
- For a transparent-background variant on dark surfaces, drop the first `<path>` (the plate) and keep the white glyph paths.
- On light surfaces, either keep the plate or recolor the glyph paths to `#6C43BF`/`#2D0066`.
- Keep clear space of at least the height of the "P" around the mark; don't stretch (native ratio 125:40).

## Visual Style Summary

Play's design is bright, confident, and consumer-friendly: a white canvas with generous whitespace, softly rounded (7px) purple CTAs, and near-black Manrope text at a slightly heavier-than-usual base weight (500) that gives the UI a sturdy, modern feel. Color is used with discipline — purple carries the brand and all interactive emphasis, while magenta and yellow appear only as small promotional bursts. Typography stays lowercase and conversational, with large airy headings and compact utility text. The overall personality: energetic telecom retail brand, friendly rather than corporate, with strong visual hierarchy driven by color and weight instead of ornament.
