import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.context.event.annotation.AfterTestMethod
import org.springframework.test.context.event.annotation.BeforeTestMethod

class KafkaTestListenerReadyConfig {

    @Bean
    fun kafkaListenerInitializer(
        kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry,
        embeddedKafka: EmbeddedKafkaBroker
    ) = object {

        @BeforeTestMethod
        fun beforeEachTest() {
            kafkaListenerEndpointRegistry.listenerContainers.forEach { container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)
            }
        }

        @AfterTestMethod
        fun afterEachTest() {
            // Optional cleanup if needed
        }
    }
}