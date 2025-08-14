package no.nav.bidrag.bidragskalkulator.consumer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.service.KafkaEventService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.sak.Sakshendelse
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component


/**
 * Consumer for Kafka events from bidrag-listener
 */
@Component
class KafkaEventConsumer(
    private val kafkaEventService: KafkaEventService,
    private val objectMapper: ObjectMapper,
) {

    private val logger = KotlinLogging.logger { KafkaEventConsumer::class.java.simpleName }

    /**
     * Listens for events on the configured topic
     */
    @KafkaListener(topics = ["\${KAFKA_TOPIC_SAK}"], groupId = "\${KAFKA_GROUP_ID_SAK}")
    fun consumeEvent(@Payload event: String) {
        logger.info { "Received event from Kafka: $event" }

        try {
            val saksHendelse = objectMapper.readValue(event, Sakshendelse::class.java)
            kafkaEventService.prosesserSakshendelse(saksHendelse)
        } catch (e: Exception) {
            when (e) {
                is JsonProcessingException -> {
                    secureLogger.error(e) { "JSON parsing error for event: $event" }
                }
                else -> {
                    secureLogger.error { "Error processing event: $event" }
                    secureLogger.error(e) { "Unexpected error for event: $event" }
                }
            }
        }
    }

    fun startConsumer() {
        logger.info { "Starting Kafka consumer for topic: \${KAFKA_TOPIC_SAK}" }
        // Logic to start the consumer if needed

    }
}