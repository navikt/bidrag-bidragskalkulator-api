package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.tilPersonInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.toInntektResultatDto
import no.nav.bidrag.bidragskalkulator.service.BrukerinformasjonService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PersonControllerTest: AbstractControllerTest() {

    @MockkBean
    private lateinit var brukerinformasjonService: BrukerinformasjonService

    private val mockResponsPersonMedEnBarnRelasjon: MotpartBarnRelasjonDto =
        JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")

    private val mockTransofmerInntekterResponse: TransformerInntekterResponse =
        JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")

    @Test
    fun `skal returnere 200 OK når person eksisterer`() {
        every { runBlocking { brukerinformasjonService.hentBrukerinformasjon(any()) } } returns
                BrukerInformasjonDto(
                    person = mockResponsPersonMedEnBarnRelasjon.person.tilPersonInformasjonDto(),
                    inntekt = mockTransofmerInntekterResponse.toInntektResultatDto().inntektSiste12Mnd,
                    barnerelasjoner = emptyList(),
                    underholdskostnader = emptyMap(),
                    samværsfradrag = emptyList()
                )

        getRequest("/api/v1/person/informasjon", gyldigOAuth2Token)
            .andExpect(status().isOk)
    }

    @Test
    fun `skal returnere 401 Unauthorized når token mangler`() {
        getRequest("/api/v1/person/informasjon")
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `skal returnere 401 Unauthorized når ugyldig token er gitt`() {
        getRequest("/api/v1/person/informasjon", ugyldigOAuth2Token)
            .andExpect(status().isUnauthorized)
    }
}
