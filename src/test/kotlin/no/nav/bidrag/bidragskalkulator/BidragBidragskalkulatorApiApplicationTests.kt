package no.nav.bidrag.bidragskalkulator

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BidragBidragskalkulatorApiApplicationTests {

    @Test
    fun contextLoads() {
        val activeProfile = System.getProperty("spring.profiles.active")
        println("Active Profile: $activeProfile") // Should print "test"
    }

}
