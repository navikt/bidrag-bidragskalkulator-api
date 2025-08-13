package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.bidrag.bidragskalkulator.dto.AndreBestemmelserSkjema
import no.nav.bidrag.bidragskalkulator.dto.Oppgjør
import no.nav.bidrag.bidragskalkulator.dto.Oppgjørsform
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarn
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBidragsmottaker
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBidragspliktig
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import no.nav.bidrag.bidragskalkulator.dto.Vedleggskrav
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.NavSkjemaId
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.Språkkode
import no.nav.bidrag.bidragskalkulator.mapper.tilPrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtalePdfService
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtaleService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.Test

class PrivatAvtaleControllerTest: AbstractControllerTest() {
    @MockkBean
    private lateinit var privatAvtaleService: PrivatAvtaleService
    @MockkBean
    private lateinit var privatAvtalePdfService: PrivatAvtalePdfService
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

    @Test
    fun `skal generere privat avtale PDF`() {
        val personIdent = "12345678910"
        val dto = lagGyldigPrivatAvtalePdfDto()

        every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns personIdent

        every { privatAvtalePdfService.genererPrivatAvtalePdf(personIdent, dto) } returns
                java.io.ByteArrayOutputStream().apply {
                    // Minimal PDF-like bytes: "%PDF-1.4\n%%EOF"
                    write(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A, 0x25, 0x25, 0x45, 0x4F, 0x46))
                }

        postRequest(
            "/api/v1/privat-avtale",
            dto,
            gyldigOAuth2Token
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", "inline; filename=\"privatavtale.pdf\""))
    }

    @Test
    fun `skal gi feil hvis harAndreBestemmelser er true men beskrivelse mangler`() {
        val dto = lagGyldigPrivatAvtalePdfDto().copy(
            andreBestemmelser = AndreBestemmelserSkjema(harAndreBestemmelser = true, beskrivelse = null)
        )
        val personIdent = "12345678910"

        every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns personIdent

        verify(exactly = 0) { privatAvtalePdfService.genererPrivatAvtalePdf(personIdent, any()) }

        postRequest("/api/v1/privat-avtale", dto, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Feltet 'andreBestemmelserTekst' er påkrevd når 'harAndreBestemmelser' er true."))
    }

    @Test
    fun `skal gi feil hvis nyAvtale er false og oppgjorsformIdag er null`() {
        val dto = lagGyldigPrivatAvtalePdfDto().copy(
            oppgjør = Oppgjør(nyAvtale = false, oppgjørsformØnsket = Oppgjørsform.INNKREVING, oppgjørsformIdag = null)
        )
        val personIdent = "12345678910"

        every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns personIdent

        verify(exactly = 0) { privatAvtalePdfService.genererPrivatAvtalePdf(personIdent, any()) }

        postRequest("/api/v1/privat-avtale", dto, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Feltet 'oppgjørsformIdag' er påkrevd når det er eksisterende avtale."))
    }


    private fun lagGyldigPrivatAvtalePdfDto(): PrivatAvtalePdfDto {
        return PrivatAvtalePdfDto(
            navSkjemaId = NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18,
            språk = Språkkode.NB,
            bidragsmottaker = PrivatAvtaleBidragsmottaker("Ola", "Nordmann", "12345678901"),
            bidragspliktig = PrivatAvtaleBidragspliktig("Kari", "Nordmann", "10987654321"),
            barn = listOf(PrivatAvtaleBarn("Barn", "Nordmann", "01010112345", 1000.0)),
            oppgjør = Oppgjør(nyAvtale = true, oppgjørsformØnsket = Oppgjørsform.INNKREVING),
            fraDato = "2025-01-01",
            vedlegg = Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON,
            andreBestemmelser = AndreBestemmelserSkjema(false, null)
        )
    }
}
