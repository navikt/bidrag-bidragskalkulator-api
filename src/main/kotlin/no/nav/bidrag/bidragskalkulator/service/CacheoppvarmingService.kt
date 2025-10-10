package no.nav.bidrag.bidragskalkulator.service

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class CacheoppvarmingService(
    private val boOgForbruksutgiftService: BoOgForbruksutgiftService,
    private val sjablonService: SjablonService
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?): Unit {
        boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell()
        sjablonService.hentSamværsfradrag()
    }
}
