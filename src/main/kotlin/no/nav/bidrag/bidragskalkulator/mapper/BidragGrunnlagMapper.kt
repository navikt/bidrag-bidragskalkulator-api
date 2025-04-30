package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.transport.behandling.grunnlag.response.AinntektspostDto
import no.nav.bidrag.transport.behandling.inntekt.request.Ainntektspost

fun List<AinntektspostDto>.tilAinntektsposter() =
    this.map {
        Ainntektspost(
            beløp = it.beløp,
            beskrivelse = it.beskrivelse,
            opptjeningsperiodeFra = it.opptjeningsperiodeFra,
            opptjeningsperiodeTil = it.opptjeningsperiodeTil,
            utbetalingsperiode = it.utbetalingsperiode,
            referanse = "", // ikke nødvendig
            etterbetalingsperiodeTil = it.etterbetalingsperiodeTil,
            etterbetalingsperiodeFra = it.etterbetalingsperiodeFra,
        )
    }

