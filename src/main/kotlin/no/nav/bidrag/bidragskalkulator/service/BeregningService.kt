package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BarneRelasjonDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.mapper.*
import no.nav.bidrag.bidragskalkulator.model.FamilieRelasjon
import no.nav.bidrag.bidragskalkulator.utils.asyncCatching
import no.nav.bidrag.bidragskalkulator.utils.avrundeTilNærmesteHundre
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val underholdskostnadService: UnderholdskostnadService,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,
    private val personService: PersonService
) {

    private val logger = getLogger(BeregningService::class.java)

    suspend fun beregnBarnebidrag(beregningRequest: BeregningRequestDto): BeregningsresultatDto {
        logger.info("Start beregning av barnebidrag")
        val beregningsgrunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        val start = System.currentTimeMillis()
        val beregningsresultat = utførBarnebidragBeregning(beregningsgrunnlag)
        val duration = System.currentTimeMillis() - start

        logger.info("Ferdig beregnet barnebidrag. Beregning av ${beregningsresultat.size} barn tok $duration ms")
        return BeregningsresultatDto(beregningsresultat)
    }

    suspend fun beregnBarnebidragAnonym(beregningRequest: ÅpenBeregningRequestDto): ÅpenBeregningsresultatDto {
        logger.info("Start beregning av barnebidrag anonym")
        val beregningsgrunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)

        val start = System.currentTimeMillis()
        val beregningsresultat = utførBarnebidragBeregningAnonym(beregningsgrunnlag)
        val duration = System.currentTimeMillis() - start

        logger.info("Ferdig beregnet barnebidrag anonym. Beregning av ${beregningsresultat.size} barn tok $duration ms")
        return ÅpenBeregningsresultatDto(beregningsresultat)
    }

    private suspend fun utførBarnebidragBeregning(grunnlag: List<PersonBeregningsgrunnlag>): List<BeregningsresultatBarnDto> =
        // vurder supervisorScope i stedet for coroutineScope dersom en feil ikke skal kansellerer alle
        coroutineScope {
            grunnlag.map { data ->
                asyncCatching(logger, "utførBarnebidragBeregning") {
                    val beregnet = beregnBarnebidragApi.beregn(data.grunnlag)
                    val sum = summerBeregnedeBeløp(beregnet.beregnetBarnebidragPeriodeListe)

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
            }.awaitAll()
    }

    private suspend fun utførBarnebidragBeregningAnonym(grunnlag: List<PersonBeregningsgrunnlagAnonym>): List<ÅpenBeregningsresultatBarnDto> =
        coroutineScope {
            grunnlag.map { data ->
                asyncCatching(logger, "utførBarnebidragBeregningAnonym") {
                    val beregnet = beregnBarnebidragApi.beregn(data.grunnlag)
                    val sum = summerBeregnedeBeløp(beregnet.beregnetBarnebidragPeriodeListe)

                    ÅpenBeregningsresultatBarnDto(
                        sum = sum,
                        bidragstype = data.bidragsType,
                        alder = data.alder
                    )
                }
            }.awaitAll()
        }

    fun beregnPersonUnderholdskostnad(personident: Personident): BigDecimal {
        val alder = kalkulerAlder(personident.fødselsdato())
        return underholdskostnadService.beregnCachedPersonUnderholdskostnad(alder)
    }

    /**
     * Parallelt beregn underholdskostnad for hvert barn i familierelasjonen.
     * Posisjon brukes som referanse for grunnlagsbeskrivelse.
     */
    suspend fun beregnUnderholdskostnaderForBarnerelasjoner(
        barnerelasjoner: List<FamilieRelasjon>
    ): List<BarneRelasjonDto> = coroutineScope {
        barnerelasjoner.map {  beregnUnderholdskostnadForRelasjon(it) }
    }

    private suspend fun beregnUnderholdskostnadForRelasjon(
        relasjon: FamilieRelasjon,
    ): BarneRelasjonDto = coroutineScope {
        val fellesBarnMedUnderholdskostnad = relasjon.fellesBarn.mapIndexed { barnIndex, barn ->
            async {
                val underholdskostnad = beregnPersonUnderholdskostnad(barn.ident)

                barn.tilBarnInformasjonDto(underholdskostnad)
            }
        }.awaitAll().sortedByDescending { it.alder }

        BarneRelasjonDto(relasjon.motpart.tilPersonInformasjonDto(), fellesBarnMedUnderholdskostnad)
    }

    private fun summerBeregnedeBeløp(periodeListe: List<ResultatPeriode>): BigDecimal =
        periodeListe.sumOf { it.resultat.beløp ?: BigDecimal.ZERO }.avrundeTilNærmesteHundre()

}
