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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableMockOAuth2Server
@ActiveProfiles("test")
abstract class ControllerTestRunner {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    lateinit var gyldigOAuth2Token: String // Injected from configuration

    protected fun MockMvc.postJson(url: String, content: Any, token: String? = null) =
        perform(post(url)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(content))
        )

    protected fun MockMvc.getRequest(url: String, token: String) =
        perform(get(url)
            .apply { header("Authorization", "Bearer $token") }
            .contentType(MediaType.APPLICATION_JSON)
        )
}