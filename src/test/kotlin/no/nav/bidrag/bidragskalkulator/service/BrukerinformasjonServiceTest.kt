package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.bidragskalkulator.mapper.tilFamilieRelasjon
import no.nav.bidrag.bidragskalkulator.model.ForelderBarnRelasjon
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class BrukerinformasjonServiceTest {

    private lateinit var brukerinformasjonService: BrukerinformasjonService
    private val mockPersonService = mockk<PersonService>()
    private val mockGrunnlagService = mockk<GrunnlagService>()
    private val underholdskostnadServiceMock = mockk<UnderholdskostnadService>()
    private val sjablonService = mockk<SjablonService>()

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
        brukerinformasjonService = BrukerinformasjonService(
            mockPersonService,
            mockGrunnlagService,
            sjablonService,
            underholdskostnadServiceMock)
    }

    @Test
    fun `skal kaste NoContentException hvis person ikke finnes`() = runTest {
        every { mockGrunnlagService.hentInntektsGrunnlag(identSomIkkeFinnes) } returns responsInntektsGrunnlag
        every { mockPersonService.hentPersoninformasjon(Personident(identSomIkkeFinnes)) } throws NoContentException("Fant ikke person med ident $identSomIkkeFinnes")
        every { sjablonService.hentSamværsfradrag() } returns emptyList()

        val exception = assertThrows<NoContentException> {
            brukerinformasjonService.hentBrukerinformasjon(identSomIkkeFinnes)
        }

        assertEquals("Fant ikke person med ident $identSomIkkeFinnes", exception.message)
    }

    @Test
    @Disabled
    fun `skal returnere tom barn-relasjonsliste når person ikke har barn`() = runTest {
        val motpartsrelasjoner = responsUtenBarn.personensMotpartBarnRelasjon.tilFamilieRelasjon()

        every { mockPersonService.hentGyldigFamilierelasjon(identUtenBarn) } returns ForelderBarnRelasjon( responsUtenBarn.person, motpartsrelasjoner )
        every { mockGrunnlagService.hentInntektsGrunnlag(identUtenBarn) } returns responsInntektsGrunnlag
        every { underholdskostnadServiceMock.genererUnderholdskostnadstabell() } returns emptyMap()

        val resultat = brukerinformasjonService.hentBrukerinformasjon(identUtenBarn)

        val relasjoner = resultat.barnerelasjoner
        assertEquals(0, relasjoner.size)
    }

    @Test
    @Disabled
    fun `skal returnere flere barn-relasjoner når person har barn med flere motparter`() = runTest {
        val motpartsrelasjoner = responsUtenBarn.personensMotpartBarnRelasjon.tilFamilieRelasjon()

        every { mockPersonService.hentGyldigFamilierelasjon(identMedFlereBarn) } returns ForelderBarnRelasjon( responsMedFlereBarn.person, motpartsrelasjoner)
        every { mockGrunnlagService.hentInntektsGrunnlag(identMedFlereBarn) } returns responsInntektsGrunnlag
        every { underholdskostnadServiceMock.genererUnderholdskostnadstabell() } returns emptyMap()

        val resultat = brukerinformasjonService.hentBrukerinformasjon(identMedFlereBarn)
        val relasjoner = resultat.barnerelasjoner

        assertTrue(relasjoner.size > 1)
    }

    @Test
    fun `skal returnere og mappe gyldig inntekt for person`() = runTest {
        every { mockGrunnlagService.hentInntektsGrunnlag(identMedFlereBarn) } returns responsInntektsGrunnlag
        every { mockPersonService.hentPersoninformasjon(Personident(identMedFlereBarn)) } returns responsMedFlereBarn.person
        every { underholdskostnadServiceMock.genererUnderholdskostnadstabell() } returns emptyMap()
        every { sjablonService.hentSamværsfradrag() } returns emptyList()

        val resultat = brukerinformasjonService.hentBrukerinformasjon(identMedFlereBarn)
        val inntekt12mnd = resultat.inntekt

        assertEquals(BigDecimal(378000), inntekt12mnd)
    }

    @Nested
    @Disabled
    inner class Familierelasjon {
        @Test
        fun `skal returnere én barn-relasjon når person har barn med én motpart`() = runTest {

            val motpartsrelasjoner = responsMedEttBarn.personensMotpartBarnRelasjon.tilFamilieRelasjon()

            every { mockGrunnlagService.hentInntektsGrunnlag(identMedEttBarn) } returns responsInntektsGrunnlag

            val resultat = brukerinformasjonService.hentBrukerinformasjon(identMedEttBarn)

            val relasjoner = resultat.barnerelasjoner

            assertAll(
                "Verifiser én relasjon",
                { assertEquals(1, relasjoner.size) },
                { assertTrue(relasjoner[0].fellesBarn.isNotEmpty()) }
            )
        }

        @Test
        fun `skal returnere tom barn-relasjonsliste når person ikke har barn`() = runTest {
            val motpartsrelasjoner = responsUtenBarn.personensMotpartBarnRelasjon.tilFamilieRelasjon()

            every { mockPersonService.hentGyldigFamilierelasjon(identUtenBarn) } returns ForelderBarnRelasjon( responsUtenBarn.person, motpartsrelasjoner )
            every { mockGrunnlagService.hentInntektsGrunnlag(identUtenBarn) } returns responsInntektsGrunnlag

            val resultat = brukerinformasjonService.hentBrukerinformasjon(identUtenBarn)

            val relasjoner = resultat.barnerelasjoner
            assertEquals(0, relasjoner.size)
        }

        @Test
        fun `skal returnere flere barn-relasjoner når person har barn med flere motparter`() = runTest {
            val motpartsrelasjoner = responsUtenBarn.personensMotpartBarnRelasjon.tilFamilieRelasjon()

            every { mockPersonService.hentGyldigFamilierelasjon(identMedFlereBarn) } returns ForelderBarnRelasjon( responsMedFlereBarn.person, motpartsrelasjoner)
            every { mockGrunnlagService.hentInntektsGrunnlag(identMedFlereBarn) } returns responsInntektsGrunnlag

            val resultat = brukerinformasjonService.hentBrukerinformasjon(identMedFlereBarn)
            val relasjoner = resultat.barnerelasjoner

            assertTrue(relasjoner.size > 1)
        }
    }
}
