package no.nav.bidrag.bidragskalkulator.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(BeregningController::class)
class BeregningControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `skal returnere resultat for enkel beregning`() {
        mockMvc.perform(get("/v1/beregning/enkel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultat").value(100.0))
    }
}