package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.mapper.tilPrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrivatAvtaleServiceTest {

    private val personConsumer = mockk<BidragPersonConsumer>()
    private val service = PrivatAvtaleService(personConsumer)

    @Test
    fun `skal hente informasjon for privat avtale`() {
        val ident = genererPersonident()
        val personDto = JsonUtils.lesJsonFil<MotpartBarnRelasjonDto>("/person/person_med_barn_et_motpart.json")
        val forventetDto = personDto.person.tilPrivatAvtaleInformasjonDto()

        every { personConsumer.hentPerson(ident) } returns personDto.person

        val resultat = service.hentInformasjonForPrivatAvtale(ident.verdi)

        assertEquals(forventetDto, resultat)
    }
}
