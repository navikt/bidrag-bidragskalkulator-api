import jakarta.validation.Validation
import jakarta.validation.Validator
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.bidragskalkulator.dto.BarnDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.fasterxml.jackson.databind.JsonMappingException
import no.nav.bidrag.domene.ident.Personident

class BeregningServiceValidationTest {

    private lateinit var validator: Validator
    private lateinit var objectMapper: ObjectMapper
    private val personIdent = "12345678910"

    @BeforeEach
    fun setup() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
        objectMapper = ObjectMapper().registerKotlinModule()
    }

    @Test
    fun `skal returnere valideringsfeil dersom bidragstype ikke er satt`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 600000.0,
            barn = listOf(
                BarnDto(
                    ident = Personident(personIdent),
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1,
                    bidragstype = BidragsType.PLIKTIG
                )
            )
        )

        // First verify that a valid request passes validation
        assertTrue(validator.validate(request).isEmpty())

        // Now create an invalid JSON request missing bidragstype
        val jsonRequest = """
            {
                "inntektForelder1": 500000.0,
                "inntektForelder2": 600000.0,
                "barn": [
                    {
                        "ident": "14429546002",
                        "samværsklasse": "SAMVÆRSKLASSE_1"
                    }
                ]
            }
        """.trimIndent()

        val exception = assertThrows<JsonMappingException> {
            objectMapper.readValue<BeregningRequestDto>(jsonRequest)
        }
        assertTrue(exception.message?.contains("bidragstype") == true, "Expected error message to mention 'bidragstype' but was: ${exception.message}")
    }

    @Test
    fun `skal returnere valideringsfeil dersom barnelisten er tom`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 500000.0,
            inntektForelder2 = 600000.0,
            barn = emptyList()
        )

        val violations = validator.validate(request)
        assertTrue(violations.any { it.message.contains("Liste over barn kan ikke være tom") })
    }

    @Test
    fun `skal returnere valideringsfeil dersom inntekt er negativ`() {
        val request = BeregningRequestDto(
            inntektForelder1 = -50000.0, // Ugyldig
            inntektForelder2 = 600000.0,
            barn = listOf(BarnDto(Personident(personIdent), Samværsklasse.SAMVÆRSKLASSE_1, BidragsType.PLIKTIG))
        )

        val violations = validator.validate(request)
        assertTrue(violations.any { it.message.contains("Inntekt for forelder 1 kan ikke være negativ") })
    }

    @Test
    fun `skal håndtere ekstremt høye inntekter korrekt`() {
        val request = BeregningRequestDto(
            inntektForelder1 = 1_000_000_000.0,  // 1 milliard
            inntektForelder2 = 900_000_000.0,  // 900 millioner
            barn = listOf(BarnDto(Personident(personIdent), Samværsklasse.SAMVÆRSKLASSE_1, BidragsType.MOTTAKER))
        )

        val violations = validator.validate(request)
        assertTrue(violations.isEmpty(), "Forventet ingen valideringsfeil for høye inntekter")
    }
}
