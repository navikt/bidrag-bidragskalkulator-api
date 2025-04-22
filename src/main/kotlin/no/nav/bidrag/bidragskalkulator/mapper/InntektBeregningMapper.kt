package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.InntektResultatDto
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import java.math.BigDecimal

/**
 * Mapper for å konvertere inntektsgrunnlag fra TransformerInntekterResponse til InntektResultatDto.
 */
fun TransformerInntekterResponse.toInntektResultatDto() =
    InntektResultatDto(
        inntektSiste12Mnd = this.summertÅrsinntektListe
            .find { it.inntektRapportering == Inntektsrapportering.AINNTEKT_BEREGNET_12MND }
            ?.sumInntekt
            ?: BigDecimal(0),
        inntektSiste3Mnd = this.summertÅrsinntektListe
            .find { it.inntektRapportering == Inntektsrapportering.AINNTEKT_BEREGNET_3MND }
            ?.sumInntekt
            ?: BigDecimal(0),
    )