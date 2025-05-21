package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.bidragskalkulator.mapper.PersonBeregningsgrunnlag
import no.nav.bidrag.bidragskalkulator.utils.asyncCatching
import no.nav.bidrag.bidragskalkulator.utils.avrundeTilNærmesteHundre
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
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
        val resultatDefer =  asyncCatching(logger, "utførBarnebidragBeregning") {
            utførBarnebidragBeregning(beregningsgrunnlag)
        }
        val duration = System.currentTimeMillis() - start

        val resultat = resultatDefer.await()
        logger.info("Ferdig beregnet barnebidrag. Beregning av ${resultat.size} barn tok $duration ms")
        BeregningsresultatDto(resultat)
    }

    private suspend fun utførBarnebidragBeregning(grunnlag: List<PersonBeregningsgrunnlag>): List<BeregningsresultatBarnDto> =
        coroutineScope {
            grunnlag.map { data ->
                async {
                    val beregnet = beregnBarnebidragApi.beregn(data.grunnlag)
                    val sum = beregnet.beregnetBarnebidragPeriodeListe
                        .sumOf { it.resultat.beløp ?: BigDecimal.ZERO }
                        .avrundeTilNærmesteHundre()

                    val barn = personService.hentPersoninformasjon(data.ident)

                    BeregningsresultatBarnDto(
                        sum = sum,
                        ident = data.ident,
                        fulltNavn = barn.visningsnavn,
                        fornavn = barn.fornavn ?: barn.visningsnavn,
                        bidragstype = data.bidragsType
                    )
                }
            }.awaitAll()
    }


    fun beregnPersonUnderholdskostnad(personident: Personident, referanse: String): BigDecimal {
        val underholdskostnadGrunnlag = beregningsgrunnlagMapper.mapTilUnderholdkostnadsgrunnlag(personident, referanse)

        return beregnBarnebidragApi.beregnUnderholdskostnad(underholdskostnadGrunnlag)
            .firstOrNull { it.type == Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD }
            ?.innholdTilObjekt<DelberegningUnderholdskostnad>()
            ?.underholdskostnad
            ?: BigDecimal.ZERO
    }

    /**
     * Parallelt beregn underholdskostnad for hvert barn i familierelasjonen.
     * Posisjon brukes som referanse for grunnlagsbeskrivelse.
     */
    suspend fun beregnUnderholdskostnaderForBarnIFamilierelasjon(familierelasjon: MotpartBarnRelasjonDto): List<BarnUnderholdskostnad> =
        coroutineScope {
            familierelasjon.personensMotpartBarnRelasjon.flatMapIndexed { i, relasjon ->
                relasjon.fellesBarn.mapIndexed { j, barn ->
                    async {
                        val beskrivelse = "Person_Søknadsbarn_${i}${j}"
                        val underholdskostnad = beregnPersonUnderholdskostnad(barn.ident, beskrivelse)
                        BarnUnderholdskostnad(barn.ident, underholdskostnad)
                    }
                }
            }.awaitAll()
        }
}

data class BarnUnderholdskostnad(
    val barnIdent: Personident,
    val underholdskostnad: BigDecimal,
)