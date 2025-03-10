package no.nav.bidrag.bidragskalkulator.controller

import no.nav.bidrag.bidragskalkulator.dto.BarnDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.bidragskalkulator.BidragBidragskalkulatorApiApplicationTests
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [BidragBidragskalkulatorApiApplicationTests::class])
@SpringBootTest(
    classes = [BidragBidragskalkulatorApiApplicationTests::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BeregningControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal ta imot en gyldig request`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1)
            )
        )

        mockMvc.perform(
            post("/api/v1/beregning/barnebidrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.beregningsResultater").isNotEmpty())
    }

    @Test
    fun `skal returnere 400 for negativ inntekt`() {
        val request = BeregningRequestDto(
            inntektForelder1 = -500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1)
            )
        )

        mockMvc.perform(
            post("/api/v1/beregning/barnebidrag")
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
            post("/api/v1/beregning/barnebidrag")
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
                BarnDto(alder = 26, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1)
            )
        )

        mockMvc.perform(
            post("/api/v1/beregning/barnebidrag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("barn[0].alder: Alder kan ikke være høyere enn 25"))
    }

}
