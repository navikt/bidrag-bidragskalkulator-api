package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonServiceTest {

    private val bidragPersonConsumer = mockk<BidragPersonConsumer>()

    private lateinit var personService: PersonService

    @BeforeEach
    fun setup() {
        personService = PersonService(bidragPersonConsumer)
    }

    @Test
    fun `skal hente personinformasjon fra personConsumer`() {
        // Arrange
        val personIdent = genererPersonident()
        val forventetPerson = PersonDto(personIdent)

        every { bidragPersonConsumer.hentPerson(personIdent) } returns forventetPerson

        // Act
        val resultat = personService.hentPersoninformasjon(personIdent)

        // Assert
        assertEquals(forventetPerson, resultat)
        verify(exactly = 1) { bidragPersonConsumer.hentPerson(personIdent) }
    }

    @Test
    fun `skal hente og filtrere gyldig familierelasjon`() {
        // Arrange
        val dataMed2Barn1Motpart =
            JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_med_barn_en_motpart.json")
        val personIdent = dataMed2Barn1Motpart.person.ident.verdi
        val relasjon = dataMed2Barn1Motpart.personensMotpartBarnRelasjon.first()

        every { bidragPersonConsumer.hentFamilierelasjon(personIdent) } returns dataMed2Barn1Motpart

        // Act
        val resultat = personService.hentGyldigFamilierelasjon(personIdent)

        // Assert
        assertEquals(dataMed2Barn1Motpart.personensMotpartBarnRelasjon.size, resultat.motpartsrelasjoner.size)

        with(resultat.motpartsrelasjoner.first()) {
            assertEquals(relasjon.motpart, motpart)
            assertEquals(relasjon.fellesBarn, fellesBarn)
            assertEquals(2, fellesBarn.size, "Skal returnere 2 gyldige barn")
        }

        verify(exactly = 1) { bidragPersonConsumer.hentFamilierelasjon(personIdent) }
    }

    @Test
    fun `skal returnere tom liste av motpartsrelasjoner hvis ingen relasjon finnes`() {
        // Arrange
        val personMedNullBarn = JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_ingen_barn.json")
        val personIdent = personMedNullBarn.person.ident.verdi

        every { bidragPersonConsumer.hentFamilierelasjon(personIdent) } returns personMedNullBarn

        // Act
        val resultat = personService.hentGyldigFamilierelasjon(personIdent)

        // Assert
        assertTrue(resultat.motpartsrelasjoner.isEmpty())

        verify(exactly = 1) { bidragPersonConsumer.hentFamilierelasjon(personIdent) }
    }

    @Test
    fun `skal ekskludere relasjoner uten motpart og barn med strengt fortrolig adresse`() {
        // Arrange
        val dataMedBådeSkjermetBarnOgNullMotpart =
            JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_med_baade_barn_strengt_fortrolig_adresse_og_null_motpart.json")
        val personIdent = dataMedBådeSkjermetBarnOgNullMotpart.person.ident.verdi
        val motpartsrelasjoner = dataMedBådeSkjermetBarnOgNullMotpart.personensMotpartBarnRelasjon
        val fellesBarn = dataMedBådeSkjermetBarnOgNullMotpart.personensMotpartBarnRelasjon.flatMap { it.fellesBarn }

        every { bidragPersonConsumer.hentFamilierelasjon(personIdent) } returns dataMedBådeSkjermetBarnOgNullMotpart

        // Act
        val resultat = personService.hentGyldigFamilierelasjon(personIdent)
        val resultatMotpartsrelasjoner = resultat.motpartsrelasjoner
        val resultatFellesBarn = resultat.motpartsrelasjoner.flatMap { it.fellesBarn }

        // Assert
        // Relasjoner
        assertEquals(2, motpartsrelasjoner.size, "Utgangspunktet er 2 relasjoner")
        assertNotEquals(
            resultatMotpartsrelasjoner.size,
            motpartsrelasjoner.size,
            "1 relasjon skal være filtrert bort (motpart er null)"
        )
        assertEquals(1, resultatMotpartsrelasjoner.size, "Kun én gyldig relasjon skal gjenstå")

        // Barn
        assertEquals(4, fellesBarn.size, "Utgangspunktet er 4 barn")
        assertNotEquals(
            resultatFellesBarn.size,
            fellesBarn.size,
            "1 relasjon (motpart er null) og 2 av 3 barn med diskresjonskode i relasjon med registrert motpart skal være filtrert bort "
        )
        assertEquals(1, resultatFellesBarn.size, "Kun ett gyldig barn skal gjenstå")

        verify(exactly = 1) { bidragPersonConsumer.hentFamilierelasjon(personIdent) }
    }

    @Test
    fun `skal ekskludere barn med fortrolig adresse i gyldige relasjoner`() {
        // Arrange
        // et barn med kode SPFO og et barn med kode SPSF
        val dataMed3Barn1StrengtFortroligAdresse =
            JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_med_barn_med_strengt_fortrolig_adresse.json")
        val personIdent = dataMed3Barn1StrengtFortroligAdresse.person.ident.verdi
        val fellesBarn = dataMed3Barn1StrengtFortroligAdresse.personensMotpartBarnRelasjon.first().fellesBarn

        every { bidragPersonConsumer.hentFamilierelasjon(personIdent) } returns dataMed3Barn1StrengtFortroligAdresse

        // Act
        val resultat = personService.hentGyldigFamilierelasjon(personIdent)
        val resultatFellesBarn = resultat.motpartsrelasjoner.first().fellesBarn

        // Assert
        assertEquals(3, fellesBarn.size, "Utgangspunktet er 3 barn")
        assertNotEquals(
            resultatFellesBarn.size,
            fellesBarn.size,
            "2 barn skal være filtrert bort (barn med skjermet adresse)"
        )
        assertEquals(1, resultatFellesBarn.size, "Skal kun inkludere barn uten diskresjonskode")

        verify(exactly = 1) { bidragPersonConsumer.hentFamilierelasjon(personIdent) }
    }

    @Test
    fun `skal ekskludere relasjoner hvor motpart er død`() {
        // Arrange
        val dataMed3Relasjoner1DødMotpart =
            JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_med_doed_motpart.json")
        val personIdent = dataMed3Relasjoner1DødMotpart.person.ident.verdi

        every { bidragPersonConsumer.hentFamilierelasjon(personIdent) } returns dataMed3Relasjoner1DødMotpart

        // Act
        val resultat = personService.hentGyldigFamilierelasjon(personIdent)

        // Assert
        assertEquals(
            3,
            dataMed3Relasjoner1DødMotpart.personensMotpartBarnRelasjon.size,
            "Utgangspunktet er 3 relasjoner"
        )
        assertNotEquals(
            resultat.motpartsrelasjoner.size,
            dataMed3Relasjoner1DødMotpart.personensMotpartBarnRelasjon.size,
            "En død motpart skal være filtrert bort"
        )
        assertEquals(2, resultat.motpartsrelasjoner.size)

        verify(exactly = 1) { bidragPersonConsumer.hentFamilierelasjon(personIdent) }
    }

    @Test
    fun `skal ekskludere døde barn fra relasjonen`() {
        // Arrange
        val dataMed2Barn1DødBarn = JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_med_doede_barn.json")
        val personIdent = dataMed2Barn1DødBarn.person.ident.verdi
        val fellesBarn = dataMed2Barn1DødBarn.personensMotpartBarnRelasjon.first().fellesBarn

        every { bidragPersonConsumer.hentFamilierelasjon(personIdent) } returns dataMed2Barn1DødBarn

        // Act
        val resultat = personService.hentGyldigFamilierelasjon(personIdent)
        val resultatFellesBarn = resultat.motpartsrelasjoner.first().fellesBarn

        // Assert
        assertEquals(2, fellesBarn.size, "Utgangspunktet er 2 barn")
        assertNotEquals(resultatFellesBarn.size, fellesBarn.size, "1 dødt barn skal være filtrert bort")
        assertEquals(1, resultatFellesBarn.size)

        verify(exactly = 1) { bidragPersonConsumer.hentFamilierelasjon(personIdent) }
    }

    @Test
    fun `skal ekskludere relasjon med motpart som har fortrolig adresse`() {
        // Arrange
        val dataMed2Motpart1MotpartFortroligAdresse =
            JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_med_motpart_med_fortrolig_adresse.json")
        val personIdent = dataMed2Motpart1MotpartFortroligAdresse.person.ident.verdi
        val motpartsrelasjoner = dataMed2Motpart1MotpartFortroligAdresse.personensMotpartBarnRelasjon

        every { bidragPersonConsumer.hentFamilierelasjon(personIdent) } returns dataMed2Motpart1MotpartFortroligAdresse

        // Act
        val resultat = personService.hentGyldigFamilierelasjon(personIdent)
        val resultatMotpartsrelasjoner = resultat.motpartsrelasjoner

        // Assert
        assertEquals(
            2,
            dataMed2Motpart1MotpartFortroligAdresse.personensMotpartBarnRelasjon.size,
            "Utgangspunktet er 2 relasjoner"
        )
        assertNotEquals(
            resultatMotpartsrelasjoner.size,
            motpartsrelasjoner.size,
            "1 motpart med diskresjonskode SPSF skal ekskluderes"
        )
        assertEquals(1, resultatMotpartsrelasjoner.size)

        verify(exactly = 1) { bidragPersonConsumer.hentFamilierelasjon(personIdent) }
    }

    @Test
    fun `skal ekskludere relasjoner med uregistrert motpart`() {
        // Arrange
        val personMed2NullMotpart = JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_med_null_motpart.json")
        val personIdent = personMed2NullMotpart.person.ident.verdi

        every { bidragPersonConsumer.hentFamilierelasjon(personIdent) } returns personMed2NullMotpart

        // Act
        val resultat = personService.hentGyldigFamilierelasjon(personIdent)

        // Assert
        assertTrue(resultat.motpartsrelasjoner.isEmpty(), "2 relasjoner uten motpart skal filtreres bort")

        verify(exactly = 1) { bidragPersonConsumer.hentFamilierelasjon(personIdent) }
    }

}
