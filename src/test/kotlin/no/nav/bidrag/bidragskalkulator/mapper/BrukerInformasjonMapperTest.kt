package no.nav.bidrag.bidragskalkulator.mapper

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import no.nav.bidrag.bidragskalkulator.dto.BrukerInfomasjonDto
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrukerInformasjonMapperTest {
    @Test
    fun `skal mappe MotpartBarnRelasjonDto til BrukerInfomasjonDto`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")

        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto)

        assertNotNull(resultat)
        assertNotNull(resultat.paaloggetPerson)
        // Forventet visningsnavn fra testdata
        assertEquals(motpartBarnRelasjonDto.person.visningsnavn, resultat.paaloggetPerson.visningsnavn)
        assertTrue(resultat.barnRelasjon.isNotEmpty(), "Barn-relasjon skal ikke være tom")

        val barnRelasjon = resultat.barnRelasjon.first()
        assertNotNull(barnRelasjon.motpart)
        // Forventet ident for motpart fra testdata
        assertEquals(motpartBarnRelasjonDto.personensMotpartBarnRelasjon.first().motpart?.visningsnavn, barnRelasjon.motpart?.visningsnavn)
        assertTrue(barnRelasjon.fellesBarn.isNotEmpty(), "Felles barn skal ikke være tomt")
        // Forventet ident for barn
        assertEquals(motpartBarnRelasjonDto.personensMotpartBarnRelasjon.first().fellesBarn.first().visningsnavn, barnRelasjon.fellesBarn.first().visningsnavn)
    }

    @Test
    fun `skal håndtere null-motpart på barn-relasjon`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_barn_flere_motpart.json")

        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto)

        assertNotNull(resultat)
        assertTrue(resultat.barnRelasjon.isNotEmpty(), "Barn-relasjon skal ikke være tom")

        val barnRelasjon = resultat.barnRelasjon.first()
        assertNull(barnRelasjon.motpart, "Motpart skal være null i denne testen")
    }

    @Test
    fun `skal håndtere tomt personensMotpartBarnRelasjon`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_ingen_barn.json")

        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto)

        assertNotNull(resultat)
        assertTrue(resultat.barnRelasjon.isEmpty(), "Barn-relasjon skal være tom hvis personensMotpartBarnRelasjon er tomt")
    }
}