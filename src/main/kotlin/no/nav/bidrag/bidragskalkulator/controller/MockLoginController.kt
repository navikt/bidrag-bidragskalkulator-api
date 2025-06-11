package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.MockLoginResponseDto
import no.nav.bidrag.bidragskalkulator.service.MockLoginService
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.ident.Personident
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Profile("!prod")
@RestController
class MockLoginController(
    private val mockLoginService: MockLoginService
) {
    @GetMapping("/api/v1/mock-login")
    @Unprotected
    @Operation(
        summary = "Genererer en innloggingstoken i dev-miljøet",
        description = "Logger inn en bruker i dev-miljøet ved å generere et mock TokenX-token basert på en ident. " +
                "Brukes for testing og utvikling uten ekte autentisering."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Token generert"),
            ApiResponse(responseCode = "400", description = "Ident på feil format"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    fun mockLogin(@RequestParam(required = true) ident: Personident): MockLoginResponseDto {
        return mockLoginService.genererMockTokenXToken(ident)
    }
}
