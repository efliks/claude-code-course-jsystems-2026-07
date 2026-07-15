## Szybkie podsumowanie

Spotkanie było sesją szkoleniową prowadzoną przez JSystems na temat wykorzystania agentów AI do tworzenia oprogramowania, ze szczególnym uwzględnieniem narzędzi Cloud i OpenAI. JSystems zademonstrował, jak konfigurować agentów i zarządzać nimi, delegować zadania oraz obsługiwać limity i tokeny. Grupa omówiła wykorzystanie różnych modeli, takich jak Fabel i Opus, oraz porównała ich wydajność i koszty. Omawiali takie tematy, jak tworzenie planów, praca z podagentami oraz używanie narzędzi takich jak Playwright i MCP do testowania i automatyzacji. Sesja obejmowała praktyczne ćwiczenia dotyczące implementacji aplikacji, zarządzania repozytoriami Git oraz rozwiązywania problemów takich jak limity tokenów i konfiguracja środowiska. Uczestnicy tacy jak Piotr, Kasia i Grzegorz zadawali pytania dotyczące przepływów pracy, uprawnień oraz najlepszych praktyk koordynacji rozwoju z agentami AI. JSystems podzielił się również spostrzeżeniami na temat prywatności danych, porównań narzędzi oraz znaczenia właściwej dokumentacji i organizacji procesów podczas pracy z AI w zespołach deweloperskich.

## Kolejne kroki

### Grzegorz

- Rozwiązuj wszelkie pozostałe konflikty i upewnij się, że wszystkie zmiany zostaną przeniesione do zdalnego repozytorium przed zamknięciem maszyny wirtualnej.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e731182-805b-11f1-bf76-721667530bc3)

### JSystems

- Zaktualizuj materiały szkoleniowe i repozytorium o poprawki i ulepszenia omówione podczas sesji, zwłaszcza w zakresie spójności dokumentacji i instrukcji.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e7304e3-805b-11f1-a9d5-721667530bc3)
- Zbadaj i rozwiąż problem z agentami, którzy nie tworzą granularnych komunikatów commit zgodnie z oczekiwaniami.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e730869-805b-11f1-9a8f-721667530bc3)
- Wyślij link do ankiety i przypomnij uczestnikom o jej wypełnieniu po szkoleniu.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e7309dc-805b-11f1-a158-721667530bc3)
- Podaj dodatkowe informacje i przykłady dotyczące orkiestracji zadań za pomocą różnych modeli (np. Fabel, Opus, Sonnet) oraz delegowania pracy mniejszym modelom.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e730b2a-805b-11f1-abdd-721667530bc3)
- Udostępnij zaktualizowane repozytorium i slajdy grupie po połowie sierpnia.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e730c5d-805b-11f1-8d3a-721667530bc3)

### Współpraca

