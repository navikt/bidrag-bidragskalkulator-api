package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,
) {

    private val logger = getLogger(BeregningsgrunnlagMapper::class.java)

    fun beregnBarnebidrag(beregningRequest: BeregningRequestDto): BeregningsresultatDto {
        val beregningsgrunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        val start = System.currentTimeMillis()
        val beregningsresultat = beregningsgrunnlag.parallelStream().map { data ->
            val beregningsResult = beregnBarnebidragApi.beregn(data.grunnlag)
            val beregnetSum = beregningsResult
                .beregnetBarnebidragPeriodeListe
                .sumOf { it.resultat.beløp ?: BigDecimal.ZERO }

            val underholdskostnad = beregningsResult.grunnlagListe
                .firstOrNull { it.type == Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD }
                ?.innholdTilObjekt<DelberegningUnderholdskostnad>()?.underholdskostnad ?: BigDecimal.ZERO

            BeregningsresultatBarnDto(
                sum = beregnetSum.avrundeTilNærmesteHundre(),
                ident = data.ident,
                fulltNavn = data.fulltNavn,
                alder = data.alder,
                underholdskostnad = underholdskostnad,
                bidragstype = data.bidragsType,
            )
        }.toList()
        
        val duration = System.currentTimeMillis() - start;
        logger.info("Beregning av ${beregningsresultat.size} barn tok $duration ms")

        return BeregningsresultatDto(beregningsresultat)
    }

    fun BigDecimal.avrundeTilNærmesteHundre() = this.divide(BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
}

