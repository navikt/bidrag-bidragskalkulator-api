package no.nav.bidrag.bidragskalkulator.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `isalive endpoint should return 200 OK`() {
        mockMvc.perform(get("/api/internal/isalive"))
            .andExpect(status().isOk)
    }

    @Test
    fun `isready endpoint should return 200 OK`() {
        mockMvc.perform(get("/api/internal/isready"))
            .andExpect(status().isOk)
    }
}