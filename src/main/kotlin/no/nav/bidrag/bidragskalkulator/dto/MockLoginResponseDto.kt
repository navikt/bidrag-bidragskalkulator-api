package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Inneholder et token for autentisering i dev-miljøet")
data class MockLoginResponseDto(
    @Schema(description = "OBO-tokenet man sender med til dev-APIet for å gjøre forespørsler for en bruker")
    val token: String
)
