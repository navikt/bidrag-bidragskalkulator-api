package no.nav.bidrag.bidragskalkulator.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.bidragskalkulator.listener.BidragKafkaEventListener
import no.nav.bidrag.bidragskalkulator.service.BidragKafkaEventService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean


@TestConfiguration
class KafkaTestConfig {

    @Bean()
    fun provideBidragKafkaEventService(): BidragKafkaEventService {
        return BidragKafkaEventService()
    }


    @Bean()
    fun provideBidragKafkaEventListener(bidragKafkaEventService: BidragKafkaEventService, objectMapper: ObjectMapper): BidragKafkaEventListener {
        return BidragKafkaEventListener(bidragKafkaEventService, objectMapper)
    }

    @Bean()
    fun provideObjectMapper(): ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
    }
}