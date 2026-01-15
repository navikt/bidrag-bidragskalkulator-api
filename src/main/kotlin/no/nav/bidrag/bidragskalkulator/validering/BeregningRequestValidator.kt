package no.nav.bidrag.bidragskalkulator.validering

import no.nav.bidrag.bidragskalkulator.dto.BidragsType.*
import no.nav.bidrag.bidragskalkulator.dto.FellesBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.IFellesBarnDto
import no.nav.bidrag.bidragskalkulator.dto.VoksneOver18Type
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
            val beløp = barnKontantstøtte?.beløp
            val deles = barnKontantstøtte?.deles

            if (deles != null && beløp == null) {
                feil("kontantstøtte.deles kan ikke settes uten at beløp også er satt (barn[$index])")
            }

            if (beløp != null) {
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
        val erPliktig = dto.bidragstype == PLIKTIG
        val erMottaker = dto.bidragstype == MOTTAKER

        val manglerDittBoforhold = erPliktig && dto.dittBoforhold == null
        val manglerMedforelderBoforhold = erMottaker && dto.medforelderBoforhold == null

        val boforhold = if(erPliktig) dto.dittBoforhold else dto.medforelderBoforhold
        val borMedBarnOver18 = boforhold?.voksneOver18Type?.contains(VoksneOver18Type.EGNE_BARN_OVER_18)
        val manglerAntallBarnOver18Vgs = borMedBarnOver18 == true && boforhold.antallBarnOver18Vgs == null

        when {
            manglerDittBoforhold ->
                feil("'dittBoforhold' må være satt fordi du er bidragspliktig i forespørselen.")
            manglerMedforelderBoforhold ->
                feil("'medforelderBoforhold' må være satt fordi du er bidragsmottaker i forespørselen.")
            manglerAntallBarnOver18Vgs ->
                feil("'antallBarnOver18Vgs' må være satt når 'voksneOver18Type' inneholder 'EGNE_BARN_OVER_18'.")
        }
    }

    private fun feil(melding: String): Nothing =
        throw UgyldigBeregningRequestException(melding)
}
