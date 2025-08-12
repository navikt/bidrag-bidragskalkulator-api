package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtalePdfService
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtaleService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.bidrag.commons.util.secureLogger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/v1/privat-avtale")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
@Profile("!prod")
class PrivatAvtaleController(
    private val privatAvtalePdfService: PrivatAvtalePdfService,
    private val privatAvtaleService: PrivatAvtaleService,
    private val innloggetBrukerUtils: InnloggetBrukerUtils
) {

    private val logger = LoggerFactory.getLogger(PrivatAvtaleController::class.java)

    @Operation(
        summary = "Henter informasjon for opprettelse av privat avtale",
        description = "Henter informasjon for opprettelse av privat avtale. Returnerer 200 ved vellykket henting, eller passende feilkoder.",
        security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Privat avtale informasjon hentet vellykket"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang - mangler eller ugyldig token"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @GetMapping("/informasjon")
    fun hentInformasjonForPrivatAvtale(): PrivatAvtaleInformasjonDto {
        logger.info("Henter informasjon for opprettelse av privat avtale")

        val personIdent = innloggetBrukerUtils.hentPåloggetPersonIdent()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ugyldig token")

        return runBlocking(Dispatchers.IO + MDCContext()) {
            secureLogger.info { "Henter informasjon om pålogget person $personIdent til bruk i en privat avtale" }

            privatAvtaleService.hentInformasjonForPrivatAvtale(personIdent).also {
                secureLogger.info { "Henter informasjon om pålogget person $personIdent til bruk i en privat avtale fullført" }
            }
        }
    }

    @PostMapping(produces = [MediaType.APPLICATION_PDF_VALUE])
    @Operation(
        summary = "Generer privat avtale PDF",
        description = "Genererer en privat avtale i PDF-format.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Privat avtale PDF generert"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @Validated
    fun genererPrivatAvtale(@Valid @RequestBody privatAvtalePdfDto: PrivatAvtalePdfDto): ResponseEntity<ByteArray>? {

        val personIdent = innloggetBrukerUtils.hentPåloggetPersonIdent()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ugyldig token")

        if (privatAvtalePdfDto.andreBestemmelser.harAndreBestemmelser && privatAvtalePdfDto.andreBestemmelser.beskrivelse.isNullOrBlank()) {
            throw IllegalArgumentException("Feltet 'andreBestemmelserTekst' er påkrevd når 'harAndreBestemmelser' er true.")
        }

        val genererPrivatAvtalePdf =  privatAvtalePdfService.genererPrivatAvtalePdf(personIdent, privatAvtalePdfDto)

        val privatAvtaleByteArray = genererPrivatAvtalePdf.toByteArray()

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "inline; filename=\"privatavtale.pdf\"")
            .body(privatAvtaleByteArray)
    }
}
