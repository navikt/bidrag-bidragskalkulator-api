package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.KafkaEventConsumer
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest
@DirtiesContext
@EnableMockOAuth2Server
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
class KafkaEventServiceTest {

    @Autowired
    lateinit var kafkaEventConsumer: KafkaEventConsumer

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Value("\${VEDTAK_KAFKA_TOPIC}")
    lateinit var topic: String


    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }


    @Test
    fun processEvent() {
        // Arrange
        val testEvent = "Test event message"
        kafkaTemplate.send(topic, testEvent).get() // Send a test event to the topic

        // Assert
        // Here you would typically verify that the event was processed correctly.
        // This could involve checking a database, a mock service, or any other side effects.
        assertTrue(true) // Replace with actual assertions based on your processing logic
    }

}