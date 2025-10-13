package no.nav.bidrag.bidragskalkulator.config

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.bidragskalkulator.utils.FastDatoPerÅrUtløp
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Month
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@Profile(value = ["!test"])
class CacheConfig {

    companion object Cachenøkler {
        const val BOOGFORBRUKSUTGIFT = "boOgForbruksutgift"
        const val PERSONINFORMASJON = "personinformasjon"
        const val PRIVAT_AVTALE_INFORMASJON = "privatAvtaleInformasjon"
        const val SAMVÆRSFRADRAG = "samværsfradrag"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val årligUtløp = FastDatoPerÅrUtløp(Month.JULY, 1)

        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            BOOGFORBRUKSUTGIFT,
            Caffeine.newBuilder().expireAfter(årligUtløp).build()
        )
        caffeineCacheManager.registerCustomCache(PERSONINFORMASJON, Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build())
        caffeineCacheManager.registerCustomCache(PRIVAT_AVTALE_INFORMASJON, Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build())
        caffeineCacheManager.registerCustomCache(
            SAMVÆRSFRADRAG,
            Caffeine.newBuilder().expireAfter(årligUtløp).build()
        )
        return caffeineCacheManager
    }
}
