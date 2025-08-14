package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.*
import no.nav.bidrag.bidragskalkulator.prosessor.PdfProsessor
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class PrivatAvtalePdfServiceTest {

    private lateinit var service: PrivatAvtalePdfService
    private val mockDokumentConsumer = mockk<BidragDokumentProduksjonConsumer>()
    private val mockFoerstesideConsumer = mockk<FoerstesidegeneratorConsumer>()
    private val mockPdfProsessor = mockk<PdfProsessor>()

    @BeforeEach
    fun init() {
        service = PrivatAvtalePdfService(mockDokumentConsumer, mockFoerstesideConsumer, mockPdfProsessor)
    }

    @Test
    fun `skal generere kun førsteside ved tilInnsending`() = runBlocking {
        // Arrange
        val forventetForside = this::class.java.getResourceAsStream("/pdf/privatavtale-kun-forside.pdf")?.readAllBytes()
            ?: error("Mangler testfil for forside")

        every { mockFoerstesideConsumer.genererFoersteside(any()) } returns
                GenererFoerstesideResultatDto(foersteside = forventetForside, loepenummer = "123")

        val dto = PrivatAvtalePdfDto(
            bidragsmottaker = PrivatAvtaleBidragsmottaker("Mottaker", "Etternavnesen", "22222222222"),
            bidragspliktig = PrivatAvtaleBidragspliktig("Pliktig", "Etternavnesen", "33333333333"),
            barn = listOf(PrivatAvtaleBarn("Barn", "Etternavnesen", "11111111111", 1000.0, fraDato = "2025-01-01")),
            oppgjør = Oppgjør(
                nyAvtale = true,
                oppgjørsformØnsket = Oppgjørsform.INNKREVING,
                oppgjørsformIdag = Oppgjørsform.INNKREVING,
            ),
            språk = Språkkode.NB,
            vedlegg = Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON,
            andreBestemmelser = AndreBestemmelserSkjema(harAndreBestemmelser = false),
            navSkjemaId = NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18
        )

        // Act
        val faktiskForside = service.genererForsideForInnsending("12345678901", dto)

        // Assert
        assertArrayEquals(forventetForside, faktiskForside)
    }

    @Test
    fun `skal generere komplett privat avtale PDF uten forside`() = runBlocking {
        // Arrange
        val forventetKontrakt = this::class.java.getResourceAsStream("/pdf/privatavtale-kun-kontrakt.pdf")?.readAllBytes()
            ?: error("Mangler testfil for kontrakt")

        every { mockDokumentConsumer.genererPrivatAvtaleAPdf(any()) } returns
                ByteArrayOutputStream().apply { write(forventetKontrakt) }

        every { mockPdfProsessor.prosesserOgSlåSammenDokumenter(any()) } returns forventetKontrakt

        val dto = PrivatAvtalePdfDto(
            bidragsmottaker = PrivatAvtaleBidragsmottaker("Mottaker", "Etternavnesen", "22222222222"),
            bidragspliktig = PrivatAvtaleBidragspliktig("Pliktig", "Etternavnesen", "33333333333"),
            barn = listOf(PrivatAvtaleBarn("Barn", "Etternavnesen", "11111111111", 1000.0, fraDato = "2025-01-01")),
            oppgjør = Oppgjør(
                nyAvtale = true,
                oppgjørsformØnsket = Oppgjørsform.INNKREVING,
                oppgjørsformIdag = Oppgjørsform.INNKREVING,
            ),
            språk = Språkkode.NB,
            vedlegg = Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON,
            andreBestemmelser = AndreBestemmelserSkjema(harAndreBestemmelser = false),
            navSkjemaId = NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18
        )

        // Act
        val faktisk = service.genererPrivatAvtalePdf("12345678910", dto)

        // Assert
        assertArrayEquals(forventetKontrakt, faktisk.toByteArray())
    }
}
