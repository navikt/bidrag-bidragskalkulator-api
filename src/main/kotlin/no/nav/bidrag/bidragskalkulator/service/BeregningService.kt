package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.controller.dto.BeregningResultatDto
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi
) {

    fun beregnBarneBidrag(): BeregningResultatDto {

        val resultat = beregnBarnebidragApi.beregn(
            BeregnGrunnlag(
                stønadstype = Stønadstype.BIDRAG,
                periode = ÅrMånedsperiode("2025-01", "2025-02"),
                søknadsbarnReferanse = "",
                opphørSistePeriode = false,
                grunnlagListe = emptyList(),
            )
        )
        return BeregningResultatDto(resultat = resultat.beregnetBarnebidragPeriodeListe.first().resultat.beløp?.toDouble() ?: 0.0)
    }
}