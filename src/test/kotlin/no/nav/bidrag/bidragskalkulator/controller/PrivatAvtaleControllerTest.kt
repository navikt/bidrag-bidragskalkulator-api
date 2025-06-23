package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.bidragskalkulator.mapper.tilPrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtaleService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.Test

class PrivatAvtaleControllerTest: AbstractControllerTest() {
    @MockkBean
    private lateinit var privatAvtaleService: PrivatAvtaleService
    @MockkBean
    private lateinit var innloggetBrukerUtils: InnloggetBrukerUtils

    private val mockResponsPersonMedEnBarnRelasjon: MotpartBarnRelasjonDto =
        JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")

    @Test
    fun `skal returnere informasjon for privat avtale`() {
        val personIdent = "03848797048"
        val forventetDto = mockResponsPersonMedEnBarnRelasjon.person.tilPrivatAvtaleInformasjonDto()

        every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns personIdent
        every { privatAvtaleService.hentInformasjonForPrivatAvtale(personIdent) } returns forventetDto

        getRequest("/api/v1/privat-avtale/informasjon", gyldigOAuth2Token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fornavn").value(forventetDto.fornavn))
            .andExpect(jsonPath("$.etternavn").value(forventetDto.etternavn))
            .andExpect(jsonPath("$.ident").value(forventetDto.ident.verdi))
    }

    @Test
    fun `skal gi feil hvis personIdent ikke kan hentes`() {

        every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns null

        getRequest("/api/v1/privat-avtale/informasjon", gyldigOAuth2Token)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Ugyldig token"))
    }
}