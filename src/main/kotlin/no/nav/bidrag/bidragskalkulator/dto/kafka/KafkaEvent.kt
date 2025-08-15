package no.nav.bidrag.bidragskalkulator.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Base class for Kafka events
 */
sealed class KafkaEvent {
    @JsonProperty("@initiated_by")
    lateinit var initiatedBy: String
}

/**
 * Event for enabling a microfrontend for a user
 */
data class EnableEvent(
    @JsonProperty("@action")
    val action: String = "enable",
    val ident: String,
    val microfrontend_id: String,
    val sensitivitet: String
) : KafkaEvent()

/**
 * Event for disabling a microfrontend for a user
 */
data class DisableEvent(
    @JsonProperty("@action")
    val action: String = "disable",
    val ident: String,
    val microfrontend_id: String
) : KafkaEvent()