package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.servlet.http.HttpServletRequest
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnUnder18RequestDto
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtalePdfService
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtaleService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import org.slf4j.LoggerFactory
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnOver18RequestDto
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
import kotlin.time.measureTimedValue


@RestController
@RequestMapping("/api/v1/privat-avtale")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
@Profile("!prod")
class PrivatAvtaleController(
    private val request: HttpServletRequest,
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
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel – valideringsfeil eller ugyldig enumverdi"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang – mangler eller ugyldig token"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @GetMapping("/informasjon")
    fun hentInformasjonForPrivatAvtale(): PrivatAvtaleInformasjonDto {
        logger.info("Starter henting av informasjon for opprettelse av privat avtale (endepunkt=${request.requestURI})")

        val personIdent = innloggetBrukerUtils.requirePåloggetPersonIdent(logger)

        val (resultat, varighet) = measureTimedValue {
            runBlocking(Dispatchers.IO + MDCContext()) {
                privatAvtaleService.hentInformasjonForPrivatAvtale(personIdent)
            }
        }

        logger.info("Fullført henting av informasjon for opprettelse av privat avtale (varighet_ms=${varighet.inWholeMilliseconds})")
        return resultat
    }

    @PostMapping("/under-18", produces = [MediaType.APPLICATION_PDF_VALUE])
    @Operation(
        summary = "Generer privat avtale PDF for barn under 18 år",
        description = "Genererer en privat avtale i PDF-format.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Privat avtale PDF generert"),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel – valideringsfeil eller ugyldig enumverdi"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang – mangler eller ugyldig token"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @Validated
    fun genererPrivatAvtaleForBarnUnder18(@Valid @RequestBody dto: PrivatAvtaleBarnUnder18RequestDto): ResponseEntity<ByteArray>? {
        logger.info("Start generere privat avtale PDF for barn under 18 år (endepunkt=${request.requestURI})")

        val personIdent = innloggetBrukerUtils.requirePåloggetPersonIdent(logger)

        val (resultat, varighet) = measureTimedValue {
            runBlocking(Dispatchers.IO + MDCContext()) {
                privatAvtalePdfService.genererPrivatAvtalePdf(personIdent, dto)
            }
        }

        logger.info("Fullført generering av privat avtale PDF for barn under 18 år (varighet_ms=${varighet.inWholeMilliseconds})")

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "inline; filename=\"privatavtale.pdf\"")
            .body(resultat.toByteArray())
    }

    @PostMapping("/over-18", produces = [MediaType.APPLICATION_PDF_VALUE])
    @Operation(
        summary = "Generer privat avtale PDF for barn over 18 år",
        description = "Genererer en privat avtale i PDF-format.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Privat avtale PDF generert"),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel – valideringsfeil eller ugyldig enumverdi"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang – mangler eller ugyldig token"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @Validated
    fun genererPrivatAvtaleForBarnOver18(@Valid @RequestBody dto: PrivatAvtaleBarnOver18RequestDto): ResponseEntity<ByteArray>? {
        logger.info("Start generere privat avtale PDF for barn over 18 år (endepunkt=${request.requestURI})")

        val personIdent = innloggetBrukerUtils.requirePåloggetPersonIdent(logger)

        val(resultat, varighet) = measureTimedValue {
            runBlocking(Dispatchers.IO + MDCContext()) {
                privatAvtalePdfService.genererPrivatAvtalePdf(personIdent, dto)
            }
        }

        logger.info("Fullført generering av privat avtale PDF for barn over 18 år (varighet_ms=${varighet.inWholeMilliseconds})")

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "inline; filename=\"privatavtale.pdf\"")
            .body(resultat.toByteArray())
    }
}
