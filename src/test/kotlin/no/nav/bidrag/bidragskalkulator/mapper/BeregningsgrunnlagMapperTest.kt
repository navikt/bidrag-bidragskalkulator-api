package no.nav.bidrag.bidragskalkulator.mapper

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
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
            ident = genererPersonident(),
            fornavn = "Navn",
            visningsnavn = "Navn Navnesen",
        )

        beregningsgrunnlagMapper = BeregningsgrunnlagMapper(mockBeregningsgrunnlagBuilder)
    }

    @Test
    fun `skal mappe BeregningRequestDto med ett barn til BeregnGrunnlag`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertBarnetsAlderOgReferanse(result.first(), beregningRequest, 0)
    }

    @Test
    fun `skal mappe BeregningRequestDto med to barn til BeregnGrunnlag`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_to_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(2, result.size, "Forventet to beregninger")
        result.forEachIndexed { index, beregnGrunnlagMedAlder ->
            assertBarnetsAlderOgReferanse(beregnGrunnlagMedAlder, beregningRequest, index)
        }
    }

    @Test
    fun `skal ha riktig antall grunnlagselementer`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_to_barn.json")

        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)
        // barnsreferanse, bidragspliktigsreferanse, bidragsmottakersreferanse, bidragspliktig inntekt,
        // bidragsmottaker inntekt, samværsklasse, bidragspliktig bostatus, barn bostatus
        assertEquals(8, result.first().grunnlag.grunnlagListe.size, "Forventet 8 grunnlagselementer")
    }

    @Test
    fun `skal sette stønadstype til BIDRAG18AAR for barn over 18`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil(filnavn = "/barnebidrag/beregning_barn_over_18.json", barn1Fnr = genererFødselsnummer(LocalDate.now().minusYears(19)))
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertEquals(Stønadstype.BIDRAG18AAR, result.first().grunnlag.stønadstype, "Stønadstype skal være BIDRAG18AAR for barn over 18")
    }

    @Test
    fun `skal sette stønadstype til BIDRAG for barn under 18`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil(filnavn = "/barnebidrag/beregning_et_barn.json", barn1Fnr = genererFødselsnummer(
            LocalDate.now().minusYears(10)))
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        assertEquals(Stønadstype.BIDRAG, result.first().grunnlag.stønadstype)
    }

    @Test
    fun `skal inkludere faktisk utgift grunnlag når brnetilsynsutgift er satt`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil("/barnebidrag/beregning_et_barn.json")
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)
        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNotNull(faktiskUtgiftGrunnlag, "Forventet grunnlag for faktisk utgift til barnetilsyn")
    }

    @Test
    fun `skal ikke inkludere faktisk utgift grunnlag når brnetilsynsutgift ikke er satt`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.lesJsonFil(filnavn = "/barnebidrag/beregning_barn_over_18.json", barn1Fnr = genererFødselsnummer(LocalDate.now().minusYears(19)))
        val result = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)
        val faktiskUtgiftGrunnlag = result.first().grunnlag.grunnlagListe
            .find { it.type == Grunnlagstype.FAKTISK_UTGIFT_PERIODE }

        assertNull(faktiskUtgiftGrunnlag, "Forventet ikke grunnlag for faktisk utgift til barnetilsyn når barnetilsynsutgift ikke er satt")
    }

    private fun assertBarnetsAlderOgReferanse(
        grunnlagOgBarnInformasjon: PersonBeregningsgrunnlag,
        beregningRequest: BeregningRequestDto,
        index: Int
    ) {
        assertEquals(beregningRequest.barn[index].ident, grunnlagOgBarnInformasjon.ident)
        assertEquals("Person_Søknadsbarn_$index", grunnlagOgBarnInformasjon.grunnlag.søknadsbarnReferanse)
    }
}
