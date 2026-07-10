# Day 4 Prompt Pack

Szybki zestaw promptów do użycia na Day 4.
Źródło pełne: `prompts/02-module-prompts.md`.

## D4-M1 — Testy
```text
Wygeneruj testy dla zachowania: <opis_przypadku>.
Bez zmian kodu produkcyjnego, chyba że to konieczne — wtedy opisz dlaczego.
Dodaj przypadki pozytywne, negatywne i edge-case.
```

## D4-M2 — Security audit legacy
```text
Przeprowadź mini-audyt bezpieczeństwa modułu <moduł_legacy>.
Zwróć: top 5 ryzyk, poziom ryzyka, rekomendację naprawy, priorytet wdrożenia.
```

## D4-M3 — CI/CD cloud vs on-prem
```text
Porównaj workflow CI/CD dla:
A) GitHub Actions (cloud)
B) Jenkins + GitHub Enterprise (on-prem)
Dla obu: approvals, security gates, audit trail, koszt utrzymania.
```
