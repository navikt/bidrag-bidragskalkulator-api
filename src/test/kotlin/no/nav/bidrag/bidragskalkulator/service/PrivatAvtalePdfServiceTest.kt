package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FørstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.GenererFørstesideResultatDto
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.Språkkode
import no.nav.bidrag.bidragskalkulator.prosessor.PdfProsessor
import no.nav.bidrag.domene.ident.Personident
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate

class PrivatAvtalePdfServiceTest {

    private lateinit var service: PrivatAvtalePdfService
    private val mockDokumentConsumer = mockk<BidragDokumentProduksjonConsumer>()
    private val mockFoerstesideConsumer = mockk<FørstesidegeneratorConsumer>()
    private val mockPdfProsessor = mockk<PdfProsessor>()

    @BeforeEach
    fun init() {
        service = PrivatAvtalePdfService(mockDokumentConsumer, mockFoerstesideConsumer, mockPdfProsessor)
    }

    @Nested
    inner class ForsideReglerTest {

        private val forsideBytes =
            this::class.java.getResourceAsStream("/pdf/privatavtale-kun-forside.pdf")?.readAllBytes()
                ?: error("Mangler testfil for forside")
        private val kontraktBytes =
            this::class.java.getResourceAsStream("/pdf/privatavtale-kun-kontrakt.pdf")?.readAllBytes()
                ?: error("Mangler testfil for kontrakt")

        // DTO-bygger for U18 (service håndterer U18/UOVER18 likt gjennom felles inngang)
        private fun dtoUnder18(
            nyAvtale: Boolean,
            ønsket: Oppgjørsform,
            idag: Oppgjørsform? = null
        ) = PrivatAvtaleBarnUnder18RequestDto(
            bidragsmottaker = PrivatAvtalePart("Ola", "Nordmann", Personident("12345678901")),
            bidragspliktig = PrivatAvtalePart("Kari", "Nordmann", Personident("10987654321")),
            barn = listOf(
                PrivatAvtaleBarn(
                    "Barn", "Etternavnesen", Personident("11111111111"),
                    sumBidrag = BigDecimal("1000"),
                    fraDato = LocalDate.now()
                )
            ),
            oppgjør = Oppgjør(nyAvtale = nyAvtale, oppgjørsformØnsket = ønsket, oppgjørsformIdag = idag),
            språk = Språkkode.NB,
            vedlegg = Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON,
            andreBestemmelser = AndreBestemmelserSkjema(harAndreBestemmelser = false),
        )

        private fun mockPrivatAvtaleDokument() {
            every { mockDokumentConsumer.genererPrivatAvtaleAPdf(any()) } returns
                    ByteArrayOutputStream().apply { write(kontraktBytes) }
        }

        private fun mockForsideGenerator() {
            every { mockFoerstesideConsumer.genererFørsteside(any()) } returns
                    GenererFørstesideResultatDto(foersteside = forsideBytes, loepenummer = "123")
        }

        private fun mockSammenslåingUtenForside() {
            every { mockPdfProsessor.prosesserOgSlåSammenDokumenter(any()) } returns kontraktBytes
        }

        private fun mockSammenslåingMedForside(sammenslått: ByteArray) {
            every { mockPdfProsessor.prosesserOgSlåSammenDokumenter(match { it.size == 2 }) } returns sammenslått
        }

        // ---------------- GENERER FORSIDE ----------------

        @Test
        fun `Eksisterende + INNKREVING idag + ønsket INNKREVING = forside`() {
            mockPrivatAvtaleDokument()
            mockForsideGenerator()
            val forventetSammenslaatt = "MERGED_FORSIDE_KONTRAKT_1".toByteArray()
            mockSammenslåingMedForside(forventetSammenslaatt)

            val faktisk = service.genererPrivatAvtalePdf("fnr", dtoUnder18(false, Oppgjørsform.INNKREVING, Oppgjørsform.INNKREVING))
                .toByteArray()

            // Forvent at forside ble brukt og at output er sammenslått dokument
            verify(exactly = 1) { mockFoerstesideConsumer.genererFørsteside(any()) }
            assertArrayEquals(forventetSammenslaatt, faktisk)
        }

        @Test
        fun `Eksisterende + PRIVAT idag + ønsket INNKREVING = forside`() {
            mockPrivatAvtaleDokument()
            mockForsideGenerator()
            val forventetSammenslaatt = "MERGED_FORSIDE_KONTRAKT_2".toByteArray()
            mockSammenslåingMedForside(forventetSammenslaatt)

            val faktisk = service.genererPrivatAvtalePdf("fnr", dtoUnder18(false, Oppgjørsform.INNKREVING, Oppgjørsform.PRIVAT))
                .toByteArray()

            verify(exactly = 1) { mockFoerstesideConsumer.genererFørsteside(any()) }
            assertArrayEquals(forventetSammenslaatt, faktisk)
        }

        @Test
        fun `Eksisterende + INNKREVING idag + ønsket PRIVAT = forside`() {
            mockPrivatAvtaleDokument()
            mockForsideGenerator()
            val forventetSammenslaatt = "MERGED_FORSIDE_KONTRAKT_3".toByteArray()
            mockSammenslåingMedForside(forventetSammenslaatt)

            val faktisk = service.genererPrivatAvtalePdf("fnr", dtoUnder18(false, Oppgjørsform.PRIVAT, Oppgjørsform.INNKREVING))
                .toByteArray()

            verify(exactly = 1) { mockFoerstesideConsumer.genererFørsteside(any()) }
            assertArrayEquals(forventetSammenslaatt, faktisk)
        }

        @Test
        fun `Ny avtale + ønsket INNKREVING = forside`() {
            mockPrivatAvtaleDokument()
            mockForsideGenerator()
            val forventetSammenslaatt = "MERGED_FORSIDE_KONTRAKT_4".toByteArray()
            mockSammenslåingMedForside(forventetSammenslaatt)

            val faktisk = service.genererPrivatAvtalePdf("fnr", dtoUnder18(true, Oppgjørsform.INNKREVING))
                .toByteArray()

            verify(exactly = 1) { mockFoerstesideConsumer.genererFørsteside(any()) }
            assertArrayEquals(forventetSammenslaatt, faktisk)
        }

        // --------------- IKKE GENERER FORSIDE (2 kombinasjoner) ------------

        @Test
        fun `Eksisterende + PRIVAT idag + ønsket PRIVAT = kun kontrakt`() {
            mockPrivatAvtaleDokument()
            mockSammenslåingUtenForside()

            val faktisk = service.genererPrivatAvtalePdf("fnr", dtoUnder18(false, Oppgjørsform.PRIVAT, Oppgjørsform.PRIVAT))
                .toByteArray()

            // Forvent at forside ikke ble generert og kontrakt returneres
            verify(exactly = 0) { mockFoerstesideConsumer.genererFørsteside(any()) }
            assertArrayEquals(kontraktBytes, faktisk)
        }

        @Test
        fun `Ny avtale + ønsket PRIVAT = kun kontrakt`() {
            mockPrivatAvtaleDokument()
            mockSammenslåingUtenForside()

            val faktisk = service.genererPrivatAvtalePdf("fnr", dtoUnder18(true, Oppgjørsform.PRIVAT))
                .toByteArray()

            verify(exactly = 0) { mockFoerstesideConsumer.genererFørsteside(any()) }
            assertArrayEquals(kontraktBytes, faktisk)
        }
    }
}
