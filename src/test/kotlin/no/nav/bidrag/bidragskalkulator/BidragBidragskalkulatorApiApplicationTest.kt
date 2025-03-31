package no.nav.bidrag.bidragskalkulator

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
@ActiveProfiles("test")
class BidragBidragskalkulatorApiApplicationTest {
    @Test
    fun contextLoads() {
    }
}