package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.bidragskalkulator.mapper.tilAinntektsposter
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.domene.enums.rolle.Rolle
import no.nav.bidrag.inntekt.InntektApi
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.inntekt.request.TransformerInntekterRequest
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.stereotype.Service

@Service
class GrunnlagService(
    private val grunnlagConsumer: BidragGrunnlagConsumer,
    private val inntektApi: InntektApi,
) {

    val logger = getLogger(GrunnlagService::class.java)

    fun hentInntektsGrunnlag(ident: String): TransformerInntekterResponse? {
        return SikkerhetsKontekst.medApplikasjonKontekst {
            grunnlagConsumer.hentGrunnlag(ident)?.let { transformerInntekter(
                it,
                rolleInnhentetFor = Rolle.BIDRAGSPLIKTIG
            ) }
        }
    }

    private fun transformerInntekter(hentGrunnlagDto: HentGrunnlagDto, rolleInnhentetFor: Rolle): TransformerInntekterResponse {
        return inntektApi.transformerInntekter(opprettTransformerInntekterRequest(hentGrunnlagDto, rolleInnhentetFor))
    }

    private fun opprettTransformerInntekterRequest(
        innhentetGrunnlag: HentGrunnlagDto,
        rolleInhentetFor: Rolle,
    ) = TransformerInntekterRequest(
        ainntektHentetDato = innhentetGrunnlag.hentetTidspunkt.toLocalDate(),
        vedtakstidspunktOpprinneligeVedtak = emptyList(),
        ainntektsposter =
            innhentetGrunnlag.ainntektListe.flatMap {
                it.ainntektspostListe.tilAinntektsposter(
                    rolleInhentetFor,
                )
            },
        barnetilleggsliste = emptyList(),
        kontantstøtteliste = emptyList(),
        skattegrunnlagsliste = emptyList(),
        småbarnstilleggliste = emptyList(),
        utvidetBarnetrygdliste = emptyList(),
    )
}
