package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.bidragskalkulator.mapper.tilAinntektsposter
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.inntekt.InntektApi
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.inntekt.request.TransformerInntekterRequest
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.stereotype.Service

@Service()
class GrunnlagService(
    private val grunnlagConsumer: BidragGrunnlagConsumer,
    private val inntektApi: InntektApi,
) {

    val logger = getLogger(GrunnlagService::class.java)

    fun hentInntektsGrunnlag(ident: String): TransformerInntekterResponse? {
        return SikkerhetsKontekst.medApplikasjonKontekst {
            runCatching {
                secureLogger.info { "Henter grunnlag for ident: $ident" }
                val grunnlag = grunnlagConsumer.hentGrunnlag(ident)
                secureLogger.info { "Transformerer inntekter for ident: $ident" }
                transformerInntekter(grunnlag).also { logger.info("Transformering av inntekt fullført") }
            }.getOrElse {
                secureLogger.error(it) { "Feil ved transformering av inntekter for ident: $ident - returnerer tom inntekt" }
                null
            }
        }
    }

    private fun transformerInntekter(hentGrunnlagDto: HentGrunnlagDto): TransformerInntekterResponse {
        return inntektApi.transformerInntekter(opprettTransformerInntekterRequest(hentGrunnlagDto))
    }

    private fun opprettTransformerInntekterRequest(
        innhentetGrunnlag: HentGrunnlagDto
    ) = TransformerInntekterRequest(
        ainntektHentetDato = innhentetGrunnlag.hentetTidspunkt.toLocalDate(),
        vedtakstidspunktOpprinneligeVedtak = emptyList(),
        ainntektsposter =
            innhentetGrunnlag.ainntektListe.flatMap {
                it.ainntektspostListe.tilAinntektsposter()
            },
        barnetilleggsliste = emptyList(),
        kontantstøtteliste = emptyList(),
        skattegrunnlagsliste = emptyList(),
        småbarnstilleggliste = emptyList(),
        utvidetBarnetrygdliste = emptyList(),
    )
}
