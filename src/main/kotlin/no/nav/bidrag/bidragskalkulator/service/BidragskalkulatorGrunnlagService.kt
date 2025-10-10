package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.coroutineScope
import no.nav.bidrag.bidragskalkulator.dto.BidragskalkulatorGrunnlagDto
import org.springframework.stereotype.Service


@Service
class BidragskalkulatorGrunnlagService(
    private val sjablonService: SjablonService,
    private val boOgForbruksutgiftService: BoOgForbruksutgiftService
) {
    suspend fun hentGrunnlagsData(): BidragskalkulatorGrunnlagDto = coroutineScope {
        BidragskalkulatorGrunnlagDto(
            boOgForbruksutgifter = boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell(),
            samværsfradrag = sjablonService.hentSamværsfradrag(),
        )
    }
}
