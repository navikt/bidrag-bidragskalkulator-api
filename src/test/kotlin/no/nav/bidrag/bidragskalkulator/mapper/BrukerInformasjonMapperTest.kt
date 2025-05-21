package no.nav.bidrag.bidragskalkulator.mapper

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.service.BarnUnderholdskostnad
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.bidragskalkulator.utils.TestDataUtils.lagBarnUnderholdskostnad
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse

import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import kotlin.test.Test
import kotlin.test.assertTrue

open class BrukerInformasjonMapperTest {

    @Test
    fun `skal mappe MotpartBarnRelasjonDto til BrukerInfomasjonDto`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_barn_et_motpart.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")
        val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(motpartBarnRelasjonDto)

        val resultat: BrukerInformasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(
            motpartBarnRelasjonDto,
            underholdkostnad,
            responsInntektsGrunnlag
        )

        assertNotNull(resultat)
        assertNotNull(resultat.person)
        // Forventet fullt navn fra testdata
        assertEquals(motpartBarnRelasjonDto.person.visningsnavn, resultat.person.fulltNavn)
        assertTrue(resultat.barnerelasjoner.isNotEmpty(), "Barn-relasjon skal ikke være tom")

        val barnRelasjon = resultat.barnerelasjoner.first()
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
        val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(motpartBarnRelasjonDto)

        val resultat: BrukerInformasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto, underholdkostnad, responsInntektsGrunnlag)

        assertNotNull(resultat)

        // Forventer at døde barn er filtrert ut
        assertEquals(1, resultat.barnerelasjoner.size)
    }

    @Test
    fun `skal filtere ut døde motparter`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_doed_motpart.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")
        val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(motpartBarnRelasjonDto)

        val resultat: BrukerInformasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto, underholdkostnad, responsInntektsGrunnlag)

        assertNotNull(resultat)

        // Forventer at døde motparter er filtrert ut
        assertEquals(2, resultat.barnerelasjoner.size)
    }

    @Test
    fun `skal filtere ut motparter med fortrolig adresse`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_motpart_med_fortrolig_adresse.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")
        val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(motpartBarnRelasjonDto)

        val resultat: BrukerInformasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto, underholdkostnad, responsInntektsGrunnlag)

        assertNotNull(resultat)

        // Forventer at motparter med fortrolig adresse er filtrert ut
        assertEquals(1, resultat.barnerelasjoner.size)
    }

    @Test
    fun `skal filtere ut barn med strengt fortrolig adresse`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_barn_med_strengt_fortrolig_adresse.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")
        val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(motpartBarnRelasjonDto)

        val resultat: BrukerInformasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto, underholdkostnad, responsInntektsGrunnlag)

        assertNotNull(resultat)

        // Forventer barn med strengt fortrolig adresse er filtrert ut
        assertEquals(1, resultat.barnerelasjoner.get(0).fellesBarn.size)
    }

    @Test
    fun `skal filtere ut motparter hvor alle barn har strengt fortrolig adresse`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_alle_barn_med_strengt_fortrolig_adresse.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")
        val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(motpartBarnRelasjonDto)

        val resultat: BrukerInformasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(motpartBarnRelasjonDto, underholdkostnad, responsInntektsGrunnlag)

        assertNotNull(resultat)

        // Forventer at barnerelasjoner er tom fordi alle felles barn har strengt fortrolig adresse
        assertEquals(0, resultat.barnerelasjoner.size)
    }

    @Test
    fun `skal håndtere tomt personensMotpartBarnRelasjon`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_ingen_barn.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")
        val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(motpartBarnRelasjonDto)

        val resultat: BrukerInformasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(
            motpartBarnRelasjonDto,
            underholdkostnad,
            responsInntektsGrunnlag
        )

        assertNotNull(resultat)
        assertTrue(resultat.barnerelasjoner.isEmpty(), "Barn-relasjon skal være tom hvis personensMotpartBarnRelasjon er tomt")
    }

    @Test
    fun `skal filtrere vekk relasjoner hvor motpart er null`() {
        val motpartBarnRelasjonDto: MotpartBarnRelasjonDto = JsonUtils.readJsonFile("/person/person_med_null_motpart.json")
        val responsInntektsGrunnlag: TransformerInntekterResponse =
            JsonUtils.readJsonFile("/grunnlag/transformer_inntekter_respons.json")
        val underholdkostnad: List<BarnUnderholdskostnad> = lagBarnUnderholdskostnad(motpartBarnRelasjonDto)

        val resultat: BrukerInformasjonDto = BrukerInformasjonMapper.tilBrukerInformasjonDto(
            motpartBarnRelasjonDto,
            underholdkostnad,
            responsInntektsGrunnlag
        )

        assertNotNull(resultat)
        assertTrue(resultat.barnerelasjoner.isEmpty(), "Skal være tom hvis motpart er null")
    }
}