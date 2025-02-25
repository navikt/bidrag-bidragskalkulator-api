package no.nav.bidrag.bidragskalkulator.controller

import no.nav.bidrag.bidragskalkulator.controller.dto.BarnDto
import no.nav.bidrag.bidragskalkulator.controller.dto.EnkelBeregningRequestDto
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

@SpringBootTest
@AutoConfigureMockMvc
class BeregningControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should accept valid request`() {
        val request = EnkelBeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsgrad = 50)
            )
        )

        mockMvc.perform(
            post("/v1/beregning/enkel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultat").exists())
    }

    @Test
    fun `should return 400 for negative income`() {
        val request = EnkelBeregningRequestDto(
            inntektForelder1 = -500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsgrad = 50)
            )
        )

        mockMvc.perform(
            post("/v1/beregning/enkel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("inntektForelder1: Inntekt for forelder 1 kan ikke være negativ"))
    }

    @Test
    fun `should return 400 for empty barn list`() {
        val request = EnkelBeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = emptyList()
        )

        mockMvc.perform(
            post("/v1/beregning/enkel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("barn: Liste over barn kan ikke være tom"))
    }

    @Test
    fun `should return 400 for age above 25`() {
        val request = EnkelBeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 26, samværsgrad = 50)
            )
        )

        mockMvc.perform(
            post("/v1/beregning/enkel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("barn[0].alder: Alder kan ikke være høyere enn 25"))
    }

    @Test
    fun `should return 400 for samværsgrad above 100`() {
        val request = EnkelBeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsgrad = 101)
            )
        )

        mockMvc.perform(
            post("/v1/beregning/enkel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("barn[0].samværsgrad: Samværsgrad kan ikke være høyere enn 100"))
    }
}
