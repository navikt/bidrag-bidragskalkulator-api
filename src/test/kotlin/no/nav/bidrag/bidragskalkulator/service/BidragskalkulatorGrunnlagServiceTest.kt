package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.dto.BidragskalkulatorGrunnlagDto
import no.nav.bidrag.bidragskalkulator.dto.SamværsfradragPeriode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

class BidragskalkulatorGrunnlagServiceTest {

    private val boOgForbruksutgiftService: BoOgForbruksutgiftService = mockk()
    private val sjablonService: SjablonService = mockk()

    private val service = BidragskalkulatorGrunnlagService(
        sjablonService = sjablonService,
        boOgForbruksutgiftService = boOgForbruksutgiftService
    )

    @Test
    fun `hentGrunnlagsData returnerer kombinert dto fra alle tjenester`() = runBlocking {
        val underhold = linkedMapOf(
            6 to BigDecimal(6547),
            11 to BigDecimal(7240)
        )
        val samvaer = listOf(
            SamværsfradragPeriode(
                alderFra = 0,
                alderTil = 5,
                beløpFradrag = mapOf(
                    "SAMVÆRSKLASSE_1" to BigDecimal(317),
                    "SAMVÆRSKLASSE_2" to BigDecimal(475),
                )
            )
        )
        val barnInntektsgrense = BigDecimal(60300)
        val selvforsørgetBarnInntektsgrense = BigDecimal(201000)

        every { boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell() } returns underhold
        every { sjablonService.hentSamværsfradrag() } returns samvaer
        every { sjablonService.hentForskuddssats() } returns BigDecimal(2010)

        // when
        val result: BidragskalkulatorGrunnlagDto = service.hentGrunnlagsData()

        // then
        assertEquals(underhold, result.boOgForbruksutgifter)
        assertEquals(samvaer, result.samværsfradrag)
        assertEquals(barnInntektsgrense, result.barnInntektsgrense)
        assertEquals(selvforsørgetBarnInntektsgrense, result.selvforsørgetBarnInntektsgrense)

        verify(exactly = 1) { boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell() }
        verify(exactly = 1) { sjablonService.hentSamværsfradrag() }
        verify(exactly = 1) { sjablonService.hentForskuddssats() }
    }

    @Test
    fun `hentGrunnlagsData kaster videre hvis underholdskostnadService feiler`() = runBlocking {
        every { boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell() } throws RuntimeException("Feil")
        every { sjablonService.hentSamværsfradrag() } returns emptyList()
        every { sjablonService.hentForskuddssats() } returns BigDecimal(2010)

        assertThrows<RuntimeException> {
            service.hentGrunnlagsData()
        }

        verify(exactly = 1) { boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell() }
        verify(exactly = 0) { sjablonService.hentSamværsfradrag() }
        verify(exactly = 1) { sjablonService.hentForskuddssats() }
    }

    @Test
    fun `hentGrunnlagsData kaster videre hvis sjablonService feiler`() = runBlocking {
        every { boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell() } returns mapOf(
            6 to BigDecimal(6547)
        )
        every { sjablonService.hentSamværsfradrag() } throws IllegalStateException("sjablon nede")
        every { sjablonService.hentForskuddssats() } returns BigDecimal(2010)

        assertThrows<IllegalStateException> {
            service.hentGrunnlagsData()
        }

        verify(exactly = 1) { boOgForbruksutgiftService.genererBoOgForbruksutgiftstabell() }
        verify(exactly = 1) { sjablonService.hentSamværsfradrag() }
        verify(exactly = 1) { sjablonService.hentForskuddssats() }

    }
}
