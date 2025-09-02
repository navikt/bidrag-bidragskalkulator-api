package no.nav.bidrag.bidragskalkulator.controller

import jakarta.servlet.http.HttpServletRequest
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.minSide.MinSideDokumenterDto
import no.nav.bidrag.bidragskalkulator.service.SafSelvbetjeningService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import kotlin.time.measureTimedValue

@RestController
@RequestMapping("/api/v1/minside")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
class MinSideController(
    private val request: HttpServletRequest,
    private val safSelvbetjeningService: SafSelvbetjeningService,
    private val innloggetBrukerUtils: InnloggetBrukerUtils
) {

    private val logger = LoggerFactory.getLogger(MinSideController::class.java)

    @GetMapping("/dokumenter")
    fun hentSelvbetjeningDokumenter(): ResponseEntity<MinSideDokumenterDto> {
        logger.info("Starter henting av dokumenter for en bruker (endepunkt=${request.requestURI})")

        val bruker = innloggetBrukerUtils.requirePåloggetPersonIdent(logger)

        val (dokumenter, varighet) = measureTimedValue {
            safSelvbetjeningService.hentSelvbetjeningJournalposter(bruker)
        }

        logger.info("Fullført henting ${dokumenter.journalposter.size} dokumenter for brukeren varighet_ms=${varighet.inWholeMilliseconds})")

        return ResponseEntity.ok(dokumenter)
    }

    @GetMapping("/dokumenter/{journalpostId}/{dokumentInfoId}")
    fun hentDokument(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String
    ): ResponseEntity<ByteArray> {
        logger.info("Start henting av dokument med journalpost- og dokumentinfo-ID for en bruker (endepunkt=${request.requestURI})")

        try {
            val (resultat, varighet) = measureTimedValue {
                safSelvbetjeningService.hentDokument(journalpostId, dokumentInfoId)
            }

            val headers = HttpHeaders()
            if (resultat.filnavn != null) {
                resultat            }
            headers.contentType = MediaType.APPLICATION_PDF

            logger.info("Fullført henting av dokument med journalpost- og dokumentinfo-ID for en bruker varighet_ms=${varighet.inWholeMilliseconds})")
            return ResponseEntity(resultat.dokument, headers, HttpStatus.OK)
        } catch (e: HttpClientErrorException) {
            logger.error("Feil ved henting av dokument: ${e.statusCode}")
            throw e
        } catch (e: Exception) {
            logger.error("Uventet feil ved henting av dokument")
            throw RuntimeException("Kunne ikke hente dokument", e)
        }
    }

}
