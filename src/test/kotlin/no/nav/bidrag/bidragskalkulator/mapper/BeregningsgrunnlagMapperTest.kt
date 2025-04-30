package no.nav.bidrag.bidragskalkulator.mapper

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.bidragskalkulator.utils.kalkulereAlder
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.person.NavnFødselDødDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsgrunnlagMapperTest {

    private lateinit var beregningsgrunnlagMapper: BeregningsgrunnlagMapper

    @BeforeEach
    fun setup() {
        val mockPersonService = mockk<PersonService>(relaxed = true)
        val fødselsdato = LocalDate.now().minusYears(10)

        every { mockPersonService.hentNavnFoedselDoed(any()) } returns NavnFødselDødDto(
            navn = "Navn Navnesen",
            fødselsdato = fødselsdato,
            fødselsår = fødselsdato.year,
            dødsdato = null,
            foedselsdato = fødselsdato,
            foedselsaar = fødselsdato.year,
            doedsdato = null
        )

        beregningsgrunnlagMapper = BeregningsgrunnlagMapper(mockPersonService)
    }

    @Test
    fun `skal mappe BeregningRequestDto med ett barn til BeregnGrunnlag`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertBarnetsAlderOgReferanse(result.first(), beregningRequest, 0)
    }

    @Test
    fun `skal mappe BeregningRequestDto med to barn til BeregnGrunnlag`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_to_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(2, result.size, "Forventet to beregninger")
        result.forEachIndexed { index, beregnGrunnlagMedAlder ->
            assertBarnetsAlderOgReferanse(beregnGrunnlagMedAlder, beregningRequest, index)
        }
    }

    @Test
    fun `skal ha riktig antall grunnlagselementer`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        // barnsreferanse, bidragspliktigsreferanse, bidragsmottakersreferanse, bidragspliktig inntekt,
        // bidragsmottaker inntekt, barn inntekt, samværsklasse, bidragspliktig bostatus, barn bostatus
        assertEquals(9, result.first().grunnlag.grunnlagListe.size, "Forventet 9 grunnlagselementer")
    }

    @Test
    fun `skal sette stønadstype til BIDRAG18AAR for barn over 18`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_barn_over_18.json")
        println("---test-----" + kalkulereAlder(beregningRequest.barn.first().ident.fødselsdato()))
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertEquals(Stønadstype.BIDRAG18AAR, result.first().grunnlag.stønadstype, "Stønadstype skal være BIDRAG18AAR for barn over 18")
    }

    private fun assertBarnetsAlderOgReferanse(
        grunnlagOgBarnInformasjon: GrunnlagOgBarnInformasjon,
        beregningRequest: BeregningRequestDto,
        index: Int
    ) {
        assertEquals(beregningRequest.barn[index].ident, grunnlagOgBarnInformasjon.ident)
        assertEquals("Person_Søknadsbarn_$index", grunnlagOgBarnInformasjon.grunnlag.søknadsbarnReferanse)
    }
}