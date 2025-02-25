FROM ghcr.io/navikt/baseimages/temurin:21
LABEL maintainer="Team Bidrag" \
      email="bidrag@nav.no"

COPY ./build/libs/bidrag-bidragskalkulator-api-*.jar app.jar
EXPOSE 8080
