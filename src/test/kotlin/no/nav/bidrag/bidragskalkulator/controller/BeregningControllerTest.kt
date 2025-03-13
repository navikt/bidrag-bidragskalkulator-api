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
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
internal class BeregningControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper
) {

    @MockkBean
    lateinit var beregningService: BeregningService

    @Test
    fun `skal ta imot en gyldig request`() {

        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 400000.0,
            barn = listOf(
                BarnDto(alder = 10, samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1)
            )
        )

        every { beregningService.beregnBarnebidrag(request) } returns BeregningsresultatDto(resultater = listOf(
            BeregningsresultatBarnDto(sum = BigDecimal(100), barnetsAlder = request.barn.first().alder, underholdskostnad = BigDecimal(8471))
        ));


        mockMvc.perform(
            post("/api/v1/beregning/barnebidrag")
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
