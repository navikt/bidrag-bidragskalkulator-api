package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.KafkaConfigurationProperties
import no.nav.bidrag.bidragskalkulator.dto.kafka.DisableEvent
import no.nav.bidrag.bidragskalkulator.dto.kafka.EnableEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service for sending Kafka events
 */
@Service
class KafkaProducerService(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val kafkaConfig: KafkaConfigurationProperties
) {

    /**
     * Send an enable event to Kafka
     */
    fun sendEnableEvent(event: EnableEvent) {
        try {
            logger.info { "Sending enable event to Kafka: $event" }
            kafkaTemplate.send(kafkaConfig.producerTopic, event)
                .whenComplete { result, ex ->
                    if (ex == null) {
                        logger.info { "Enable event sent successfully: ${result.recordMetadata.offset()}" }
                    } else {
                        logger.error(ex) { "Error sending enable event to Kafka: $event" }
                    }
                }
        } catch (e: Exception) {
            logger.error(e) { "Error sending enable event to Kafka: $event" }
        }
    }

    /**
     * Send a disable event to Kafka
     */
    fun sendDisableEvent(event: DisableEvent) {
        try {
            logger.info { "Sending disable event to Kafka: $event" }
            kafkaTemplate.send(kafkaConfig.producerTopic, event)
                .whenComplete { result, ex ->
                    if (ex == null) {
                        logger.info { "Disable event sent successfully: ${result.recordMetadata.offset()}" }
                    } else {
                        logger.error(ex) { "Error sending disable event to Kafka: $event" }
                    }
                }
        } catch (e: Exception) {
            logger.error(e) { "Error sending disable event to Kafka: $event" }
        }
    }
}