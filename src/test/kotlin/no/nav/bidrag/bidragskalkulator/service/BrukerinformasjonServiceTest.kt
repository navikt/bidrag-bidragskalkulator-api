package no.nav.bidrag.bidragskalkulator.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.bidragskalkulator.utils.TestDataUtils.lagBarnUnderholdskostnad
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class BrukerinformasjonServiceTest {

    private lateinit var brukerinformasjonService: BrukerinformasjonService
    private val mockPersonConsumer = mockk<BidragPersonConsumer>()
    private val mockGrunnlagService = mockk<GrunnlagService>()
    private val mockberegnService = mockk<BeregningService>()

    private val identUtenBarn = "05499323087"
    private val identMedEttBarn = "03848797048"
    private val identMedFlereBarn = "08025327080"
    private val identSomIkkeFinnes = "12345678910"

    private val responsUtenBarn: MotpartBarnRelasjonDto =
        JsonUtils.readJsonFile("/person/person_ingen_barn.json")
    private val responsMedEttBarn: MotpartBarnRelasjonDto =
        JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")
    private val responsMedFlereBarn: MotpartBarnRelasjonDto =
        JsonUtils.readJsonFile("/person/person_med_barn_flere_motpart.json")

    private val responsInntektsGrunnlag: TransformerInntekterResponse =
        JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")

    @BeforeEach
    fun setUp()  {
        brukerinformasjonService = BrukerinformasjonService(mockPersonConsumer, mockGrunnlagService, mockberegnService)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `skal kaste NoContentException hvis person ikke finnes`() = runBlocking {
        every { mockPersonConsumer.hentFamilierelasjon(identSomIkkeFinnes) } throws NoContentException("Fant ikke person med ident $identSomIkkeFinnes")
        every { mockGrunnlagService.hentInntektsGrunnlag(identSomIkkeFinnes) } returns responsInntektsGrunnlag

        val exception = assertThrows<NoContentException> {
            brukerinformasjonService.hentBrukerinformasjon(identSomIkkeFinnes)
        }

        assertEquals("Fant ikke person med ident $identSomIkkeFinnes", exception.message)
    }

    @Test
    fun `skal returnere én barn-relasjon når person har barn med én motpart`() = runBlocking {
        every { mockPersonConsumer.hentFamilierelasjon(identMedEttBarn) } returns responsMedEttBarn
        every { mockGrunnlagService.hentInntektsGrunnlag(identMedEttBarn) } returns responsInntektsGrunnlag
        coEvery { mockberegnService.beregnUnderholdskostnaderForBarnIFamilierelasjon(responsMedEttBarn) } returns lagBarnUnderholdskostnad(responsMedEttBarn)

        val resultat = brukerinformasjonService.hentBrukerinformasjon(identMedEttBarn)

        val relasjoner = resultat.barnerelasjoner

        assertAll(
            "Verifiser én relasjon",
            { assertEquals(1, relasjoner.size) },
            { assertTrue(relasjoner[0].fellesBarn.isNotEmpty()) }
        )
    }

    @Test
    fun `skal returnere tom barn-relasjonsliste når person ikke har barn`() = runBlocking {
        every { mockPersonConsumer.hentFamilierelasjon(identUtenBarn) } returns responsUtenBarn
        every { mockGrunnlagService.hentInntektsGrunnlag(identUtenBarn) } returns responsInntektsGrunnlag
        coEvery { mockberegnService.beregnUnderholdskostnaderForBarnIFamilierelasjon(responsUtenBarn) } returns lagBarnUnderholdskostnad(responsUtenBarn)

        val resultat = brukerinformasjonService.hentBrukerinformasjon(identUtenBarn)

        val relasjoner = resultat.barnerelasjoner
        assertEquals(0, relasjoner.size)
    }

    @Test
    fun `skal returnere flere barn-relasjoner når person har barn med flere motparter`() = runBlocking {
        every { mockPersonConsumer.hentFamilierelasjon(identMedFlereBarn) } returns responsMedFlereBarn
        every { mockGrunnlagService.hentInntektsGrunnlag(identMedFlereBarn) } returns responsInntektsGrunnlag
        coEvery { mockberegnService.beregnUnderholdskostnaderForBarnIFamilierelasjon(responsMedFlereBarn) } returns lagBarnUnderholdskostnad(responsMedFlereBarn)

        val resultat = brukerinformasjonService.hentBrukerinformasjon(identMedFlereBarn)
        val relasjoner = resultat.barnerelasjoner

        assertTrue(relasjoner.size > 1)
    }

    @Test
    fun `skal returnere og mappe gyldig inntekt for person`() = runBlocking {
        every { mockPersonConsumer.hentFamilierelasjon(identMedFlereBarn) } returns responsMedFlereBarn
        every { mockGrunnlagService.hentInntektsGrunnlag(identMedFlereBarn) } returns responsInntektsGrunnlag
        coEvery { mockberegnService.beregnUnderholdskostnaderForBarnIFamilierelasjon(responsMedFlereBarn) } returns lagBarnUnderholdskostnad(responsMedFlereBarn)

        val resultat = brukerinformasjonService.hentBrukerinformasjon(identMedFlereBarn)
        val inntekt12mnd = resultat.inntekt

        assertEquals(BigDecimal(378000), inntekt12mnd)
    }
}