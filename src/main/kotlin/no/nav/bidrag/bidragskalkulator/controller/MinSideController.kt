package no.nav.bidrag.bidragskalkulator.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.minSide.MinSideDokumenterDto
import no.nav.bidrag.bidragskalkulator.service.SafSelvbetjeningService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.bidrag.commons.util.RequestContextAsyncContext
import no.nav.bidrag.commons.util.SecurityCoroutineContext
import no.nav.bidrag.commons.util.secureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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

        val token = innloggetBrukerUtils.hentPåloggetPersonToken()
            ?: throw HttpClientErrorException(
                HttpStatus.UNAUTHORIZED,
                "Ugyldig token, ingen pålogget bruker ident funnet"
            )


        secureLogger.info { "Henter dokumenter for bruker med ident: $bruker" }

        val dokumenter = safSelvbetjeningService.hentSelvbetjeningJournalposter(bruker, token)

        secureLogger.info { "Hentet ${dokumenter.journalposter.size} dokumenter for bruker med ident: $bruker" }

        return ResponseEntity.ok(dokumenter)
    }

}