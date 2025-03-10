package no.nav.bidrag.bidragskalkulator

import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
class BidragBidragskalkulatorApiApplicationTests


fun main(args: Array<String>) {
    val app = SpringApplication(BidragBidragskalkulatorApiApplication::class.java)
    app.setAdditionalProfiles("test")
    app.run(*args)
}
