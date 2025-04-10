package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.client.AbstractRestClient
import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GrunnlagService(
    private val consumer: BidragGrunnlagConsumer,
) {

    val logger = getLogger(GrunnlagService::class.java)

    fun hentInntektsGrunnlag(ident: String): String {
        SikkerhetsKontekst.medApplikasjonKontekst {

        }
        return SikkerhetsKontekst.medApplikasjonKontekst {
            consumer.hentGrunnlag(ident)
        }
    }


}