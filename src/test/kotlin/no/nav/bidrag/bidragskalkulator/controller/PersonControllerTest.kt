package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper
import no.nav.bidrag.bidragskalkulator.service.BarnUnderholdskostnad
import no.nav.bidrag.bidragskalkulator.service.BrukerinformasjonService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.bidragskalkulator.utils.TestDataUtils.lagBarnUnderholdskostnad
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PersonControllerTest: AbstractControllerTest() {

    @MockkBean
    private lateinit var brukerinformasjonService: BrukerinformasjonService

    private val mockResponsPersonMedEnBarnRelasjon: MotpartBarnRelasjonDto =
        JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")

    private val mockTransofmerInntekterResponse: TransformerInntekterResponse =
        JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")

    private val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(mockResponsPersonMedEnBarnRelasjon)

    @Test
    fun `skal returnere 200 OK og familierelasjon når person eksisterer`() {
        every { runBlocking { brukerinformasjonService.hentBrukerinformasjon(any()) } } returns
            BrukerInformasjonMapper
                .tilBrukerInformasjonDto(
                    mockResponsPersonMedEnBarnRelasjon,
                    underholdkostnad,
                    mockTransofmerInntekterResponse
                )

        getRequest("/api/v1/person/informasjon", gyldigOAuth2Token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.barnerelasjoner").isNotEmpty())
    }

    @Test
    fun `skal returnere 204 No Content når personen ikke finnes`() {
        mockkObject(TokenUtils)

        every { TokenUtils.hentBruker() } returns null
        every { runBlocking { brukerinformasjonService.hentBrukerinformasjon(any()) } } throws NoContentException("Fant ikke person")

        getRequest("/api/v1/person/informasjon", gyldigOAuth2Token)
            .andExpect(status().isNoContent)

        unmockkAll()
    }

    @Test
    fun `skal returnere 401 Unauthorized når token mangler`() {
        getRequest("/api/v1/person/informasjon", "")
            .andExpect(status().isUnauthorized)
    }
}