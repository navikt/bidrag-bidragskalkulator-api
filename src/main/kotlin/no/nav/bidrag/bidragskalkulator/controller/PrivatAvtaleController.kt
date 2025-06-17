package no.nav.bidrag.bidragskalkulator.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtaleService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/privat-avtale")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
class PrivatAvtaleController(
    private val privatAvtaleService: PrivatAvtaleService,
    private val innloggetBrukerUtils: InnloggetBrukerUtils
) {
    private val logger = LoggerFactory.getLogger(PrivatAvtaleController::class.java)

    @GetMapping("/informasjon")
    fun hentInformasjonForPrivatAvtale(): PrivatAvtaleInformasjonDto {
        logger.info("Henter informasjon for opprettelse av privat avtale")

        val personIdent: String = requireNotNull(innloggetBrukerUtils.hentPåloggetPersonIdent()) {
            "Brukerident er ikke tilgjengelig i token"
        }

        return runBlocking(Dispatchers.IO + MDCContext()) {
            secureLogger.info { "Henter informasjon om pålogget person $personIdent til bruk i en privat avtale" }

            privatAvtaleService.hentInformasjonForPrivatAvtale(personIdent).also {
                secureLogger.info { "Henter informasjon om pålogget person $personIdent til bruk i en privat avtale fullført" }
            }

        }

    }
}