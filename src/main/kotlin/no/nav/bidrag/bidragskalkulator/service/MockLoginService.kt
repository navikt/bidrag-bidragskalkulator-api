package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.dto.MockLoginResponseDto
import no.nav.bidrag.domene.ident.Ident
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Profile("!prod")
@Service
class MockLoginService(
    @Qualifier("basic") private val restTemplate: RestTemplate
) {
    private val logger = LoggerFactory.getLogger(MockLoginService::class.java)
    fun genererMockTokenXToken(ident: Ident): MockLoginResponseDto {
        logger.info("Generating mock TokenX token for ident: {}", ident)
        val url = "https://tokenx-token-generator.intern.dev.nav.no/api/public/obo"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val requestBody = LinkedMultiValueMap<String, String>()
        requestBody.add("aud", "dev-gcp:bidrag:bidrag-bidragskalkulator-api")
        // Extract the actual identifier value from the Ident object
        val identValue = ident.toString().replace(Regex("[^0-9]"), "")
        requestBody.add("pid", identValue)

        val requestEntity = HttpEntity(requestBody, headers)

        try {
            val start = System.currentTimeMillis()
            val response = restTemplate.postForObject(url, requestEntity, String::class.java)
            val duration = System.currentTimeMillis() - start

            logger.info("Successfully generated mock TokenX token in {} ms", duration)
            return MockLoginResponseDto(token = response ?: "")
        } catch (e: Exception) {
            logger.error("Failed to generate mock TokenX token: {}", e.message, e)
            throw e
        }
    }
}
