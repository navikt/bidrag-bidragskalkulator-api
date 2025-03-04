package no.nav.bidrag.bidragskalkulator

import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
class BidragBidragskalkulatorApiLocal

fun main(args: Array<String>) {
    val app = SpringApplication(BidragBidragskalkulatorApiLocal::class.java)
    app.setAdditionalProfiles("local", "localnais")
    app.run(*args)
}