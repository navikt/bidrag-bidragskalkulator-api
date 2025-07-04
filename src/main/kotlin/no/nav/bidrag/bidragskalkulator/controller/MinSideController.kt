package no.nav.bidrag.bidragskalkulator.controller

import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.minSide.MinSideDokumenterDto
import no.nav.bidrag.bidragskalkulator.service.SafSelvbetjeningService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/minside")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
class MinSideController(
    private val safSelvbetjeningService: SafSelvbetjeningService,
    private val innloggetBrukerUtils: InnloggetBrukerUtils
) {

    @GetMapping("/dokumenter")
    fun hentSelvbetjeningDokumenter(): ResponseEntity<MinSideDokumenterDto> {
        // Henter identen til den innloggede brukeren
        // Hvis ingen bruker er pålogget, kaster vi en feil
        val bruker = innloggetBrukerUtils.hentPåloggetPersonIdent()
            ?: throw IllegalStateException("Ugyldig token, ingen pålogget bruker ident funnet")

        val dokumenter = safSelvbetjeningService.hentSelvbetjeningJournalposter(bruker)
        return ResponseEntity.ok(dokumenter)
    }

}