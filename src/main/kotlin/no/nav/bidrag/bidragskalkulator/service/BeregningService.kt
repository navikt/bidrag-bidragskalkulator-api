package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.controller.dto.BeregningResultatDto
import no.nav.bidrag.bidragskalkulator.controller.dto.EnkelBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.mapping.BeregnGrunnlagMapper
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregnGrunnlagMapper: BeregnGrunnlagMapper,
) {

    fun beregnBarneBidrag(beregningRequest: EnkelBeregningRequestDto): BeregningResultatDto {
        val beregnGrunlagList = beregnGrunnlagMapper.mapToBeregnGrunnlag(beregningRequest)
        val beregnetBarnebidragResultat = beregnGrunlagList.map { beregnGrunnlag -> beregnBarnebidragApi.beregn((beregnGrunnlag)) }

        val sum = beregnetBarnebidragResultat
            .flatMap { it.beregnetBarnebidragPeriodeListe }
            .mapNotNull { it.resultat.beløp }
            .fold(BigDecimal.ZERO) { acc, value -> acc + value }

        return BeregningResultatDto(resultat = sum)
    }
}