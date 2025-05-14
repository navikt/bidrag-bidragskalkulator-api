package no.nav.bidrag.bidragskalkulator.validering

import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.exception.UgyldigBeregningRequestException

object BeregningRequestValidator {
    fun valider(dto: BeregningRequestDto) {
        val typer = dto.barn.map { it.bidragstype }.toSet()
        require(!(typer.contains(BidragsType.PLIKTIG) && dto.dinBoforhold == null ||
                typer.contains(BidragsType.MOTTAKER) && dto.medforelderBoforhold == null)) {
            throw UgyldigBeregningRequestException("Boforhold må være satt når det finnes barn med bidragstype PLIKTIG eller MOTTAKER")
        }
    }
}