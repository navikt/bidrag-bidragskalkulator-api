# Bidragskalkulator API

Denne applikasjonen gjør det mulig for borgere å kalkulere hvor mye de skal betale eller motta i barnebidrag.

Den fungerer som et API for [frontend-applikasjonen](/navikt/bidrag-bidragskalkulator-ui), og er bygget med Spring Boot.

## Deployments

Enn så lenge er ikke denne applikasjonen deployet noe sted.

## Utvikling

### Testing
Alle pull requests kjører automatisk gjennom en test-workflow som:
- Bygger applikasjonen
- Kjører alle tester
- Publiserer testresultater i PR-en

For å kjøre testene lokalt:
```bash
./gradlew test
```

## Eierskap

Appen er vedlikeholdt av Team Bidragskalkulator, som er et subteam til Team Bidrag i PO Familie.
