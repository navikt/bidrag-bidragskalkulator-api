package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,
) {

    fun beregnBarnebidrag(beregningRequest: BeregningRequestDto): BeregningsresultatDto {
        val beregningsgrunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        val beregningsresultat = beregningsgrunnlag.map { data ->
            BeregningsresultatBarnDto(
                sum = beregnBarnebidragApi.beregn(data.grunnlag)
                    .beregnetBarnebidragPeriodeListe
                    .sumOf { it.resultat.beløp ?: BigDecimal.ZERO },
                barnetsAlder = data.barnetsAlder
            )
        }

        return BeregningsresultatDto(beregningsresultat)
    }
}