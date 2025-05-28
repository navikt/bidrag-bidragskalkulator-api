package no.nav.bidrag.bidragskalkulator

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
@EnableJwtTokenValidation(ignore = ["org.springdoc", "org.springframework"])
@EnableCaching
class BidragBidragskalkulatorApiApplication

fun main(args: Array<String>) {
    runApplication<BidragBidragskalkulatorApiApplication>(*args)
}
