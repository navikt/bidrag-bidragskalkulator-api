package no.nav.bidrag.bidragskalkulator.validering

import no.nav.bidrag.bidragskalkulator.dto.BidragsType.*
import no.nav.bidrag.bidragskalkulator.dto.FellesBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.IFellesBarnDto
import no.nav.bidrag.bidragskalkulator.exception.UgyldigBeregningRequestException

object BeregningRequestValidator {

    fun <T : IFellesBarnDto, R : FellesBeregningRequestDto<T>> valider(dto: R) {
        val harPliktigeBarn = dto.barn.any { it.bidragstype == PLIKTIG }
        val harMottakerBarn = dto.barn.any { it.bidragstype == MOTTAKER }

        val manglerDittBoforhold = harPliktigeBarn && dto.dittBoforhold == null
        val manglerMedforelderBoforhold = harMottakerBarn && dto.medforelderBoforhold == null

        when {
            manglerDittBoforhold && manglerMedforelderBoforhold ->
                feil("Både 'dittBoforhold' og 'medforelderBoforhold' mangler, men må være satt når forespørselen inneholder barn der du er bidragspliktig og/eller bidragsmottaker.")
            manglerDittBoforhold ->
                feil("'dittBoforhold' må være satt fordi forespørselen inneholder minst ett barn der du er bidragspliktig.")
            manglerMedforelderBoforhold ->
                feil("'medforelderBoforhold' må være satt fordi forespørselen inneholder minst ett barn der du er bidragsmottaker.")
        }
    }

    private fun feil(melding: String): Nothing =
        throw UgyldigBeregningRequestException(melding)
}