package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
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
import no.nav.bidrag.bidragskalkulator.utils.kalkulerAlder
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val boOgForbruksutgiftService: BoOgForbruksutgiftService,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,
    private val personService: PersonService,
) {
    suspend fun beregnBarnebidrag(beregningRequest: BeregningRequestDto): BeregningsresultatDto {
        logger.info { "Starter beregning av barnebidrag." }
        val (resultatListe, varighet) = runCatching {
            measureTimedValue {
                val grunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)
                utførBarnebidragBeregning(grunnlag)
            }
        }.onFailure { e ->
            logger.error{ "Beregning av barnebidrag feilet." }
            secureLogger.error(e) { "Beregning av barnebidrag feilet: ${e.message}" }
        }.getOrThrow()

        logger.info { "Ferdig beregnet barnebidrag. Beregning av ${resultatListe.size} barn (varighet_ms=${varighet.inWholeMilliseconds})." }
        return BeregningsresultatDto(resultatListe)
    }

    suspend fun beregnBarnebidragAnonym(beregningRequest: ÅpenBeregningRequestDto): ÅpenBeregningsresultatDto {
        logger.info { "Starter anonym beregning av barnebidrag." }
        val (resultatListe, varighet) = runCatching {
            measureTimedValue {
                val grunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlagAnonym(beregningRequest)
                utførBarnebidragBeregningAnonym(grunnlag)
            }
        }.onFailure { e ->
            logger.error{ "Anonym beregning av barnebidrag feilet." }
            secureLogger.error(e) { "Anonym beregning av barnebidrag feilet: ${e.message}" }
        }.getOrThrow()

        logger.info { "Ferdig med anonym beregning av barnebidrag. Beregning av ${resultatListe.size} barn (varighet_ms=${varighet.inWholeMilliseconds})." }
        return ÅpenBeregningsresultatDto(resultatListe)
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
                            alder = data.alder
                        )
                }
            }.awaitAll()
    }

    private suspend fun utførBarnebidragBeregningAnonym(grunnlag: List<BeregnGrunnlag>): List<ÅpenBeregningsresultatBarnDto> {
        val periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1))
        val beregnet = beregnBarnebidragApi.beregnV2(periode, grunnlag)

        return beregnet.map { it ->
            ÅpenBeregningsresultatBarnDto(
                sum = summerBeregnedeBeløp(it.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe),
                alder = alderFraBarnReferanse(it.søknadsbarnreferanse) ?: error("Ugyldig barnReferanse: ${it.søknadsbarnreferanse}")

            )
        }
    }

    fun beregnPersonUnderholdskostnad(personident: Personident): BigDecimal {
        val alder = kalkulerAlder(personident.fødselsdato())
        return boOgForbruksutgiftService.beregnCachedPersonBoOgForbruksutgiftskostnad(alder)
    }

    /**
     * Parallelt beregn underholdskostnad for hvert barn i familierelasjonen.
     * Posisjon brukes som referanse for grunnlagsbeskrivelse.
     */
    suspend fun beregnUnderholdskostnaderForBarnerelasjoner(
        barnerelasjoner: List<FamilieRelasjon>
    ): List<BarneRelasjonDto> = coroutineScope {
        logger.info { "Starter beregning av underholdskostnader for barnerelasjoner." }
        val (resultater, varighet) = runCatching {
            measureTimedValue {
                barnerelasjoner.map { beregnUnderholdskostnadForRelasjon(it) }
            }
        }.onFailure { e ->
            logger.error{ "Beregning av underholdskostnader for barnerelasjoner feilet." }
            secureLogger.error(e) { "Beregning av underholdskostnader for barnerelasjoner feilet: ${e.message}" }
        }.getOrThrow()

        logger.info { "Fullførte beregning av underholdskostnader for ${resultater.size} relasjoner (varighet_ms=${varighet.inWholeMilliseconds})." }
        resultater
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
        periodeListe.sumOf { it.resultat.beløp ?: BigDecimal.ZERO }

    private fun alderFraBarnReferanse(referanse: String): Int? {
        val prefix = BeregningsgrunnlagMapper.Referanser.SØKNADSBARN
        if (!referanse.startsWith(prefix)) return null

        val rest = referanse.removePrefix(prefix)
        if (rest.isBlank()) return null

        val alderDel = rest.substringBefore('_')

        if (alderDel.isEmpty() || alderDel.any { !it.isDigit() }) return null
        return alderDel.toInt()
    }

}
