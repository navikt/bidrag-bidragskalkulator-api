package no.nav.bidrag.bidragskalkulator.utils

import no.nav.bidrag.bidragskalkulator.dto.BarnetilsynDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.dto.BoforholdDto
import no.nav.bidrag.bidragskalkulator.dto.ForelderInntektDto
import no.nav.bidrag.bidragskalkulator.dto.KontantstøtteDto
import no.nav.bidrag.bidragskalkulator.dto.UtvidetBarnetrygdDto
import no.nav.bidrag.bidragskalkulator.dto.VoksneOver18Type
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.BarnMedAlderDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import java.math.BigDecimal

fun lagBarnDto(alder: Int = 1,
               samværklasse: Samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2,
               barnetilsyn: BarnetilsynDto? = null,
               kontantstøtte: KontantstøtteDto? = null,
               inntekt: BigDecimal? = null
) = BarnMedAlderDto(
    alder = alder,
    samværsklasse = samværklasse,
    barnetilsyn = barnetilsyn,
    kontantstøtte = kontantstøtte,
    inntekt = inntekt
)

fun lagBoforhold(antallBarnUnder18BorFast: Int = 0,
                         voksneOver18Type: Set<VoksneOver18Type>? = null,
                         antallBarnOver18Vgs: Int? = null) = BoforholdDto(
    antallBarnUnder18BorFast = antallBarnUnder18BorFast,
    voksneOver18Type = voksneOver18Type,
    antallBarnOver18Vgs = antallBarnOver18Vgs
)

fun lagBereningRequestDto(bmInntekt: ForelderInntektDto,
                                  bpInntekt: ForelderInntektDto,
                                  bidragstype: BidragsType,
                                  barn: List<BarnMedAlderDto> = emptyList(),
                                  dittBoforhold: BoforholdDto? = null,
                                  medforelderBoforhold: BoforholdDto? = null,
                                  utvidetBarnetrygd: UtvidetBarnetrygdDto? = null,
                                  småbarnstillegg: Boolean = false
): ÅpenBeregningRequestDto {
    return ÅpenBeregningRequestDto(
        bidragsmottakerInntekt = bmInntekt,
        bidragspliktigInntekt = bpInntekt,
        bidragstype = bidragstype,
        barn = barn,
        dittBoforhold = dittBoforhold,
        medforelderBoforhold = medforelderBoforhold,
        utvidetBarnetrygd = utvidetBarnetrygd,
        småbarnstillegg = småbarnstillegg
    )
}
