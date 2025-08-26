package no.nav.bidrag.bidragskalkulator.listener

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.bidragskalkulator.service.BidragKafkaEventService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.sak.Sakshendelse
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Consumer for Kafka events from bidrag-listener
 */
@Component
class BidragKafkaEventListener(
    private val kafkaEventService: BidragKafkaEventService,
    private val objectMapper: ObjectMapper,
) {

    /**
     * Lytter på hendelser fra Kafka-topic for sakshendelser, hvis `kafka.enabled` er satt til true.
     */
    @KafkaListener(topics = ["\${KAFKA_TOPIC_SAK}"], groupId = "\${KAFKA_GROUP_ID_SAK}", autoStartup = "\${kafka.enabled}")
    fun consumeSakEvent(@Payload event: String) {
        secureLogger.info { "Received event from Kafka: $event" }

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
}