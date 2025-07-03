package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarn
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBidragsmottaker
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBidragspliktig
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import no.nav.bidrag.bidragskalkulator.prosessor.PdfProsessor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class PrivatAvtalePdfServiceTest {

    lateinit var privatAvtalePdfService: PrivatAvtalePdfService

    val mockBidragDokumentConsumer by lazy { mockk<BidragDokumentProduksjonConsumer>() }
    val mockFoerstesidegeneratorConsumer by lazy { mockk<FoerstesidegeneratorConsumer>() }
    val mockPdfProsessor by lazy { mockk<PdfProsessor>() }

    @BeforeEach
    fun setUp() {
        privatAvtalePdfService = PrivatAvtalePdfService(mockBidragDokumentConsumer, mockFoerstesidegeneratorConsumer, mockPdfProsessor)
    }


    @Test
    fun `skal generere foersteside for en privat avtale`() {
        // Arrange
        val expectedPdfContent = this.javaClass.getResourceAsStream("/pdf/privatavtale-kun-forside.pdf")?.readAllBytes()
            ?: throw IllegalStateException("Kunne ikke lese inn PDF privatavtale-kun-forside.pdf")

        // Mock behavior
        every { mockFoerstesidegeneratorConsumer.genererFoersteside(any())  }.returns(ByteArrayOutputStream().apply { write(expectedPdfContent) })

        // Act
        val genererForsideOutputStream = runBlocking {
            privatAvtalePdfService.genererForsideForInnsending("12345678901")
        }

        Assertions.assertArrayEquals(expectedPdfContent, genererForsideOutputStream.toByteArray())
    }

    @Test
    fun `skal generere privat avtale PDF`() {
        val expectedPdfContent = this.javaClass.getResourceAsStream("/pdf/privatavtale-kun-kontrakt.pdf")?.readAllBytes()

            every { mockBidragDokumentConsumer.genererPrivatAvtaleAPdf(any()) }.returns(ByteArrayOutputStream().apply { write(expectedPdfContent) })

            every { mockPdfProsessor.prosesserOgSlåSammenDokumenter(any()) }.returns(expectedPdfContent!!)
            val privatAvtale = runBlocking { privatAvtalePdfService.genererPrivatAvtalePdf("12345678910",
                PrivatAvtalePdfDto(
                    innhold = "Test Innhold",
                    bidragsmottaker = PrivatAvtaleBidragsmottaker(fulltNavn = "Test Mottaker", "12345678910"),
                    bidragspliktig = PrivatAvtaleBidragspliktig(fulltNavn = "Test Mottaker", "12345678910"),
                    barn = listOf(PrivatAvtaleBarn(fulltNavn = "Test Mottaker", "12345678910", sumBidrag = 1000.0)),
                    fraDato = "1.1.2023",
                    nyAvtale = true,
                    oppgjorsform = "Bankoverføring",
                    tilInnsending = false,
                ))
            }



        Assertions.assertArrayEquals(expectedPdfContent, privatAvtale.toByteArray())
    }

}