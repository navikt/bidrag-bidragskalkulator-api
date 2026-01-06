package no.nav.bidrag.bidragskalkulator.validering

import no.nav.bidrag.bidragskalkulator.dto.BidragsType.*
import no.nav.bidrag.bidragskalkulator.dto.FellesBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.IFellesBarnDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.BarnMedAlderDto
import no.nav.bidrag.bidragskalkulator.exception.UgyldigBeregningRequestException

object BeregningRequestValidator {
    private const val MAKS_ALDER_FOR_BARNETILSYN = 10

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

        val barneliste = dto.barn

        // Barnetilsyn-regler:

        barneliste
            .filterIsInstance<BarnMedAlderDto>()
            .forEach { barn ->
                val barnetilsyn = barn.barnetilsyn ?: return@forEach

                // 1) Alder: barnetilsyn er ikke tillatt over 10 år
                if (barn.alder > MAKS_ALDER_FOR_BARNETILSYN) {
                    feil("Barnetilsyn kan ikke oppgis for barn over $MAKS_ALDER_FOR_BARNETILSYN år (barnets alder=${barn.alder}).")
                }

                // 2) Enten/eller: kan ikke sende både månedligUtgift og plassType
                val harMånedligUtgift = barnetilsyn.månedligUtgift != null
                val harPlassType = barnetilsyn.plassType != null
                if (harMånedligUtgift && harPlassType) {
                    feil("Ugyldig barnetilsyn: kan ikke oppgi både månedligUtgift og plassType samtidig.")
                }
            }

        // Kontantstøtte
        barneliste.forEachIndexed { index, barn ->
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
            val harBarn0Til3År = barneliste
                .mapNotNull { (it as? BarnMedAlderDto)?.alder }
                .any { it in 0..3 }

            if (!harBarn0Til3År) {
                feil("småbarnstillegg kan kun settes til true når det finnes minst ett barn med alder 0-3 år")
            }
        }

        // Boforhold
        val harPliktigeBarn = barneliste.any { it.bidragstype == PLIKTIG }
        val harMottakerBarn = barneliste.any { it.bidragstype == MOTTAKER }

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
