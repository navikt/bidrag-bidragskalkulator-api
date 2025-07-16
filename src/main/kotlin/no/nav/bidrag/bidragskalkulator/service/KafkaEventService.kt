package no.nav.bidrag.bidragskalkulator.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.dto.kafka.DisableEvent
import no.nav.bidrag.bidragskalkulator.dto.kafka.EnableEvent
import no.nav.bidrag.bidragskalkulator.dto.kafka.KafkaEvent
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service for processing Kafka events
 */
@Service
class KafkaEventService(
    private val objectMapper: ObjectMapper,
    private val kafkaProducerService: KafkaProducerService
) {

    /**
     * Process an event from Kafka
     */
    fun processEvent(eventJson: String) {
        val jsonNode = objectMapper.readTree(eventJson)
        val action = jsonNode.path("@action").asText()

        when (action) {
            "enable" -> processEnableEvent(jsonNode)
            "disable" -> processDisableEvent(jsonNode)
            else -> logger.warn { "Unknown action in Kafka event: $action" }
        }
    }

    private fun processEnableEvent(jsonNode: JsonNode) {
        try {
            val event = objectMapper.treeToValue(jsonNode, EnableEvent::class.java)
            logger.info { "Processing enable event for ident: ${event.ident}, microfrontend: ${event.microfrontend_id}" }
            
            // Implement business logic for enable event here
            // For example, you might want to store the event in a database or trigger some other action
            
            // Forward the event to another topic if needed
            kafkaProducerService.sendEnableEvent(event)
        } catch (e: Exception) {
            logger.error(e) { "Error processing enable event: $jsonNode" }
        }
    }

    private fun processDisableEvent(jsonNode: JsonNode) {
        try {
            val event = objectMapper.treeToValue(jsonNode, DisableEvent::class.java)
            logger.info { "Processing disable event for ident: ${event.ident}, microfrontend: ${event.microfrontend_id}" }
            
            // Implement business logic for disable event here
            // For example, you might want to store the event in a database or trigger some other action
            
            // Forward the event to another topic if needed
            kafkaProducerService.sendDisableEvent(event)
        } catch (e: Exception) {
            logger.error(e) { "Error processing disable event: $jsonNode" }
        }
    }
}