package no.nav.bidrag.bidragskalkulator.mapper

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import no.nav.bidrag.bidragskalkulator.dto.BrukerInfomasjonDto
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse

import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

open class BrukerInformasjonMapperTest {


    @Test
    fun `skal mappe MotpartBarnRelasjonDto til BrukerInfomasjonDto`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")

        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(
            motpartBarnRelasjonDto,
            responsInntektsGrunnlag
        )

        assertNotNull(resultat)
        assertNotNull(resultat.påloggetBruker)
        // Forventet fullt navn fra testdata
        assertEquals(motpartBarnRelasjonDto.person.visningsnavn, resultat.påloggetBruker.fulltNavn)
        assertTrue(resultat.relasjoner.barneRelasjoner.isNotEmpty(), "Barn-relasjon skal ikke være tom")

        val barnRelasjon = resultat.relasjoner.barneRelasjoner.first()
        assertNotNull(barnRelasjon.motpart)
        // Forventet fullt navn for motpart fra testdata
        assertEquals(motpartBarnRelasjonDto.personensMotpartBarnRelasjon.first().motpart?.visningsnavn, barnRelasjon.motpart?.fulltNavn)
        assertTrue(barnRelasjon.fellesBarn.isNotEmpty(), "Felles barn skal ikke være tomt")
        // Forventet fullt navn for barn
        assertEquals(motpartBarnRelasjonDto.personensMotpartBarnRelasjon.first().fellesBarn.first().visningsnavn, barnRelasjon.fellesBarn.first().fulltNavn)
    }

    @Test
    fun `skal filtere ut døde barn`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_doede_barn.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")


        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto, responsInntektsGrunnlag)

        assertNotNull(resultat)

        // Forventer at døde barn er filtrert ut
        assertEquals(1, resultat.relasjoner.barneRelasjoner.size)
    }

    @Test
    fun `skal filtere ut døde motparter`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_doed_motpart.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")

        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto, responsInntektsGrunnlag)

        assertNotNull(resultat)

        // Forventer at døde motparter er filtrert ut
        assertEquals(2, resultat.relasjoner.barneRelasjoner.size)
    }

    @Test
    fun `skal filtere ut barn med strengt fortrolig adresse`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_barn_med_strengt_fortrolig_adresse.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")

        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto, responsInntektsGrunnlag)

        assertNotNull(resultat)

        // Forventer at døde motparter er filtrert ut
        assertEquals(0, resultat.relasjoner.barneRelasjoner.get(0).fellesBarn.size)
    }

    @Test
    fun `skal håndtere null-motpart på barn-relasjon`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_barn_flere_motpart.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")


        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(
            motpartBarnRelasjonDto,
            responsInntektsGrunnlag
        )

        assertNotNull(resultat)
        assertTrue(resultat.relasjoner.barneRelasjoner.isNotEmpty(), "Barn-relasjon skal ikke være tom")

        val barnRelasjon = resultat.relasjoner.barneRelasjoner.first()
        assertNull(barnRelasjon.motpart, "Motpart skal være null i denne testen")
    }

    @Test
    fun `skal håndtere tomt personensMotpartBarnRelasjon`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_ingen_barn.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")

        val resultat: BrukerInfomasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(
            motpartBarnRelasjonDto,
            responsInntektsGrunnlag
        )

        assertNotNull(resultat)
        assertTrue(resultat.relasjoner.barneRelasjoner.isEmpty(), "Barn-relasjon skal være tom hvis personensMotpartBarnRelasjon er tomt")
    }
}