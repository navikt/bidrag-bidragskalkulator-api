package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BeregningResultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningResultatPerBarnDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregnGrunnlagMapper
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregnGrunnlagMapper: BeregnGrunnlagMapper,
) {

    fun beregnBarnebidrag(beregningRequest: BeregningRequestDto): BeregningResultatDto {
        val beregnGrunnlag = beregnGrunnlagMapper.mapToBeregnGrunnlag(beregningRequest)

        val beregningResultatPerBarn = beregnGrunnlag.map { data ->
            BeregningResultatPerBarnDto(
                resultat = beregnBarnebidragApi.beregn(data.beregnGrunnlag)
                    .beregnetBarnebidragPeriodeListe
                    .sumOf { it.resultat.beløp ?: BigDecimal.ZERO },
                barnetsAlder = data.barnetsAlder
            )
        }

        return BeregningResultatDto(beregningResultatPerBarn)
    }
}