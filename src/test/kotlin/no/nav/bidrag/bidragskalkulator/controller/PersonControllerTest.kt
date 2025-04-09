package no.nav.bidrag.bidragskalkulator.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.bidragskalkulator.config.TestOAuth2TokenProvider.TestData.påloggetPerson
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.enums.person.Kjønn
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjon
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.PersonDto
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class PersonControllerTest: AbstractControllerTest() {

    @MockkBean
    private lateinit var personService: PersonService

    @Test
    fun `skal returnere 200 OK og familierelasjon når person eksisterer`() {
        every { personService.hentFamilierelasjon() } returns mockResponsPersonMedBarn

        getRequest("/api/v1/person/familierelasjon", gyldigOAuth2Token)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.personensMotpartBarnRelasjon").isNotEmpty())
            .andExpect(jsonPath("$.personensMotpartBarnRelasjon[0].motpart.visningsnavn").value(motpart.visningsnavn))
    }

    @Test
    fun `skal returnere 204 No Content når personen ikke finnes`() {
        every { personService.hentFamilierelasjon() } throws NoContentException("Fant ikke person")

        getRequest("/api/v1/person/familierelasjon", gyldigOAuth2Token)
            .andExpect(status().isNoContent)
    }

    @Test
    fun `skal returnere 401 Unauthorized når token mangler`() {
        getRequest("/api/v1/person/familierelasjon", "")
            .andExpect(status().isUnauthorized)
    }

    companion object TestData {
        val motpart = PersonDto(
            ident = Personident("21828597504"),
            navn = "TREGRENSE, NORMAL",
            fornavn = "NORMAL",
            mellomnavn = "",
            etternavn = "TREGRENSE",
            kjønn = Kjønn.MANN,
            fødselsdato = LocalDate.parse("1985-02-21"),
            visningsnavn = "Normal Tregrense")

        val barn1 = PersonDto(
            ident = Personident("20892098426"),
            navn = "TREGRENSE, TRÅDLØS",
            fornavn = "Trådløs",
            mellomnavn = "",
            etternavn = "TREGRENSE",
            kjønn = Kjønn.KVINNE,
            fødselsdato = LocalDate.parse("2020-09-20"),
            visningsnavn = "Trådløs Tregrense")

        val barn2 = PersonDto(
            ident = Personident("29891198289"),
            navn = "TREGRENSE, NØDVENDIG",
            fornavn = "Nødvendig",
            mellomnavn = "",
            etternavn = "TREGRENSE",
            kjønn = Kjønn.KVINNE,
            fødselsdato = LocalDate.parse("2011-09-29"),
            visningsnavn = "Nødvendig Tregrense")

        val mockResponsPersonMedBarn = MotpartBarnRelasjonDto(
            person = påloggetPerson,
            personensMotpartBarnRelasjon = listOf(MotpartBarnRelasjon(
                forelderrolleMotpart = Familierelasjon.FAR,
                motpart = motpart,
                fellesBarn = listOf(barn1, barn2)
            ))
        )
    }
}