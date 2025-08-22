package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.bidrag.bidragskalkulator.dto.AndreBestemmelserSkjema
import no.nav.bidrag.bidragskalkulator.dto.Oppgjør
import no.nav.bidrag.bidragskalkulator.dto.Oppgjørsform
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarn
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePart
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnUnder18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.Vedleggskrav
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.Språkkode
import no.nav.bidrag.bidragskalkulator.mapper.tilPrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtalePdfService
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtaleService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
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

        getRequest("/api/v1/privat-avtale/informasjon", ugyldigOAuth2Token)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.detail").value("Ugyldig eller manglende token"))
    }

    @Test
    fun `skal generere privat avtale PDF`() {
        val personIdent = "12345678910"
        val dto = lagGyldigPrivatAvtaleBarnUnder18RequestDto()

        every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns personIdent

        every { privatAvtalePdfService.genererPrivatAvtalePdf(personIdent, dto) } returns
                java.io.ByteArrayOutputStream().apply {
                    // Minimal PDF-like bytes: "%PDF-1.4\n%%EOF"
                    write(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A, 0x25, 0x25, 0x45, 0x4F, 0x46))
                }

        postRequest(
            "/api/v1/privat-avtale/under-18",
            dto,
            gyldigOAuth2Token
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", "inline; filename=\"privatavtale.pdf\""))
    }

    @Test
    fun `skal gi feil hvis harAndreBestemmelser er true men beskrivelse mangler`() {
        val dto = lagGyldigPrivatAvtaleBarnUnder18RequestDto().copy(
            andreBestemmelser = AndreBestemmelserSkjema(harAndreBestemmelser = true, beskrivelse = null)
        )
        val personIdent = "12345678910"
        every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns personIdent
        every { privatAvtalePdfService.genererPrivatAvtalePdf(any(), any()) } returns ByteArrayOutputStream()

        postRequest("/api/v1/privat-avtale/under-18", dto, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail")
                .value("andreBestemmelser.beskrivelse: beskrivelse må settes når harAndreBestemmelser=true"))

        // Tjenesten skal ikke kalles når validering feiler
        verify(exactly = 0) { privatAvtalePdfService.genererPrivatAvtalePdf(any(), any()) }
    }

    @Test
    fun `skal gi feil hvis nyAvtale er false og oppgjørsformIdag er null`() {
        val dto = lagGyldigPrivatAvtaleBarnUnder18RequestDto().copy(
            oppgjør = Oppgjør(
                nyAvtale = false,
                oppgjørsformØnsket = Oppgjørsform.INNKREVING,
                oppgjørsformIdag = null // trigger @ValidOppgjør
            )
        )
        val personIdent = "12345678910"
        every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns personIdent
        every { privatAvtalePdfService.genererPrivatAvtalePdf(any(), any()) } returns ByteArrayOutputStream()

        postRequest("/api/v1/privat-avtale/under-18", dto, gyldigOAuth2Token)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.detail")
                .value("oppgjør.oppgjørsformIdag: oppgjørsformIdag må settes når nyAvtale=false"))

        verify(exactly = 0) { privatAvtalePdfService.genererPrivatAvtalePdf(any(), any()) }
    }

    private fun lagGyldigPrivatAvtaleBarnUnder18RequestDto(): PrivatAvtaleBarnUnder18RequestDto {
        return PrivatAvtaleBarnUnder18RequestDto(
            språk = Språkkode.NB,
            bidragsmottaker = PrivatAvtalePart("Ola", "Nordmann", Personident("12345678901")),
            bidragspliktig = PrivatAvtalePart("Kari", "Nordmann", Personident("10987654321")),
            barn = listOf(PrivatAvtaleBarn("Barn", "Nordmann", Personident("01010112345"), BigDecimal("1000"), fraDato = "2025-01-01")),
            oppgjør = Oppgjør(nyAvtale = true, oppgjørsformØnsket = Oppgjørsform.INNKREVING),
            vedlegg = Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON,
            andreBestemmelser = AndreBestemmelserSkjema(false, null)
        )
    }
}
