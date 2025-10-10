package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.dto.BidragskalkulatorGrunnlagDto
import no.nav.bidrag.bidragskalkulator.dto.SamværsfradragPeriode
import no.nav.bidrag.bidragskalkulator.service.BidragskalkulatorGrunnlagService
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class BidragskalkulatorGrunnlagControllerTest : AbstractControllerTest() {

    @MockkBean
    private lateinit var brukerinformasjonService: BidragskalkulatorGrunnlagService

    @Test
    fun `skal returnere 200 OK og tom payload naar service returnerer tomme data`() {
        every { runBlocking { brukerinformasjonService.hentGrunnlagsData() } } returns
                BidragskalkulatorGrunnlagDto(
                    boOgForbruksutgifter = emptyMap(),
                    samværsfradrag = emptyList()
                )

        getRequest("/api/v1/bidragskalkulator/grunnlagsdata")
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""{"boOgForbruksutgifter":{},"samværsfradrag":[]}""", true))

        verify(exactly = 1) { runBlocking { brukerinformasjonService.hentGrunnlagsData() } }
    }

    @Test
    fun `skal returnere 200 OK og seriell payload fra service`() {
        val expectedJson = """
            {
              "boOgForbruksutgifter": {
                "6": 6547,
                "11": 7240
              },
              "samværsfradrag": [
                    {
                        "alderFra": 0,
                        "alderTil": 5,
                        "beløpFradrag": {
                            "SAMVÆRSKLASSE_1": 332,
                            "SAMVÆRSKLASSE_2": 1099,
                            "SAMVÆRSKLASSE_3": 2950,
                            "SAMVÆRSKLASSE_4": 3703
                        }
                    },
                    {
                        "alderFra": 6,
                        "alderTil": 10,
                        "beløpFradrag": {
                            "SAMVÆRSKLASSE_1": 469,
                            "SAMVÆRSKLASSE_2": 1553,
                            "SAMVÆRSKLASSE_3": 3582,
                            "SAMVÆRSKLASSE_4": 4497
                        }
                    }
            ]
          }
        """.trimIndent()

        val dto = BidragskalkulatorGrunnlagDto(
            boOgForbruksutgifter = linkedMapOf(
                6 to BigDecimal(6547),
                11 to BigDecimal(7240)
            ),
            samværsfradrag = listOf(
                SamværsfradragPeriode(alderFra = 0, alderTil = 5, beløpFradrag = linkedMapOf(
                    "SAMVÆRSKLASSE_1" to BigDecimal(332),
                    "SAMVÆRSKLASSE_2" to BigDecimal(1099),
                    "SAMVÆRSKLASSE_3" to BigDecimal(2950),
                    "SAMVÆRSKLASSE_4" to BigDecimal(3703)
                )),
                SamværsfradragPeriode(alderFra = 6, alderTil = 10, beløpFradrag = linkedMapOf(
                    "SAMVÆRSKLASSE_1" to BigDecimal(469),
                    "SAMVÆRSKLASSE_2" to BigDecimal(1553),
                    "SAMVÆRSKLASSE_3" to BigDecimal(3582),
                    "SAMVÆRSKLASSE_4" to BigDecimal(4497)
                )),
            )
        )

        every { runBlocking { brukerinformasjonService.hentGrunnlagsData() } } returns dto

        getRequest("/api/v1/bidragskalkulator/grunnlagsdata")
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json(expectedJson, true))

        verify(exactly = 1) { runBlocking { brukerinformasjonService.hentGrunnlagsData() } }
    }

    @Test
    fun `skal returnere 500 Internal Server Error når service kaster`() {
        every { runBlocking { brukerinformasjonService.hentGrunnlagsData() } } throws
                RuntimeException("Uventet feil")

        getRequest("/api/v1/bidragskalkulator/grunnlagsdata")
            .andExpect(status().is5xxServerError)

        verify(exactly = 1) { runBlocking { brukerinformasjonService.hentGrunnlagsData() } }
    }
}
