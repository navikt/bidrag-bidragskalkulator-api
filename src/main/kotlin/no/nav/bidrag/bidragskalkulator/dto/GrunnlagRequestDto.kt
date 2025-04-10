package no.nav.bidrag.bidragskalkulator.dto

data class GrunnlagRequestDto(
    val formaal: String, // Eks: FORSKUDD
    val grunnlagRequestDtoListe: List<GrunnlagRequestItemDto>
)

data class GrunnlagRequestItemDto(
    val type: String, // Eks: AINNTEKT
    val personId: String,
    val periodeFra: String,
    val periodeTil: String
)