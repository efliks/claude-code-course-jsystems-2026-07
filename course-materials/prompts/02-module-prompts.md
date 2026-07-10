# 02-module-prompts.md — Prompty modułowe (zsynchronizowane z Day 1–Day 5)

Cel pliku: szybka nawigacja po promptach, które **muszą być zgodne** z inline sekcjami:
- `materials/scripts/day-1-script.md#prompty-trenerskie-day-1-inline`
- `materials/scripts/day-2-script.md#prompty-trenerskie-day-2-inline-gotowe-do-wklejenia`
- `materials/scripts/day-3-script.md#prompty-trenerskie-day-3-inline-gotowe-do-wklejenia`
- `materials/scripts/day-4-script.md#prompty-trenerskie-day-4-inline-gotowe-do-wklejenia`
- `materials/scripts/day-5-script.md#prompty-trenerskie-day-5-inline-gotowe-do-wklejenia`

> Źródło prawdy: skrypty dzienne. Ten plik jest indeksem + quick copy.

## Stały kontekst kursu (domyślny)
- Case kursowy: **SOC Copilot Incident Triage**.
- Tryb pracy: mały zakres → diff → test → review → poprawka.
- Ograniczenia: środowisko regulowane, bez danych wrażliwych w promptach.

---

## Day 1 — Fundamenty i start workflow

### D1-P1 — Start kursu i kontrakt pracy
```text
Ćwiczenie startowe (5 min):
1) Napisz 1 zdaniem: gdzie AI realnie pomaga Ci dziś w pracy.
2) Napisz 1 ryzyko, którego najbardziej chcesz uniknąć.
Format: „Pomaga: ... | Ryzyko: ...”.
```

### D1-P2 — Quiz calibration helper
```text
Przeanalizuj wyniki anonimowego quizu podstaw AI.
Wynik podaj w 3 sekcjach:
1) Co grupa już umie (max 5 punktów),
2) Co wymaga doprecyzowania dzisiaj (max 5 punktów),
3) Jak zmienić tempo Day 2 (3 konkretne decyzje trenerskie).
Ton: wspierający, bez oceniania.
```

### D1-P3 — MVP scope prompt
```text
Przygotuj roboczy zakres MVP dla aplikacji „SOC Copilot Incident Triage”.
Zwróć: cel biznesowy, user story, kryteria akceptacji, out-of-scope,
3 główne ryzyka i minimalny zakres na 1 dzień pracy.
Bez kodu. Język: polski.
```

---

## Day 2 — Problem framing, architektura, UX/UI

### D2-P1 — Problem framing
```text
Jesteś analitykiem produktu dla aplikacji SOC Copilot Incident Triage.
Przygotuj: problem statement, personę, 5 kryteriów akceptacji,
listę assumptions/constraints oraz jasne out-of-scope.
Uwzględnij środowisko bankowe i wymagania audytu.
```

### D2-P2 — Architektura + ADR
```text
Przygotuj ADR-0001 dla decyzji architektonicznej w projekcie SOC Copilot Incident Triage.
Uwzględnij: kontekst, opcje, decyzję, konsekwencje, ryzyka,
wpływ na bezpieczeństwo, audit trail i obsługę on-prem.
```

### D2-P3 — Bridge React -> Angular
```text
Przygotuj translację podejścia React (demo szkoleniowe) -> Angular (projekt produkcyjny).
W 2 kolumnach pokaż: co mapuje się 1:1 (kontrakty, testy, slicing zadań),
a co wymaga translacji frameworkowej (komponenty, stan, routing, tooling).
```

---

## Day 3 — Pair programming i implementacja rdzenia

### D3-P1 — Mały slice implementacyjny
```text
Zaimplementuj mały slice: <nazwa_slice>.
Zakres: tylko <pliki/moduły>.
Wynik: diff + krótki opis + lista testów do uruchomienia.
Nie zmieniaj nic poza zakresem.
```

### D3-P2 — Debug loop
```text
Pomóż zdebugować błąd.
Objaw: <objaw>
Expected: <oczekiwane>
Actual: <faktyczne>
Logi: <fragment>
Zaproponuj: hipotezy, kroki weryfikacji, minimalną poprawkę.
```

### D3-P3 — Refactor bez zmiany zachowania
```text
Zrefaktoryzuj <moduł> dla czytelności i utrzymania.
Nie zmieniaj zachowania biznesowego.
Dodaj uzasadnienie zmian i checklistę regresji.
```

---

## Day 4 — Jakość, security, CI/CD

### D4-P1 — Testy krytycznego flow
```text
Wygeneruj testy dla zachowania: <opis_przypadku>.
Bez zmian kodu produkcyjnego, chyba że to konieczne — wtedy opisz dlaczego.
Dodaj przypadki pozytywne, negatywne i edge-case.
```

### D4-P2 — Legacy JFTP security checkpoint
```text
Zrób security audit fragmentu legacy (JFTP).
Wypisz min. 5 ryzyk i oznacz każde jako CRITICAL/HIGH/MEDIUM/LOW.
Dla CRITICAL/HIGH dopisz: fix now + fix next sprint.
Na końcu decyzja: fix now / mitigate / accept (z uzasadnieniem).
```

### D4-P3 — CI/CD cloud vs on-prem
```text
Porównaj workflow CI/CD:
A) GitHub Actions (cloud)
B) Jenkins + GitHub Enterprise (on-prem)
Dla obu pokaż: approvals, security gates, audit trail, fallback gdy model/cloud niedostępny.
```

---

## Day 5 — Domknięcie, audit końcowy, 30 dni

### D5-P1 — Finalny audit rozwiązania
```text
Zrób finalny przegląd rozwiązania SOC Copilot Incident Triage.
Oceń: jakość kodu, testowalność, bezpieczeństwo, gotowość do wdrożenia.
Zaproponuj 3 poprawki o najwyższym ROI.
```

### D5-P2 — Adaptacja cloud -> on-prem
```text
Przygotuj plan adaptacji workflow z cloud do on-prem/local models.
Uwzględnij: hosting modeli, sekrety, logowanie, approvals, ograniczenia sieciowe,
audit wymagany w środowisku regulowanym.
```

### D5-P3 — Plan 30 dni po kursie
```text
Przygotuj plan 30 dni wdrożenia AI workflow w zespole developerskim.
Podziel na tygodnie, dodaj KPI, ryzyka i szybkie wygrane.
```

---

## Uwaga operacyjna
Jeśli cokolwiek w tym pliku rozjeżdża się z sekcją „Prompty trenerskie Day X” w skryptach dziennych,
popraw najpierw ten plik tak, aby był 1:1 zgodny ze skryptami.