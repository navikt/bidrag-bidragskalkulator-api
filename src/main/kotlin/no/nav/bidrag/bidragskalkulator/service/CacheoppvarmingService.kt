package no.nav.bidrag.bidragskalkulator.service

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class CacheoppvarmingService(
    private val underholdskostnadService: UnderholdskostnadService
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?): Unit {
        underholdskostnadService.genererUnderholdskostnadstabell()
    }
}