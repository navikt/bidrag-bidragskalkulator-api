package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockkObject
import no.nav.bidrag.commons.service.sjablon.Samværsfradrag
import no.nav.bidrag.commons.service.sjablon.SjablonProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SjablonServiceTest {

    private lateinit var sjablonService: SjablonService

    @BeforeEach
    fun setUp() {
        sjablonService = SjablonService()
        mockkObject(SjablonProvider)
    }

    @Test
    fun `skal returnere tom liste hvis ingen sjabloner finnes for dagens dato`() {
        every { SjablonProvider.hentSjablonSamværsfradrag() } returns listOf(
            Samværsfradrag(
                datoFom = LocalDate.now().minusYears(2),
                datoTom = LocalDate.now().minusYears(1),
                alderTom = 5,
                samvaersklasse = "1",
                belopFradrag = BigDecimal("100")
            )
        )

        val resultat = sjablonService.hentSamværsfradrag()

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun `skal gruppere og mappe samværsfradrag korrekt`()  {
        val iDag = LocalDate.now()
        every { SjablonProvider.hentSjablonSamværsfradrag() } returns listOf(
            Samværsfradrag(
                datoFom = iDag.minusDays(1),
                datoTom = iDag.plusDays(1),
                alderTom = 5,
                samvaersklasse = "1",
                belopFradrag = BigDecimal("100")
            ),
            Samværsfradrag(
                datoFom = iDag.minusDays(1),
                datoTom = iDag.plusDays(1),
                alderTom = 10,
                samvaersklasse = "2",
                belopFradrag = BigDecimal("200")
            )
        )

        val resultat = sjablonService.hentSamværsfradrag()

        assertEquals(2, resultat.size)
        assertEquals(0, resultat[0].alderFra)
        assertEquals(5, resultat[0].alderTil)
        assertEquals(BigDecimal("100"), resultat[0].beløpFradrag["SAMVÆRSKLASSE_1"])
        assertEquals(6, resultat[1].alderFra)
        assertEquals(10, resultat[1].alderTil)
        assertEquals(BigDecimal("200"), resultat[1].beløpFradrag["SAMVÆRSKLASSE_2"])
    }

    @Test
    fun `skal håndtere nullverdier for samvaersklasse og belopFradrag`()  {
        val iDag = LocalDate.now()
        every { SjablonProvider.hentSjablonSamværsfradrag() } returns listOf(
            Samværsfradrag(
                datoFom = iDag.minusDays(1),
                datoTom = iDag.plusDays(1),
                alderTom = 7,
                samvaersklasse = null,
                belopFradrag = null
            )
        )

        val resultat = sjablonService.hentSamværsfradrag()
        assertEquals(1, resultat.size)
        assertEquals(BigDecimal.ZERO, resultat[0].beløpFradrag["SAMVÆRSKLASSE_0"])
    }

    @Test
    fun `skal bruke 99 som default for alderTom hvis null`()  {
        val iDag = LocalDate.now()
        every { SjablonProvider.hentSjablonSamværsfradrag() } returns listOf(
            Samværsfradrag(
                datoFom = iDag.minusDays(1),
                datoTom = iDag.plusDays(1),
                alderTom = null,
                samvaersklasse = "3",
                belopFradrag = BigDecimal("300")
            )
        )

        val resultat = sjablonService.hentSamværsfradrag()
        assertEquals(1, resultat.size)
        assertEquals(99, resultat[0].alderTil)
        assertEquals(BigDecimal("300"), resultat[0].beløpFradrag["SAMVÆRSKLASSE_3"])
    }

    /**
     * Test for å sikre at flere samværsklasser for samme alderTom i samme periode blir samlet i én periode.
     * AlderFom skal alltid være 0 for første periode, og oppdateres etter hver iterasjon.
     */
    @Test
    fun `skal samle flere samværsklasser for samme alderTom i samme periode`() {
        val iDag = LocalDate.now()
        every { SjablonProvider.hentSjablonSamværsfradrag() } returns listOf(
            Samværsfradrag(
                datoFom = iDag.minusDays(1),
                datoTom = iDag.plusDays(1),
                alderTom = 5,
                samvaersklasse = "1",
                belopFradrag = BigDecimal("100")
            ),
            Samværsfradrag(
                datoFom = iDag.minusDays(1),
                datoTom = iDag.plusDays(1),
                alderTom = 5,
                samvaersklasse = "2",
                belopFradrag = BigDecimal("200")
            )
        )

        val resultat = sjablonService.hentSamværsfradrag()

        assertEquals(1, resultat.size)
        assertEquals(0, resultat[0].alderFra)
        assertEquals(5, resultat[0].alderTil)
        assertEquals(BigDecimal("100"), resultat[0].beløpFradrag["SAMVÆRSKLASSE_1"])
        assertEquals(BigDecimal("200"), resultat[0].beløpFradrag["SAMVÆRSKLASSE_2"])
    }

    /**
     * Test for å sikre at alderFom og alderTom blir beregnet riktig selv om sjablonene ikke er sortert på alderTom.
     * AlderFom skal alltid være 0 for første periode, og oppdateres etter hver iterasjon.
     */
    @Test
    fun `skal håndtere usortert alderTom og beregne alderFom riktig`() {
        val iDag = LocalDate.now()
        every { SjablonProvider.hentSjablonSamværsfradrag() } returns listOf(
            Samværsfradrag(
                datoFom = iDag.minusDays(1),
                datoTom = iDag.plusDays(1),
                alderTom = 10,
                samvaersklasse = "1",
                belopFradrag = BigDecimal("100")
            ),
            Samværsfradrag(
                datoFom = iDag.minusDays(1),
                datoTom = iDag.plusDays(1),
                alderTom = 5,
                samvaersklasse = "2",
                belopFradrag = BigDecimal("200")
            )
        )

        val resultat = sjablonService.hentSamværsfradrag()

        assertEquals(2, resultat.size)
        assertEquals(0, resultat[0].alderFra)
        assertEquals(5, resultat[0].alderTil)
        assertEquals(6, resultat[1].alderFra)
        assertEquals(10, resultat[1].alderTil)
    }
}
