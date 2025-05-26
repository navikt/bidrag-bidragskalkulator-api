package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.bidragskalkulator.mapper.PersonBeregningsgrunnlag
import no.nav.bidrag.bidragskalkulator.mapper.tilBarnInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.tilPersonInformasjonDto
import no.nav.bidrag.bidragskalkulator.model.FamilieRelasjon
import no.nav.bidrag.bidragskalkulator.utils.asyncCatching
import no.nav.bidrag.bidragskalkulator.utils.avrundeTilNærmesteHundre
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,
    private val personService: PersonService
) {

    private val logger = getLogger(BeregningService::class.java)

    suspend fun beregnBarnebidrag(beregningRequest: BeregningRequestDto): BeregningsresultatDto = coroutineScope {
        logger.info("Start beregning av barnebidrag")
        val beregningsgrunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        val start = System.currentTimeMillis()
        val beregningsresultatJobb =  asyncCatching(logger, "utførBarnebidragBeregning") {
            utførBarnebidragBeregning(beregningsgrunnlag)
        }
        val duration = System.currentTimeMillis() - start

        val resultat = beregningsresultatJobb.await()
        logger.info("Ferdig beregnet barnebidrag. Beregning av ${resultat.size} barn tok $duration ms")
        BeregningsresultatDto(resultat)
    }

    suspend fun beregnBarnebidragAnonym(beregningRequest: ÅpenBeregningRequestDto): BeregningsresultatDto = coroutineScope {
        logger.info("Start beregning av barnebidrag anonym")
        val beregningsgrunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        val start = System.currentTimeMillis()
        val beregningsresultatJobb =  asyncCatching(logger, "utførBarnebidragBeregning") {
            utførBarnebidragBeregning(beregningsgrunnlag, false)
        }
        val duration = System.currentTimeMillis() - start

        val resultat = beregningsresultatJobb.await()
        logger.info("Ferdig beregnet barnebidrag anonym. Beregning av ${resultat.size} barn tok $duration ms")
        BeregningsresultatDto(resultat)
    }

    private suspend fun utførBarnebidragBeregning(grunnlag: List<PersonBeregningsgrunnlag>, skalHentePersoninformasjon: Boolean? = true): List<BeregningsresultatBarnDto> =
        coroutineScope {
            grunnlag.map { data ->
                async {
                    val beregnet = beregnBarnebidragApi.beregn(data.grunnlag)
                    val sum = beregnet.beregnetBarnebidragPeriodeListe
                        .sumOf { it.resultat.beløp ?: BigDecimal.ZERO }
                        .avrundeTilNærmesteHundre()

                    if(skalHentePersoninformasjon == true) {
                        val barn = personService.hentPersoninformasjon(data.ident)
                        BeregningsresultatBarnDto(
                            sum = sum,
                            ident = data.ident,
                            fulltNavn = barn.visningsnavn,
                            fornavn = barn.fornavn ?: barn.visningsnavn,
                            bidragstype = data.bidragsType,
                            alder = data.alder
                        )
                    }

                    BeregningsresultatBarnDto(
                        sum = sum,
                        ident = data.ident,
                        fulltNavn = "",
                        fornavn = "",
                        bidragstype = data.bidragsType,
                        alder = data.alder
                    )
                }
            }.awaitAll()
    }


    fun beregnPersonUnderholdskostnad(personident: Personident, referanse: String): BigDecimal {
        logger.info("Beregn underholdskostnad for en person")

        val underholdskostnadGrunnlag = beregningsgrunnlagMapper.mapTilUnderholdkostnadsgrunnlag(personident, referanse)

        return beregnBarnebidragApi.beregnUnderholdskostnad(underholdskostnadGrunnlag)
            .firstOrNull { it.type == Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD }
            ?.innholdTilObjekt<DelberegningUnderholdskostnad>()
            ?.underholdskostnad
            ?: BigDecimal.ZERO.also { logger.info("Ferdig beregnet underholdskostnad for en person") }
    }

    /**
     * Parallelt beregn underholdskostnad for hvert barn i familierelasjonen.
     * Posisjon brukes som referanse for grunnlagsbeskrivelse.
     */
    suspend fun beregnUnderholdskostnaderForBarnerelasjoner(
        barnerelasjoner: List<FamilieRelasjon>
    ): List<BarneRelasjonDto> = coroutineScope {
        barnerelasjoner.mapIndexed { i, relasjon ->  beregnUnderholdskostnadForRelasjon(relasjon, i) }
    }

    private suspend fun beregnUnderholdskostnadForRelasjon(
        relasjon: FamilieRelasjon,
        relasjonsIndex: Int
    ): BarneRelasjonDto = coroutineScope {
        val fellesBarnMedUnderholdskostnad = relasjon.fellesBarn.mapIndexed { barnIndex, barn ->
            async {
                val beskrivelse = "Person_Søknadsbarn_${relasjonsIndex}${barnIndex}"
                val underholdskostnad = beregnPersonUnderholdskostnad(barn.ident, beskrivelse)

                barn.tilBarnInformasjonDto(underholdskostnad)
            }
        }.awaitAll().sortedByDescending { it.alder }

        BarneRelasjonDto(relasjon.motpart.tilPersonInformasjonDto(), fellesBarnMedUnderholdskostnad)
    }

}