package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.dto.BidragskalkulatorGrunnlagDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

private const val ANTALL_FORSKUDDSSATSER_FOR_INNTEKTSPAVIRKNING = 30
private const val ANTALL_FORSKUDDSSATSER_FOR_SELVFORSORGET_BARN = 100

@Service
class BidragskalkulatorGrunnlagService(
    private val sjablonService: SjablonService,
    private val boOgForbruksutgiftService: BoOgForbruksutgiftService,
) {
    suspend fun hentGrunnlagsData(): BidragskalkulatorGrunnlagDto {
        val forskuddssats = sjablonService.hentForskuddssats()

        return BidragskalkulatorGrunnlagDto(
            boOgForbruksutgifter = boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell(),
            samværsfradrag = sjablonService.hentSamværsfradrag(),
            barnInntektsgrense = forskuddssats * BigDecimal(ANTALL_FORSKUDDSSATSER_FOR_INNTEKTSPAVIRKNING),
            selvforsørgetBarnInntektsgrense = forskuddssats * BigDecimal(ANTALL_FORSKUDDSSATSER_FOR_SELVFORSORGET_BARN),
        )
    }
}
