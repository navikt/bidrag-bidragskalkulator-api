package no.nav.bidrag.bidragskalkulator.dto

data class GrunnlagResponseDto(
    val ainntektListe: List<AinntektListeDto>? = null
)

data class AinntektListeDto(
    val personId: String,
    val periodeFra: String,
    val periodeTil: String,
    val ainntektspostListe: List<AinntektspostDto>
)

data class AinntektspostDto(
    val utbetalingsperiode: String,
    val opptjeningsperiodeFra: String,
    val opptjeningsperiodeTil: String,
    val opplysningspliktigId: String,
    val virksomhetId: String,
    val fordelType: String,
    val beskrivelse: String,
    val etterbetalingsperiodeFra: String,
    val etterbetalingsperiodeTil: String,
    val kategori: String, // Example: LOENNSINNTEKT
    val beløp: Int
)

