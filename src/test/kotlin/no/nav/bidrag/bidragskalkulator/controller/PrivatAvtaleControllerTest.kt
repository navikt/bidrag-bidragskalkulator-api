package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.bidrag.bidragskalkulator.dto.AndreBestemmelserSkjema
import no.nav.bidrag.bidragskalkulator.dto.Bidrag
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.bidragskalkulator.dto.Oppgjør
import no.nav.bidrag.bidragskalkulator.dto.Oppgjørsform
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarn
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnOver18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePart
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnUnder18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.Vedleggskrav
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.Språkkode
import no.nav.bidrag.bidragskalkulator.mapper.tilPrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtalePdfService
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtaleService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.junit.jupiter.api.Nested
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test

class PrivatAvtaleControllerTest: AbstractControllerTest() {
    @MockkBean
    private lateinit var privatAvtaleService: PrivatAvtaleService
    @MockkBean
    private lateinit var privatAvtalePdfService: PrivatAvtalePdfService
    @MockkBean
    private lateinit var innloggetBrukerUtils: InnloggetBrukerUtils

    private val mockResponsPersonMedEnBarnRelasjon: MotpartBarnRelasjonDto =
        JsonUtils.lesJsonFil("/person/person_med_barn_en_motpart.json")

    // Minimal PDF-like bytes: "%PDF-1.4\n%%EOF"
    private val pdfMock = byteArrayOf(
        0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A, 0x25, 0x25, 0x45, 0x4F, 0x46
    )

    @Nested
    inner class BarnUnder18 {
        @Test
        fun `skal returnere informasjon for privat avtale`() {
            val personIdent = genererFødselsnummer()
            val forventetDto = mockResponsPersonMedEnBarnRelasjon.person.tilPrivatAvtaleInformasjonDto()

            every { innloggetBrukerUtils.requirePåloggetPersonIdent(any()) } returns personIdent
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
            val dto = lagGyldigPrivatAvtaleBarnUnder18RequestDto()

            every { privatAvtalePdfService.genererPrivatAvtalePdf(dto) } returns
                    ByteArrayOutputStream().apply { write(pdfMock) }

            postRequest(
                "/api/v1/privat-avtale/under-18",
                dto,
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(APPLICATION_PDF))
                .andExpect(header()
                    .string("Content-Disposition", "inline; filename=\"privatavtale.pdf\""))
        }

        @Test
        fun `skal gi feil hvis harAndreBestemmelser er true men beskrivelse mangler`() {
            val dto = lagGyldigPrivatAvtaleBarnUnder18RequestDto().copy(
                andreBestemmelser = AndreBestemmelserSkjema(harAndreBestemmelser = true, beskrivelse = null)
            )

            every { privatAvtalePdfService.genererPrivatAvtalePdf(any()) } returns
                    ByteArrayOutputStream()

            postRequest("/api/v1/privat-avtale/under-18", dto)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("\$.detail")
                    .value(
                        "andreBestemmelser.beskrivelse: beskrivelse må settes når harAndreBestemmelser=true"
                    )
                )

            // Tjenesten skal ikke kalles når validering feiler
            verify(exactly = 0) { privatAvtalePdfService.genererPrivatAvtalePdf(any()) }
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
            val personIdent = genererFødselsnummer()
            every { innloggetBrukerUtils.hentPåloggetPersonIdent() } returns personIdent
            every { privatAvtalePdfService.genererPrivatAvtalePdf(any()) } returns ByteArrayOutputStream()

            postRequest("/api/v1/privat-avtale/under-18", dto)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("\$.detail")
                    .value("oppgjør.oppgjørsformIdag: oppgjørsformIdag må settes når nyAvtale=false"))

            verify(exactly = 0) { privatAvtalePdfService.genererPrivatAvtalePdf(any()) }
        }

        private fun lagGyldigPrivatAvtaleBarnUnder18RequestDto(): PrivatAvtaleBarnUnder18RequestDto {
            return PrivatAvtaleBarnUnder18RequestDto(
                språk = Språkkode.NB,
                bidragstype = BidragsType.MOTTAKER,
                bidragsmottaker = PrivatAvtalePart(
                    "Ola",
                    "Nordmann",
                    genererPersonident()
                ),
                bidragspliktig = PrivatAvtalePart(
                    "Kari",
                    "Nordmann",
                    genererPersonident()
                ),
                barn = listOf(PrivatAvtaleBarn(
                    "Barn",
                    "Nordmann",
                    genererPersonident(),
                    BigDecimal("1000"),
                    fraDato = LocalDate.of(2025, 1, 15)
                )),
                oppgjør = Oppgjør(nyAvtale = true, oppgjørsformØnsket = Oppgjørsform.INNKREVING),
                vedlegg = Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON,
                andreBestemmelser = AndreBestemmelserSkjema(false, null)
            )
        }
    }

    @Nested
    inner class BarnOver18 {
        @Test
        fun `skal generere privat avtale PDF for barn over 18`() {
            val dto = lagGyldigPrivatAvtaleBarnOver18RequestDto()

            every { privatAvtalePdfService.genererPrivatAvtalePdf(dto) } returns
                    ByteArrayOutputStream().apply { write(pdfMock) }

            postRequest(
                "/api/v1/privat-avtale/over-18",
                dto,
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(APPLICATION_PDF))
                .andExpect(
                    header().string(
                        "Content-Disposition", "inline; filename=\"privatavtale.pdf\""
                    )
                )

            verify(exactly = 1) { privatAvtalePdfService.genererPrivatAvtalePdf(dto) }
        }

        @Test
        fun `skal gi feil for barn over 18 hvis tilDato er før fraDato`() {
            // Ugyldig: tilDato < fraDato
            val ugyldigDto = lagGyldigPrivatAvtaleBarnOver18RequestDto().copy(
                bidrag = listOf(
                    Bidrag(
                        bidragPerMåned = BigDecimal("1200"),
                        fraDato = YearMonth.of(2025, 5),
                        tilDato = YearMonth.of(2025, 4) // trigger @GyldigPeriode
                    )
                )
            )

            every { privatAvtalePdfService.genererPrivatAvtalePdf(any()) } returns
                    ByteArrayOutputStream()

            postRequest("/api/v1/privat-avtale/over-18", ugyldigDto)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("\$.detail")
                    .value(org.hamcrest.Matchers.containsString(
                        "tilDato kan ikke være før fraDato"
                    )))

            verify(exactly = 0) { privatAvtalePdfService.genererPrivatAvtalePdf(any()) }
        }

        private fun lagGyldigPrivatAvtaleBarnOver18RequestDto(): PrivatAvtaleBarnOver18RequestDto {
            return PrivatAvtaleBarnOver18RequestDto(
                språk = Språkkode.NB,
                bidragstype = BidragsType.MOTTAKER,
                bidragsmottaker = PrivatAvtalePart(
                    "Ola",
                    "Nordmann",
                    genererPersonident()
                ),
                bidragspliktig = PrivatAvtalePart(
                    "Kari",
                    "Nordmann",
                    genererPersonident()
                ),
                bidrag = listOf(
                    Bidrag(
                        bidragPerMåned = BigDecimal("1500"),
                        fraDato = YearMonth.of(2025, 1),
                        tilDato = YearMonth.of(2025, 3)
                    )
                ),
                oppgjør = Oppgjør(nyAvtale = true, oppgjørsformØnsket = Oppgjørsform.INNKREVING),
                vedlegg = Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON,
                andreBestemmelser = AndreBestemmelserSkjema(false, null)
            )
        }

    }
}
