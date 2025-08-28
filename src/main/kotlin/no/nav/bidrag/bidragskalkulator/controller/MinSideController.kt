package no.nav.bidrag.bidragskalkulator.controller

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

@RestController
@RequestMapping("/api/v1/minside")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
class MinSideController(
    private val safSelvbetjeningService: SafSelvbetjeningService,
    private val innloggetBrukerUtils: InnloggetBrukerUtils
) {

    private val logger = LoggerFactory.getLogger(MinSideController::class.java)

    @GetMapping("/dokumenter")
    fun hentSelvbetjeningDokumenter(): ResponseEntity<MinSideDokumenterDto> {
        val bruker = innloggetBrukerUtils.hentPåloggetPersonIdent()
            ?: throw IllegalStateException("Ugyldig token, ingen pålogget bruker ident funnet")

        logger.info("Henter dokumenter for en bruker ")

        val dokumenter = safSelvbetjeningService.hentSelvbetjeningJournalposter(bruker)

        logger.info("Ferdig hentet ${dokumenter.journalposter.size} dokumenter for brukeren")

        return ResponseEntity.ok(dokumenter)
    }

    @GetMapping("/dokumenter/{journalpostId}/{dokumentInfoId}")
    fun hentDokument(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String
    ): ResponseEntity<ByteArray> {
        logger.info("Henter dokument med journalpost- og dokumentinfo-ID for en bruker")

        try {
            val dokumentRespons = safSelvbetjeningService.hentDokument(journalpostId, dokumentInfoId)

            val headers = HttpHeaders()
            if (dokumentRespons.filnavn != null) {
                headers.setContentDispositionFormData("attachment", dokumentRespons.filnavn)
            }
            headers.contentType = MediaType.APPLICATION_PDF

            logger.info( "Ferdig hentet dokument med journalpost- og dokumentinfo-ID for brukeren")

            return ResponseEntity(dokumentRespons.dokument, headers, HttpStatus.OK)
        } catch (e: HttpClientErrorException) {
            logger.error("Feil ved henting av dokument: ${e.statusCode}")
            throw e
        } catch (e: Exception) {
            logger.error("Uventet feil ved henting av dokument", e)
            throw RuntimeException("Kunne ikke hente dokument", e)
        }
    }

}
