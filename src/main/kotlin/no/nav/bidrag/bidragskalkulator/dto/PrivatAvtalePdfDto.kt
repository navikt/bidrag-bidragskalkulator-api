package no.nav.bidrag.bidragskalkulator.dto

interface BidragPerson {
    val fornavn: String
    val etternavn: String
    val fodselsnummer: String
}

data class Bidragsmottaker(
    override val fornavn: String,
    override val etternavn: String,
    override val fodselsnummer: String
) : BidragPerson

data class Bidragspliktig(
    override val fornavn: String,
    override val etternavn: String,
    override val fodselsnummer: String
) : BidragPerson

data class Barn(
    override val fornavn: String,
    override val etternavn: String,
    override val fodselsnummer: String,
    val sumBidrag: Double  // Beløp in NOK
) : BidragPerson

// You might want to create an enum for Oppgjorsform
enum class Oppgjorsform {
    // Add your enum values here
}

data class PrivatAvtalePdfDto(
    val innhold: String,
    val bidragsmottaker: Bidragsmottaker,
    val bidragspliktig: Bidragspliktig,
    val barn: Barn,
    val fraDato: String,  // Consider using LocalDate instead of String
    val nyAvtale: Boolean,
    val oppgjorsform: String  // Consider using the Oppgjorsform enum instead of String
)
