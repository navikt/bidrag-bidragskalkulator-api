package no.nav.bidrag.bidragskalkulator.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.KafkaConfigurationProperties
import no.nav.bidrag.bidragskalkulator.service.KafkaEventService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Consumer for Kafka events from bidrag-listener
 */
@Component
class KafkaEventConsumer(
    private val kafkaEventService: KafkaEventService,
) {

    /**
     * Listens for events on the configured topic
     */
    @KafkaListener(topics = ["\${kafka.listenerTopic}"], groupId = "\${kafka.consumerGroupId}")
    fun consumeEvent(@Payload event: String) {
        logger.info { "Received event from Kafka: $event" }
        try {
            kafkaEventService.processEvent(event)
        } catch (e: Exception) {
            logger.error(e) { "Error processing Kafka event: $event" }
        }
    }
}