- Wszyscy uczestnicy: Upewnij się, że token dostępu osobistego GitHuba ma uprawnienia „content: read and write”, aby umożliwić agentom wprowadzanie zmian.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e730d84-805b-11f1-9dd9-721667530bc3)
- Wszyscy uczestnicy: Pobierz kod z repozytorium, aby kontynuować pracę nad aplikacją na własnych urządzeniach.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e730e8e-805b-11f1-984f-721667530bc3)
- Wszyscy uczestnicy: Eksperymentuj z wykorzystaniem API OpenAI i innych dostawców (np. Open Router, ZTI) do delegowania modeli i optymalizacji kosztów.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e730f91-805b-11f1-9498-721667530bc3)
- Wszyscy uczestnicy: Przejrzyj materiały szkoleniowe i dodatkowe zasoby udostępniane przez JSystems w celu dalszej nauki.[](https://tasks.zoom.us?meetingId=ki0g4SLNTFuodoI53%2BWo7A%3D%3D&stepId=1e731084-805b-11f1-aa05-721667530bc3)

## Podsumowanie

### Wdrożenie systemu i planowanie dokumentacji

Zespół omówił wdrożenie planu i współpracę z agentami, koncentrując się na usuwaniu nieistotnych umiejętności i dokumentacji. JSystems udostępnił wskazówki dotyczące sprawdzania zawartości różnych plików pod kątem spójności i usuwania zbędnych materiałów, szczególnie tych związanych z Javą. Omówiono również techniczne aspekty punktów końcowych i standardów, wspominając o popularności i kompatybilności różnych opcji, takich jak OpenAI i AWS. Rozmowa obejmowała rekomendacje dotyczące dokumentacji i narzędzi wspierających realizację projektu.

### Otwarte zasady routera i danych

Zespół omówił umowy Open Router i zasady przechowywania danych, a JSystems podzielił się badaniami dotyczącymi możliwości Perplexity w zakresie zamówień rządowych UE i bezpieczeństwa danych. Omówili poprawki dokumentacji i omówili wdrożenie systemu planowania zadań z zależnościami, podkreślając znaczenie stworzenia matrycy do śledzenia współzależnych zadań i równoległych strumieni pracy. Rozmowa dotyczyła również szczegółów konfiguracji technicznej, w tym plików środowiskowych i konfiguracji terminali do pracy z CLUDE, a JSystems planuje usprawnić proces instalacji i dostarczyć lepszą dokumentację.

### Usprawnienie procesu ręcznej walidacji

JSystems omówił znaczenie ręcznej walidacji aplikacji po głównych etapach rozwoju, podkreślając, że zautomatyzowane testy E2E mogą powodować fałszywie pozytywne wyniki i zalecając ręczne testy zapewnienia jakości przy użyciu Playwright MCP lub CLI. Postanowił dodać ręczny etap walidacji do instrukcji przepływu pracy TDD w AGENTS.md i materiałach szkoleniowych, wymagający zrzutów ekranu i porównania z wytycznymi projektowymi w celu zapewnienia odpowiedniej funkcjonalności i spójności wizualnej. JSystems zauważył również, że chociaż testy automatyczne są przydatne, walidacja ręczna powinna być zawsze wykonywana przed uznaniem zadania za zakończone, szczególnie w przypadku krytycznych zmian aplikacji.

### Modelowanie zachowania i postępu rozwojowego

Kasia i JSystems omawiali zagadnienia związane z zachowaniem modeli, w szczególności dotyczące rekomendacji komitetów oraz różnic interpretacyjnych instrukcji między modelami takimi jak Opus czy Fable. Piotr poinformował o postępach prac rozwojowych, w tym o zakończonej pracy nad punktem końcowym S4.1 z uwierzytelnianiem BMI i przetwarzaniem obrazu, natomiast S4.2 napotkał tymczasowy problem infrastrukturalny z usługą weryfikacji kodu Claude. Zespół przeanalizował statystyki użytkowania API i koszty, omawiając strategie wyboru modeli oraz kompromisy między używaniem większych modeli, takich jak Fable, a delegowaniem zadań mniejszym modelom, takim jak Sonnet, w celu zwiększenia efektywności kosztowej i wydajności.

### Wyzwania koordynacji modelu LLM

Piotr omówił z kolegą wyzwania związane z koordynowaniem czasu zakończenia zadań, zwłaszcza około godziny 17:00 jako terminu wirtualnego ponownego uruchomienia, choć nie było pewności, czy kolega otrzymał komunikat o przerwaniu pracy. JSystems dostarczył wskazówek dotyczących pracy z mniejszymi modelami LLM, takimi jak Sonnet i Opus w celu delegowania zadań, wyjaśniając względy kosztowe i strategie zarządzania zachowaniem modelu. Dyskusja dotyczyła również kwestii technicznych związanych z rozwojem agentów, w tym problemów z uruchamianiem nowych projektów i korzystaniem z repozytoriów Git, a JSystems podzielił się spostrzeżeniami na temat pracy z podagentami i zarządzania limitami tokenów.

### Obejścia przełączania modeli AI

JSystems omówił wyzwania związane z przełączaniem się między różnymi modelami sztucznej inteligencji w ramach jednej sesji i zbadał rozwiązania, w tym wykorzystanie niestandardowych funkcji i aliasów w bash i PowerShell do zarządzania różnymi dostawcami i punktami końcowymi. Zademonstrowali, jak zresetować zmienne środowiskowe w celu przełączania się między modelami takimi jak GPT-4, Claude i inne, oraz podkreślili korzyści płynące z korzystania z zewnętrznych dostawców dla efektywności kosztowej i wyższych limitów tokenów. Firma JSystems poleciła to podejście każdemu, kto jest zainteresowany efektywnym zarządzaniem wieloma subskrypcjami AI i udostępniła odpowiednie linki do dalszej konfiguracji.

### Modele AI i obawy dotyczące bezpieczeństwa

JSystems omówił różnice między różnymi modelami sztucznej inteligencji, podkreślając znaczną lukę w jakości między modelami takimi jak Fabran a droższymi opcjami, takimi jak Opus. Wyraził obawy dotyczące prywatności danych, zwłaszcza w odniesieniu do chińskich modeli wysyłających dane do Chin, i podzielił się informacjami na temat problemu bezpieczeństwa, w którym model wysyłał całą zawartość folderów na serwery SpaceX bez wiedzy użytkownika. Dyskusja dotyczyła również technicznych aspektów pracy z agentami Claude AI, w tym sposobu obsługi limitów sesji, powiadomień i przełączania kont, a JSystems demonstrował różne metody otrzymywania powiadomień i zarządzania wieloma agentami.

### Zarządzanie kontem i procesy autoryzacji

JSystems i Piotr omówili proces logowania i zarządzania wieloma kontami w ich systemie, w tym obsługę kodów autoryzacyjnych oraz limitów sesji. Firma JSystems wyjaśniła, jak skonfigurować procedury w chmurze do automatycznego odnawiania limitów i zasugerowała użycie kont organizacyjnych w celu lepszej kontroli i statystyk. Kasia zapytała o korzyści płynące z używania licencji Teams w porównaniu do konta prywatnego, a JSystems przedstawił informacje na temat różnic kosztowych i kwestii bezpieczeństwa danych. Grupa zrobiła przerwę i planowała kontynuować dyskusję po obiedzie.

### Claude Automation Tools Dyskusja

Zespół omówił dwa różne narzędzia automatyzacji dla Claude’a: procedury oparte na chmurze i lokalne aplikacje desktopowe. JSystems wyjaśnił, że procedury chmurowe działają niezależnie i mogą być planowane za pośrednictwem interfejsu internetowego, podczas gdy aplikacje lokalne wymagają działania aplikacji desktopowej. Grzegorz wyraził obawy dotyczące nieprawidłowego ustawiania limitów automatyzacji, które – jak potwierdził JSystems – zostały niedawno odnowione. Dyskusja dotyczyła również zalet i wad równoległego w porównaniu z sekwencyjnym wykonywaniem agentów, przy czym JSystems zauważył, że chociaż wykonanie równoległe zużywa więcej tokenów, może być bardziej wydajne dla niektórych rodzajów pracy, które nie wiążą się z konfliktami plików.

### Wyzwania koordynacji frontend-backend

Zespół omówił wyzwania związane z koordynacją oddzielnych zespołów deweloperskich frontendu i backendu, szczególnie podczas współpracy z zewnętrznymi dostawcami, którzy tworzyli niepołączone komponenty aplikacji. JSystems zasugerował wykorzystanie narzędzi dokumentacyjnych, takich jak Confluence czy Jira, do udostępniania wymagań i specyfikacji między zespołami, a także rozważenie udzielenia agentom dostępu do repozytoriów kodu zaplecza w celu lepszego zrozumienia. Dyskusja zakończyła się tym, że JSystems zalecił zespołowi konkretne zasoby sztucznej inteligencji i programowania do wykorzystania, w tym kanały YouTube i ekspertów w tej dziedzinie, a także wspomniał o nadchodzących materiałach szkoleniowych, które zostaną zaktualizowane w połowie sierpnia.

### Spotkanie poświęcone opracowywaniu aplikacji Room Service

Piotr ukończył aplikację dla room service, która umożliwia użytkownikom sprawdzanie uszkodzeń produktów, składanie reklamacji oraz rekomendowanie potraw na podstawie zawartości lodówki. JSystems omówił kwestie techniczne związane z ograniczeniami narzędzia i zasugerował pobranie kodu przed zakończeniem szkolenia. Zespół rozwiązał problemy z drzewami roboczymi Gita oraz potrzebę scalania kodu do głównego repozytorium przed zamknięciem maszyny wirtualnej za dwie godziny. JSystems dostarczył instrukcje dotyczące konfigurowania uprawnień GitHub i wprowadzania zmian do zdalnego repozytorium.

### Sesja szkoleniowa Git i GitHub

JSystems przeprowadził sesję szkoleniową dotyczącą operacji Git i GitHub, koncentrując się na wprowadzaniu zmian kodu do repozytoriów. Dyskusja obejmowała kwestie techniczne związane z uprawnieniami GitHub CLI i połączeniami repozytoriów, a JSystems dostarczał krok po kroku wskazówek dotyczących rozwiązywania problemów z uwierzytelnianiem i tokenami dostępu. Piotr podziękował za szkolenie i wspomniał o konieczności przygotowania się do przyszłych sesji, natomiast Grzegorz napotkał trudności techniczne z operacjami Gita wymagające rozwiązywania problemów. Sesja zakończyła się tym, że JSystems podkreślił znaczenie odpowiednich uprawnień GitHub i udostępnił uczestnikom link do aktualizacji osobistych tokenów dostępu.

### Demonstracja funkcji Claude AI

JSystems zademonstrował różne funkcje Claude AI, w tym Agent View, agent Teams i Workflows, wyjaśniając, w jaki sposób narzędzia te mogą zarządzać wieloma agentami i automatyzować zadania. Dyskusja obejmowała praktyczną demonstrację, podczas której Grzegorz z powodzeniem wprowadził zmiany kodu do repozytorium, choć napotkał kilka konfliktów wymagających rozwiązania. JSystems podkreślił, że choć agenci AI oferują potężne możliwości, wymagają odpowiedniej dokumentacji i zrozumienia podstawowych systemów, zauważając, że oczekiwania biznesowe czasami przekraczają obecne możliwości AI. Sesja zakończyła się prośbą firmy JSystems o informacje zwrotne za pośrednictwem ankiety i wspomnieniem, że dłuższy 4-dniowy format szkolenia może być bardziej idealny dla przyszłych sesji.
