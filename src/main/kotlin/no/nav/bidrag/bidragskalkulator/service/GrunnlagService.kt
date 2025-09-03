package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.bidragskalkulator.exception.InntektTransformException
import no.nav.bidrag.bidragskalkulator.mapper.tilAinntektsposter
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.inntekt.InntektApi
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.inntekt.request.TransformerInntekterRequest
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import org.springframework.stereotype.Service
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

@Service
class GrunnlagService(
    private val grunnlagConsumer: BidragGrunnlagConsumer,
    private val inntektApi: InntektApi,
) {

    fun hentInntektsGrunnlag(ident: String): TransformerInntekterResponse? =
        SikkerhetsKontekst.medApplikasjonKontekst {
            logger.info { "Henter inntektsgrunnlag" }
            val grunnlag = grunnlagConsumer.hentGrunnlag(ident)

            try {
                val (inntektsgrunnlag, varighet) = measureTimedValue {
                    inntektApi.transformerInntekter(opprettTransformerInntekterRequest(grunnlag))
                }
                logger.info { "Transformering OK (ms=${varighet.inWholeMilliseconds})" }
                inntektsgrunnlag
            } catch (e: Exception) {
                logger.error { "Transformering i inntekt-api feilet" }
                secureLogger.error(e) { "Transformering feilet: ${e.message}" }
                throw InntektTransformException("Feil ved kall til inntekt-api", e)
            }
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
