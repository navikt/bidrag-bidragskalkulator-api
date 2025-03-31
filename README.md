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
Bygg og start applikasjonen med **local profilen** ved å kjøre:

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

### 📌 Kjøre applikasjonen lokalt mot sky

For å kjøre applikasjonen lokalt mot sky, følg disse stegene:  

#### 🖥️ Alternativ 1: Kjøre skript

Skripten vil sette nødvendige miljøvariabler, starte applikasjonen, og deretter åpne både Swagger og TokenX generator (se punkt 4. Generer token nedenfor).

```bash
./local-cloud-run.sh
```

#### 🖥️ Alternativ 2: Manuell kjøring av applikasjon

### 1. Konfigurer kubectl til `dev-gcp`

Åpne terminalen i rotmappen til `bidrag-bidragskalkulator-api` og konfigurer kubectl til å bruke `dev-gcp`-klusteret:

```bash
# Sett cluster til dev-gcp
kubectx dev-gcp

# Sett namespace til bidrag
kubens bidrag 

# -- Eller hvis du ikke har kubectx/kubens installert 
# (da må -n=bidrag legges til etter exec i neste kommando)
kubectl config use dev-gcp
```

### 2. Importer secrets

For å hente nødvendige secrets, kjør følgende kommando:

```bash
kubectl exec --tty deployment/bidrag-bidragskalkulator-api -- printenv \
  | grep -E 'TOKEN_X_WELL_KNOWN_URL|TOKEN_X_CLIENT_ID' \
  > src/test/resources/application-local-nais.properties
```

⚠ **_Viktig_**: Filen som opprettes (application-local-nais.properties) må ikke committes til Git.

### 3. Start applikasjonen med local-nais-profil

Du kan starte applikasjonen på to måter:

**✅ Via terminal:**
```bash
./gradlew bootRun --args='--spring.profiles.active=local-nais'
```

✅ **Eller kjør BidragBidragskalkulatorApiApplication.kt** i en IDE med profilen local-nais.

### 4. Generer token

For å generere et gyldig token, gå til:

🔗 https://tokenx-token-generator.intern.dev.nav.no/api/obo?aud=<audience>

Erstatt <audience> med verdien av `TOKEN_X_CLIENT_ID` fra application-local-nais.properties (steg 2).
Eller settes til:

`<cluster>:<namespace>:<application>`

Eksempel:

`dev-gcp:my-team:my-app`

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
