import jakarta.validation.Validation
import jakarta.validation.Validator
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.bidragskalkulator.dto.BarnMedIdentDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.fasterxml.jackson.databind.JsonMappingException
import no.nav.bidrag.bidragskalkulator.dto.ForelderInntektDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.BarnMedAlderDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.generer.testdata.person.genererPersonident
import java.math.BigDecimal

class BeregningServiceValidationTest {

    private lateinit var validator: Validator
    private lateinit var objectMapper: ObjectMapper
    private val personIdent = genererPersonident()

    @BeforeEach
    fun setup() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
        objectMapper = ObjectMapper().registerKotlinModule()
    }

    @Test
    fun `skal returnere valideringsfeil dersom bidragstype ikke er satt`() {
        val request = ÅpenBeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("500000")),
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("600000")),
            bidragstype = BidragsType.PLIKTIG,
            barn = listOf(
                BarnMedAlderDto(
                    alder = 1,
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1,
                )
            )
        )

        // First verify that a valid request passes validation
        assertTrue(validator.validate(request).isEmpty())

        // Now create an invalid JSON request missing bidragstype
        val jsonRequest = """
            {
                "bidragsmottakerInntekt": {
                    "inntekt": 500000.0
                },
                "bidragspliktigInntekt": {
                    "inntekt": 600000.0
                },
                "barn": [
                    {
                        "alder": 1,
                        "samværsklasse": "SAMVÆRSKLASSE_1"
                    }
                ]
            }
        """.trimIndent()

        val exception = assertThrows<JsonMappingException> {
            objectMapper.readValue<ÅpenBeregningRequestDto>(jsonRequest)
        }
        assertTrue(exception.message?.contains("bidragstype") == true, "Expected error message to mention 'bidragstype' but was: ${exception.message}")
    }

    @Test
    fun `skal returnere valideringsfeil dersom barnelisten er tom`() {
        val request = BeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("500000")),
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("600000")),
            bidragstype = BidragsType.PLIKTIG,
            barn = emptyList()
        )

        val violations = validator.validate(request)
        assertTrue(violations.any { it.message.contains("Liste over barn kan ikke være tom") })
    }

    @Test
    fun `skal returnere valideringsfeil dersom inntekt er negativ`() {
        val request = BeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("-500000")),
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("600000")),
            bidragstype = BidragsType.PLIKTIG,
            barn = listOf(BarnMedIdentDto(personIdent, Samværsklasse.SAMVÆRSKLASSE_1))
        )

        val violations = validator.validate(request)

        assertTrue(
            violations.any {
                it.propertyPath.toString() == "bidragsmottakerInntekt.inntekt" &&
                        it.message.contains("kan ikke være negativ")
            },
            "Forventet valideringsfeil på bidragsmottakerInntekt.inntekt"
        )
    }

    @Test
    fun `skal håndtere ekstremt høye inntekter korrekt`() {
        val request = BeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(inntekt = BigDecimal("1000000000")), // 1 milliard
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("900000000")), // 900 millioner
            bidragstype = BidragsType.PLIKTIG,
            barn = listOf(BarnMedIdentDto(personIdent, Samværsklasse.SAMVÆRSKLASSE_1))
        )

        val violations = validator.validate(request)
        assertTrue(violations.isEmpty(), "Forventet ingen valideringsfeil for høye inntekter")
    }

    @Test
    fun `skal returnere valideringsfeil dersom nettoPositivKapitalinntekt er negativ`() {
        val request = BeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(
                inntekt = BigDecimal("500000"),
                nettoPositivKapitalinntekt = BigDecimal("-1")
            ),
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("600000")),
            bidragstype = BidragsType.PLIKTIG,
            barn = listOf(BarnMedIdentDto(personIdent, Samværsklasse.SAMVÆRSKLASSE_1))
        )

        val violations = validator.validate(request)

        assertTrue(
            violations.any {
                it.propertyPath.toString() == "bidragsmottakerInntekt.nettoPositivKapitalinntekt" &&
                        it.message.contains("kan ikke være negativ")
            },
            "Forventet valideringsfeil på bidragsmottakerInntekt.nettoPositivKapitalinntekt"
        )
    }

    @Test
    fun `skal returnere valideringsfeil dersom nettoPositivKapitalinntekt har mer enn 2 desimaler`() {
        val request = BeregningRequestDto(
            bidragsmottakerInntekt = ForelderInntektDto(
                inntekt = BigDecimal("500000"),
                nettoPositivKapitalinntekt = BigDecimal("10000.123") // 3 desimaler
            ),
            bidragspliktigInntekt = ForelderInntektDto(inntekt = BigDecimal("600000")),
            bidragstype = BidragsType.PLIKTIG,
            barn = listOf(BarnMedIdentDto(personIdent, Samværsklasse.SAMVÆRSKLASSE_1))
        )

        val violations = validator.validate(request)

        assertTrue(
            violations.any {
                it.propertyPath.toString() == "bidragsmottakerInntekt.nettoPositivKapitalinntekt" &&
                        it.message.contains("maks 2 desimaler")
            },
            "Forventet valideringsfeil på bidragsmottakerInntekt.nettoPositivKapitalinntekt (desimaler)"
        )
    }

    @Test
    fun `skal defaulte nettoPositivKapitalinntekt til 0 når feltet ikke er oppgitt i request`() {
        val jsonRequest = """
        {
          "bidragsmottakerInntekt": { "inntekt": 500000.00 },
          "bidragspliktigInntekt": { "inntekt": 600000.00 },
          "bidragstype": "MOTTAKER",
          "barn": [
            {
              "alder": 1,
              "samværsklasse": "SAMVÆRSKLASSE_1"
            }
          ]
        }
    """.trimIndent()

        val dto = objectMapper.readValue<ÅpenBeregningRequestDto>(jsonRequest)

        assertEquals(
            BigDecimal.ZERO,
            dto.bidragsmottakerInntekt.nettoPositivKapitalinntekt,
            "Forventet at nettoPositivKapitalinntekt defaultes til 0 når feltet ikke er oppgitt"
        )

        assertTrue(validator.validate(dto).isEmpty())
    }

}
