package no.nav.bidrag.bidragskalkulator.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.bidragskalkulator.dto.kafka.DisableEvent
import no.nav.bidrag.bidragskalkulator.dto.kafka.EnableEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class KafkaEventServiceTest {

    private lateinit var objectMapper: ObjectMapper

    @MockK
    private lateinit var kafkaProducerService: KafkaProducerService

    private lateinit var kafkaEventService: KafkaEventService

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        kafkaEventService = KafkaEventService(objectMapper, kafkaProducerService)
    }

    @Test
    fun `processEvent should handle enable event correctly`() {
        // Given
        val enableEventJson = """
            {
                "@action": "enable",
                "ident": "12345678901",
                "microfrontend_id": "test-microfrontend",
                "sensitivitet": "substantial",
                "@initiated_by": "bidrag-team"
            }
        """.trimIndent()

        val enableEventSlot = slot<EnableEvent>()
        every { kafkaProducerService.sendEnableEvent(capture(enableEventSlot)) } returns Unit

        // When
        kafkaEventService.processEvent(enableEventJson)

        // Then
        verify(exactly = 1) { kafkaProducerService.sendEnableEvent(any()) }
        assertEquals("12345678901", enableEventSlot.captured.ident)
        assertEquals("test-microfrontend", enableEventSlot.captured.microfrontend_id)
        assertEquals("substantial", enableEventSlot.captured.sensitivitet)
        assertEquals("enable", enableEventSlot.captured.action)
    }

    @Test
    fun `processEvent should handle disable event correctly`() {
        // Given
        val disableEventJson = """
            {
                "@action": "disable",
                "ident": "12345678901",
                "microfrontend_id": "test-microfrontend",
                "@initiated_by": "bidrag-team"
            }
        """.trimIndent()

        val disableEventSlot = slot<DisableEvent>()
        every { kafkaProducerService.sendDisableEvent(capture(disableEventSlot)) } returns Unit

        // When
        kafkaEventService.processEvent(disableEventJson)

        // Then
        verify(exactly = 1) { kafkaProducerService.sendDisableEvent(any()) }
        assertEquals("12345678901", disableEventSlot.captured.ident)
        assertEquals("test-microfrontend", disableEventSlot.captured.microfrontend_id)
        assertEquals("disable", disableEventSlot.captured.action)
    }

    @Test
    fun `processEvent should handle unknown action gracefully`() {
        // Given
        val unknownEventJson = """
            {
                "@action": "unknown",
                "ident": "12345678901",
                "microfrontend_id": "test-microfrontend",
                "@initiated_by": "bidrag-team"
            }
        """.trimIndent()

        // When
        kafkaEventService.processEvent(unknownEventJson)

        // Then
        verify(exactly = 0) { kafkaProducerService.sendEnableEvent(any()) }
        verify(exactly = 0) { kafkaProducerService.sendDisableEvent(any()) }
    }
}
