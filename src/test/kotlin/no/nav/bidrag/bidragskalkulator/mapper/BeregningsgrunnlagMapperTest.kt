package no.nav.bidrag.bidragskalkulator.mapper

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class BeregningsgrunnlagMapperTest {

    @MockK(relaxed = true)
    lateinit var mockPersonService: PersonService

    // Bruk ekte builder
    private val mockBeregningsgrunnlagBuilder = BeregningsgrunnlagBuilder()

    private lateinit var beregningsgrunnlagMapper: BeregningsgrunnlagMapper

    @BeforeEach
    fun setup() {
        val fødselsdato = LocalDate.now().minusYears(10)

        every { mockPersonService.hentPersoninformasjon(any()) } returns PersonDto(
            fødselsdato = fødselsdato,
            ident = Personident("06451759610"),
            fornavn = "Navn",
            visningsnavn = "Navn Navnesen",
        )

        beregningsgrunnlagMapper = BeregningsgrunnlagMapper(mockPersonService, mockBeregningsgrunnlagBuilder)
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
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertEquals(Stønadstype.BIDRAG18AAR, result.first().grunnlag.stønadstype, "Stønadstype skal være BIDRAG18AAR for barn over 18")
    }

    @Test
    fun `skal sette stønadstype til BIDRAG for barn under 18`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(Stønadstype.BIDRAG, result.first().grunnlag.stønadstype)
    }

    @Test
    fun `skal ikke opprettes grunnlag for barn i samme husstand dersom personen ikke har barn som bor fast hos seg`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("/barnebidrag/beregning_et_barn.json")
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)
        val harBarnBorFast = result.first().grunnlag.grunnlagListe
            .filter { it.type == Grunnlagstype.BOSTATUS_PERIODE }.map { it.referanse }.contains("Bostatus_med_forelder")

        assertFalse(harBarnBorFast)
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