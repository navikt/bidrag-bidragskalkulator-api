package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.*
import no.nav.bidrag.bidragskalkulator.prosessor.PdfProsessor
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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

    @Nested
    inner class ForsideReglerTest {

        private val forventetForside =
            this::class.java.getResourceAsStream("/pdf/privatavtale-kun-forside.pdf")?.readAllBytes()
                ?: error("Mangler testfil for forside")

        private val forventetKontrakt =
            this::class.java.getResourceAsStream("/pdf/privatavtale-kun-kontrakt.pdf")?.readAllBytes()
                ?: error("Mangler testfil for kontrakt")

        private fun dto(
            nyAvtale: Boolean,
            ønsket: Oppgjørsform,
            idag: Oppgjørsform? = null
        ) = PrivatAvtalePdfDto(
            bidragsmottaker = PrivatAvtaleBidragsmottaker("Mottaker", "Etternavnesen", "22222222222"),
            bidragspliktig = PrivatAvtaleBidragspliktig("Pliktig", "Etternavnesen", "33333333333"),
            barn = listOf(PrivatAvtaleBarn("Barn", "Etternavnesen", "11111111111", 1000.0, fraDato = "2025-01-01")),
            oppgjør = Oppgjør(
                nyAvtale = nyAvtale,
                oppgjørsformØnsket = ønsket,
                oppgjørsformIdag = idag
            ),
            språk = Språkkode.NB,
            vedlegg = Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON,
            andreBestemmelser = AndreBestemmelserSkjema(harAndreBestemmelser = false),
            navSkjemaId = NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18
        )

        private fun mockForside() {
            every { mockFoerstesideConsumer.genererFoersteside(any()) } returns
                    GenererFoerstesideResultatDto(foersteside = forventetForside, loepenummer = "123")
        }

        private fun mockKontrakt() {
            every { mockDokumentConsumer.genererPrivatAvtaleAPdf(any()) } returns
                    ByteArrayOutputStream().apply { write(forventetKontrakt) }
            every { mockPdfProsessor.prosesserOgSlåSammenDokumenter(any()) } returns forventetKontrakt
        }

        // ---------------- GENERER FORSIDE ----------------
        @Test
        fun `Eksisterende + INNKREVING idag + ønsket INNKREVING = forside`() {
            mockForside()
            val faktisk = service.genererForsideForInnsending("fnr", dto(false, Oppgjørsform.INNKREVING, Oppgjørsform.INNKREVING))
            assertArrayEquals(forventetForside, faktisk)
        }

        @Test
        fun `Eksisterende + PRIVAT idag + ønsket INNKREVING = forside`() {
            mockForside()
            val faktisk = service.genererForsideForInnsending("fnr", dto(false, Oppgjørsform.INNKREVING, Oppgjørsform.PRIVAT))
            assertArrayEquals(forventetForside, faktisk)
        }

        @Test
        fun `Eksisterende + INNKREVING idag + ønsket PRIVAT = forside`() {
            mockForside()
            val faktisk = service.genererForsideForInnsending("fnr", dto(false, Oppgjørsform.PRIVAT, Oppgjørsform.INNKREVING))
            assertArrayEquals(forventetForside, faktisk)
        }

        @Test
        fun `Ny avtale + ønsket INNKREVING = forside`() {
            mockForside()
            val faktisk = service.genererForsideForInnsending("fnr", dto(true, Oppgjørsform.INNKREVING))
            assertArrayEquals(forventetForside, faktisk)
        }

        // ---------------- IKKE GENERER FORSIDE ----------------
        @Test
        fun `Eksisterende + PRIVAT idag + ønsket PRIVAT = kun kontrakt`() {
            mockKontrakt()
            val faktisk = service.genererPrivatAvtalePdf("fnr", dto(false, Oppgjørsform.PRIVAT, Oppgjørsform.PRIVAT))
            assertArrayEquals(forventetKontrakt, faktisk.toByteArray())
        }

        @Test
        fun `Ny avtale + ønsket PRIVAT = kun kontrakt`() {
            mockKontrakt()
            val faktisk = service.genererPrivatAvtalePdf("fnr", dto(true, Oppgjørsform.PRIVAT))
            assertArrayEquals(forventetKontrakt, faktisk.toByteArray())
        }
    }
}
