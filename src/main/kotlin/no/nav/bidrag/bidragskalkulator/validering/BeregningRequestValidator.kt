package no.nav.bidrag.bidragskalkulator.validering

import no.nav.bidrag.bidragskalkulator.dto.BidragsType.*
import no.nav.bidrag.bidragskalkulator.dto.FellesBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.IFellesBarnDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.BarnMedAlderDto
import no.nav.bidrag.bidragskalkulator.exception.UgyldigBeregningRequestException

object BeregningRequestValidator {

    fun <T : IFellesBarnDto, R : FellesBeregningRequestDto<T>> valider(dto: R) {
        // Utvidet barnetrygd
        val utvidetBarnetrygd = dto.utvidetBarnetrygd
        if (utvidetBarnetrygd != null) {
            // delerMedMedforelder er kun relevant når harUtvidetBarnetrygd = true
            // Hvis harUtvidetBarnetrygd = false, forventer vi at delerMedMedforelder er false.
            if (!utvidetBarnetrygd.harUtvidetBarnetrygd && utvidetBarnetrygd.delerMedMedforelder) {
                feil("utvidetBarnetrygd.delerMedMedforelder kan ikke være true når utvidetBarnetrygd.harUtvidetBarnetrygd = false")
            }
        }

        // Kontantstøtte
        dto.barn.forEachIndexed { index, barn ->
            val barnKontantstøtte = barn.kontantstøtte

            if (barnKontantstøtte != null) {
                val alder = (barn as? BarnMedAlderDto)?.alder
                if (alder != null && alder != 1) {
                    feil("Kontantstøtte kan kun settes for barn som er 1 år (barn[$index] har alder=$alder)")
                }
            }
        }

        // Småbarnstillegg
        if (dto.småbarnstillegg) {
            val harBarn0Til3År = dto.barn
                .mapNotNull { (it as? BarnMedAlderDto)?.alder }
                .any { it in 0..3 }

            if (!harBarn0Til3År) {
                feil("småbarnstillegg kan kun settes til true når det finnes minst ett barn med alder 0-3 år")
            }
        }

        // Boforhold
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
