# 📊 Bidragskalkulator API

**Bidragskalkulator API** beregner hvor mye en forelder skal betale eller motta i barnebidrag.

Applikasjonen fungerer som backend for [bidrag-bidragskalkulator-ui](https://github.com/navikt/bidrag-bidragskalkulator-ui) og er bygget med **Spring Boot**.

---

## 🚀 Teknologi

- **Språk:** Kotlin
- **Rammeverk:** Spring Boot
- **Byggeverktøy:** Gradle
- **Dokumentasjon:** OpenAPI / Swagger

---

## 📌 Kom i gang

### 🚧 Krav

For å kjøre applikasjonen lokalt, må du ha installert:

- **Java 21**
- **Gradle**

### 📌 Kjøre applikasjonen lokalt

Du kan starte applikasjonen lokalt enten via terminalen eller direkte i din IDE.

#### 🖥️ Alternativ 1: Kjøre via terminal
Bygg og start applikasjonen med **local-profilen** ved å kjøre:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

#### 🖥️ Alternativ 2: Kjøre via IntelliJ IDEA

1. Høyreklikk på BidragBidragskalkulatorApiApplication.kt
2. Velg **More Run/Debug**
3. Klikk på **Modify Run Configuration**
4. Under **Active Profiles**, legg til "local"
5. Klikk **Apply** og deretter **OK**
6. Start BidragBidragskalkulatorApiApplication.kt

### 🧪 Testing

Alle pull requests kjører automatisk gjennom en test-pipeline som:

✔ Bygger applikasjonen <br>
✔ Kjører alle tester

```bash
./gradlew test
```

### 📜 API-dokumentasjon

API-dokumentasjonen er tilgjengelig via Swagger UI når applikasjonen kjører **lokalt**:

🔗 http://localhost:8080/swagger-ui/index.html

I **Dev-miljøet** er dokumentasjonen tilgjengelig her:

🔗 https://bidragskalkulator-api.intern.dev.nav.no/swagger-ui/index.html

### 🚀 Deployments

Per nå er applikasjonen ikke i produksjon.


### 👥 Eierskap

**Team Bidragskalkulator**, en del av **Team Bidrag** i PO Familie, er ansvarlig for vedlikehold av denne applikasjonen.

For spørsmål eller bidrag, ta kontakt med teamet i NAV sin interne Slack-kanal.
