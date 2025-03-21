package no.nav.bidrag.bidragskalkulator.controller


import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.bidragskalkulator.BidragBidragskalkulatorApiApplication
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.http.objectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.HashMap

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [BidragBidragskalkulatorApiApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BeregningControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var beregningService: BeregningService

    companion object {
        private val oAuth2Server = MockOAuth2Server()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            oAuth2Server.start(65181)
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            oAuth2Server.shutdown()
        }
    }

    private fun genereToken(): String {
        val personident = "12345678910"
        val claims = HashMap<String, Any>()
        claims["idp"] = personident
        val issuerId = "tokenx"
        return oAuth2Server.issueToken(issuerId, personident, "aud-localhost", claims).serialize()
    }

    @Test
    fun `skal returnere 200 OK for gyldig request`() {
        val token = genereToken()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/beregning/barnebidrag")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
    }

    @Test
    fun `skal ta imot en gyldig request`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, bidragstype = BidragsType.PLIKTIG)
            )
        )

        every { beregningService.beregnBarnebidrag(request) } returns BeregningsresultatDto(resultater = listOf(
            BeregningsresultatBarnDto(sum = BigDecimal(100), barnetsAlder = request.barn.first().alder, underholdskostnad = BigDecimal(8471))
        ));

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/beregning/barnebidrag")
                .header("Authorization", genereToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultater").isNotEmpty())
    }



    @Test
    fun `skal returnere 400 for negativ inntekt`() {
        val request = BeregningRequestDto(
            inntektForelder1 = -500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, bidragstype = BidragsType.PLIKTIG)
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/beregning/barnebidrag")
                .header(HttpHeaders.AUTHORIZATION, genereToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("inntektForelder1: Inntekt for forelder 1 kan ikke være negativ"))
    }

    @Test
    fun `skal returnere 400 for et tom barn liste`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = emptyList()
        )


        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/beregning/barnebidrag")
                .header(HttpHeaders.AUTHORIZATION, genereToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("barn: Liste over barn kan ikke være tom"))
    }

    @Test
    fun `skal returnere 400 for alder over 25`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 26, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, bidragstype = BidragsType.PLIKTIG)
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/beregning/barnebidrag")
                .header(HttpHeaders.AUTHORIZATION, genereToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("barn[0].alder: Alder kan ikke være høyere enn 25"))
    }

    @Test
    fun `skal håndtere inntekt mapping korrekt for bidragspliktig`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, bidragstype = BidragsType.PLIKTIG)
            )
        )

        every { beregningService.beregnBarnebidrag(request) } returns BeregningsresultatDto(resultater = listOf(
            BeregningsresultatBarnDto(sum = BigDecimal(100), barnetsAlder = request.barn.first().alder, underholdskostnad = BigDecimal(8471))
        ))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/beregning/barnebidrag")
                .header(HttpHeaders.AUTHORIZATION, genereToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultater").isNotEmpty())
    }

    @Test
    fun `skal håndtere inntekt mapping korrekt for bidragsmottaker`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, bidragstype = BidragsType.MOTTAKER)
            )
        )

        every { beregningService.beregnBarnebidrag(request) } returns BeregningsresultatDto(resultater = listOf(
            BeregningsresultatBarnDto(sum = BigDecimal(100), barnetsAlder = request.barn.first().alder, underholdskostnad = BigDecimal(8471))
        ))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/beregning/barnebidrag")
                .header(HttpHeaders.AUTHORIZATION, genereToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultater").isNotEmpty())
    }
}