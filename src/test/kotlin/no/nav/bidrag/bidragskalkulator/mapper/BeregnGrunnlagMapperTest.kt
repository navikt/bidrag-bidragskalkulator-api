package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BeregnGrunnlagMapperTest {

    private lateinit var beregnGrunnlagMapper: BeregnGrunnlagMapper

    @BeforeEach
    fun setup() {
        beregnGrunnlagMapper = BeregnGrunnlagMapper()
    }

    @Test
    fun `skal mappe BeregningRequestDto med ett barn til BeregnGrunnlag`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("beregning_et_barn.json")

        val result = beregnGrunnlagMapper.mapToBeregnGrunnlag(beregningRequest)

        assertEquals(1, result.size, "Forventet én beregning")
        assertBarnetsAlderOgReferanse(result.first(), beregningRequest, 0)
    }

    @Test
    fun `skal mappe BeregningRequestDto med to barn til BeregnGrunnlag`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("beregning_to_barn.json")

        val result = beregnGrunnlagMapper.mapToBeregnGrunnlag(beregningRequest)

        assertEquals(2, result.size, "Forventet to beregninger")
        result.forEachIndexed { index, beregnGrunnlagMedAlder ->
            assertBarnetsAlderOgReferanse(beregnGrunnlagMedAlder, beregningRequest, index)
        }
    }

    @Test
    fun `skal ha riktig antall grunnlagselementer`() {
        val beregningRequest: BeregningRequestDto = JsonUtils.readJsonFile("beregning_et_barn.json")

        val result = beregnGrunnlagMapper.mapToBeregnGrunnlag(beregningRequest)

        // barnsreferanse, bidragspliktigsreferanse, bidragsmottakersreferanse, bidragspliktig inntekt,
        // bidragsmottaker inntekt, barn inntekt, samværsklasse, bidragspliktig bostatus, barn bostatus
        assertEquals(9, result.first().beregnGrunnlag.grunnlagListe.size, "Forventet 9 grunnlagselementer")
    }

    private fun assertBarnetsAlderOgReferanse(
        beregnGrunnlagMedAlder: BeregnGrunnlagMedAlder,
        beregningRequest: BeregningRequestDto,
        index: Int
    ) {
        assertEquals(beregningRequest.barn[index].alder, beregnGrunnlagMedAlder.barnetsAlder)
        assertEquals("Person_Søknadsbarn_$index", beregnGrunnlagMedAlder.beregnGrunnlag.søknadsbarnReferanse)
    }
}