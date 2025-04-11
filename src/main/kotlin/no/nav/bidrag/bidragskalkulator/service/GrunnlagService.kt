package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.bidragskalkulator.dto.GrunnlagResponseDto
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.stereotype.Service

@Service
class GrunnlagService(
    private val consumer: BidragGrunnlagConsumer,
) {

    val logger = getLogger(GrunnlagService::class.java)

    fun hentInntektsGrunnlag(ident: String): GrunnlagResponseDto? {
        return SikkerhetsKontekst.medApplikasjonKontekst {
            consumer.hentGrunnlag(ident)
        }
    }


}