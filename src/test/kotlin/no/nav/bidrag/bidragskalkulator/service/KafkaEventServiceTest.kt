package no.nav.bidrag.bidragskalkulator.service

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import no.nav.bidrag.bidragskalkulator.listener.BidragKafkaEventListener
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@DirtiesContext
@EnableMockOAuth2Server
@TestPropertySource(properties = ["kafka.enabled=true"])
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
@Import(KafkaTestListenerReadyConfig::class)
class KafkaEventServiceTest {


    @SpykBean
    lateinit var kafkaEventConsumer: BidragKafkaEventListener

    @SpykBean
    lateinit var bidragKafkaEventService: BidragKafkaEventService

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Value("\${KAFKA_TOPIC_SAK}")
    lateinit var topic: String


    @AfterEach
    fun tearDown() {
    }


    @Test
    fun `skal prosessere et event i sak-topic`() {

        val testEvent =
            """
                {"saksnummer":"2500058","hendelsestype":"ENDRING","roller":[{"ident":"16490550511","type":"BA","samhandlerId":null,"reelMottager":null,"ukjent":false},{"ident":"09841499538","type":"BA","samhandlerId":null,"reelMottager":null,"ukjent":false},{"ident":"25818398312","type":"BM","samhandlerId":null,"reelMottager":null,"ukjent":false},{"ident":"04838298643","type":"BP","samhandlerId":null,"reelMottager":null,"ukjent":false},{"ident":"14881799112","type":"BA","samhandlerId":null,"reelMottager":null,"ukjent":false}],"sporingId":"1753146242399_bisys_sakhendelse"}
            """.trimIndent()

        kafkaTemplate.send(topic, testEvent)
        verify(timeout = 5000, exactly = 1) { kafkaEventConsumer.consumeSakEvent(testEvent) }
        verify(timeout = 5000, exactly = 1) { bidragKafkaEventService.prosesserSakshendelse(any()) }
    }

}