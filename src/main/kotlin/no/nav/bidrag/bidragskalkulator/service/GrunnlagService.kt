package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
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

    fun hentInntektsGrunnlag(ident: String): TransformerInntekterResponse? = SikkerhetsKontekst.medApplikasjonKontekst {
        runCatching {
            logger.info { "Henter inntektsgrunnlag" }
            val grunnlag = grunnlagConsumer.hentGrunnlag(ident)
            transformerInntekter(grunnlag)
        }.getOrElse {
            logger.info { "Henter inntektsgrunnlag feilet" }
            secureLogger.error(it) { "Henter inntektsgrunnlag feilet: ${it.message}" }
            null
        }
    }

    private fun transformerInntekter(hentGrunnlagDto: HentGrunnlagDto): TransformerInntekterResponse {
        logger.info { "Transformer inntekt i bidrag inntekt-api" }

        return try {
            val (inntekt, varighet) = measureTimedValue {
                inntektApi.transformerInntekter(opprettTransformerInntekterRequest(hentGrunnlagDto))
            }
            logger.info { "Kall til bidrag inntekt-api OK (varighet_ms=${varighet.inWholeMilliseconds})" }
            inntekt
        } catch (e: Exception) {
            logger.error{ "Transformer inntekt i bidrag inntekt-api feilet" }
            secureLogger.error(e) { "Transformer inntekt i bidrag inntekt-api feilet: ${e.message}" }

            throw RuntimeException("Feil ved kall til bidrag inntekt-api", e)
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
