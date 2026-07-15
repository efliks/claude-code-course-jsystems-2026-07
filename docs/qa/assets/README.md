# QA Test-Image Fixtures

Generated with Python 3.14 + Pillow 12.3.0 (`pip install pillow`, no fallback needed).
Verified with `Image.open(...).verify()`, magic-byte inspection, and `os.path.getsize`.
Regenerate with the script kept in the session scratchpad (not checked in) if fixtures are lost — placeholder gradient/noise images, no real device photos.

| File | Purpose | AC exercised | Format (PIL) | Dimensions | Byte size |
|---|---|---|---|---|---|
| `valid-photo.jpg` | Baseline valid upload — happy-path submission (flows 4.1–4.3) | AC-01, AC-05, AC-06, AC-09 | JPEG | 1200×900 | 45,441 bytes (~44 KB) |
| `valid-photo.png` | Valid upload in the other allowed format, confirms PNG is accepted like JPEG | AC-01, AC-05, AC-09 | PNG | 1200×900 | 8,966 bytes (~9 KB) |
| `oversized.jpg` | Strictly over the 5 MB limit — must be rejected client-side and server-side, message names the 5 MB limit | AC-06 | JPEG | 2600×1950 | 5,956,141 bytes (~5.68 MB, > 5 MB) |
| `wrong-format.gif` | Real GIF file (not JPEG/PNG) — must be rejected, message names allowed formats | AC-05 | GIF | 400×300 | 25,248 bytes (~25 KB) |
| `tiny-unusable.jpg` | Tiny, near-black, low-detail image — expected to make the vision model report the photo as unusable, driving the "Needs more info" path | AC-17 | JPEG | 40×30 | 651 bytes |

## Verification performed

For every file above:
- `Image.open(path).verify()` succeeded (valid, non-corrupt image data).
- Magic bytes at the start of the file match the expected format signature (`\xFF\xD8\xFF` for JPEG, `\x89PNG\r\n\x1a\n` for PNG, `GIF8` for GIF).
- `Image.open(path).format` matches the intended format (JPEG / PNG / GIF).
- `oversized.jpg` confirmed `> 5 * 1024 * 1024` bytes (5,956,141 > 5,242,880).

## Notes

- All images are synthetic (gradients / noise / solid near-black) — no real product photos, no PII. Content does not need to resemble real electronics; only format, size, and (for `tiny-unusable.jpg`) unusability matter for the ACs above.
- `oversized.jpg` uses random-noise pixel data (resists JPEG compression) at 2600×1950/quality 95 to land just over 5 MB without an unnecessarily large repo file.
- `tiny-unusable.jpg` is intentionally near-black and undersized to increase the chance the vision model flags it as unusable/not-showing-equipment (AC-17); this is a best-effort fixture — actual model behavior depends on the live LLM call and must be confirmed during manual test execution.
