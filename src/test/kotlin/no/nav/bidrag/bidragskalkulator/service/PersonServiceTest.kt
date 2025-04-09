package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class PersonServiceTest {

    private lateinit var personService: PersonService
    private val mockPersonConsumer = mockk<BidragPersonConsumer>()

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

    @BeforeEach
    fun setUp() {
        personService = PersonService(mockPersonConsumer)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `skal kaste NoContentException hvis person ikke finnes`() {
        every { mockPersonConsumer.hentFamilierelasjon(identSomIkkeFinnes) } returns null

        val exception = assertThrows<NoContentException> {
            personService.hentFamilierelasjon(identSomIkkeFinnes)
        }

        assertEquals("Fant ikke person med ident $identSomIkkeFinnes", exception.message)
    }

    @Test
    fun `skal returnere én barn-relasjon når person har barn med én motpart`() {
        every { mockPersonConsumer.hentFamilierelasjon(identMedEttBarn) } returns responsMedEttBarn

        val resultat = personService.hentFamilierelasjon(identMedEttBarn)
        assertNotNull(resultat)

        val relasjoner = resultat!!.personensMotpartBarnRelasjon
        assertAll(
            "Verifiser én relasjon",
            { assertEquals(1, relasjoner.size) },
            { assertTrue(relasjoner[0].fellesBarn.isNotEmpty()) }
        )
    }

    @Test
    fun `skal returnere tom barn-relasjonsliste når person ikke har barn`() {
        every { mockPersonConsumer.hentFamilierelasjon(identUtenBarn) } returns responsUtenBarn

        val resultat = personService.hentFamilierelasjon(identUtenBarn)
        assertNotNull(resultat)

        val relasjoner = resultat!!.personensMotpartBarnRelasjon
        assertEquals(0, relasjoner.size)
    }

    @Test
    fun `skal returnere flere barn-relasjoner når person har barn med flere motparter`() {
        every { mockPersonConsumer.hentFamilierelasjon(identMedFlereBarn) } returns responsMedFlereBarn

        val resultat = personService.hentFamilierelasjon(identMedFlereBarn)
        assertNotNull(resultat)

        val relasjoner = resultat!!.personensMotpartBarnRelasjon
        assertTrue(relasjoner.size > 1)
    }
}