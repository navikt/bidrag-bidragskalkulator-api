package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableMockOAuth2Server
@ActiveProfiles("test")
class BeregningControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var validOAuth2Token: String // Injected from configuration

    @MockkBean
    private lateinit var beregningService: BeregningService

    @BeforeEach
    fun setupMocks() {
        every { beregningService.beregnBarnebidrag(mockGyldigRequest) } returns mockRespons

    }

    @Test
    fun `skal returnere 200 OK uten token for et åpent endepunkt`() {
        mockMvc.postJson("/api/v1/beregning/barnebidrag", mockGyldigRequest)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultater").isNotEmpty())
    }

    @Test
    fun `skal bekrefte at mock OAuth2-server kjører`() {
        val issuerUrl = mockOAuth2Server.issuerUrl("tokenx").toString()
        println("Issuer URL: $issuerUrl")
        assert(issuerUrl.contains("localhost"))
    }

    @Test
    fun `skal returnere 200 OK med gyldig OAuth2-token`() {
        mockMvc.postJson("/api/v1/beregning/beskyttet/barnebidrag", mockGyldigRequest, validOAuth2Token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultater").isNotEmpty())
    }

    @Test
    fun `skal returnere 401 Unauthorized når ingen token er gitt`() {
        mockMvc.postJson("/api/v1/beregning/beskyttet/barnebidrag", mockGyldigRequest)
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `skal returnere 400 for negativ inntekt`() {
        val request = mockGyldigRequest.copy(inntektForelder1 = -500000.0)

        mockMvc.postJson("/api/v1/beregning/barnebidrag", request, validOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("inntektForelder1: Inntekt for forelder 1 kan ikke være negativ"))
    }

    @Test
    fun `skal returnere 400 for et tom barn liste`() {
        val request = mockGyldigRequest.copy(barn = emptyList())

        mockMvc.postJson("/api/v1/beregning/barnebidrag", request, validOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("barn: Liste over barn kan ikke være tom"))
    }

    companion object TestData {
        val mockGyldigRequest = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(BarnDto(alder = 10, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, bidragstype = BidragsType.PLIKTIG))
        )

        val mockRespons = BeregningsresultatDto(
            resultater = listOf(
                BeregningsresultatBarnDto(sum = BigDecimal(100), barnetsAlder = mockGyldigRequest.barn.first().alder, underholdskostnad = BigDecimal(8471), bidragstype = mockGyldigRequest.barn.first().bidragstype)
            )
        )
    }

    private fun MockMvc.postJson(url: String, content: Any, token: String? = null) =
        perform(post(url)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(content))
        )
}