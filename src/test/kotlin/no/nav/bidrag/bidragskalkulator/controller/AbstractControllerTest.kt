package no.nav.bidrag.bidragskalkulator.controller

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

/**
 * Baseklasse for kontrollertester med felles testoppsett og hjelpefunksjoner.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableMockOAuth2Server
@ActiveProfiles("test")
abstract class AbstractControllerTest {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    protected lateinit var gyldigOAuth2Token: String

    protected fun postRequest(url: String, body: Any, token: String? = null): ResultActions {
        return mockMvc.perform(buildJsonRequest(post(url), body, token))
    }

    protected fun getRequest(url: String, token: String? = null): ResultActions {
        return mockMvc.perform(buildJsonRequest(get(url), null, token))
    }

    private fun buildJsonRequest(
        builder: MockHttpServletRequestBuilder,
        body: Any? = null,
        token: String? = null
    ): MockHttpServletRequestBuilder {
        builder.contentType(MediaType.APPLICATION_JSON)
        token?.let { builder.header("Authorization", "Bearer $it") }
        body?.let { builder.content(objectMapper.writeValueAsString(it)) }
        return builder
    }
}