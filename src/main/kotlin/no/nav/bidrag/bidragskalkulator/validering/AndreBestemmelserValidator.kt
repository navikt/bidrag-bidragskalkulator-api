package no.nav.bidrag.bidragskalkulator.validering

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import no.nav.bidrag.bidragskalkulator.dto.AndreBestemmelserSkjema
import kotlin.reflect.KClass

// AndreBestemmelser må ha beskrivelse når harAndreBestemmelser=true
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AndreBestemmelserValidator::class])
annotation class ValidAndreBestemmelser(
    val message: String = "Feltet 'beskrivelse' er påkrevd når 'harAndreBestemmelser' er true.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class AndreBestemmelserValidator : ConstraintValidator<ValidAndreBestemmelser, AndreBestemmelserSkjema> {
    override fun isValid(bestemmelserSkjema: AndreBestemmelserSkjema?, ctx: ConstraintValidatorContext): Boolean {
        if (bestemmelserSkjema == null) {
            return true
        }

        if (bestemmelserSkjema.harAndreBestemmelser && bestemmelserSkjema.beskrivelse.isNullOrBlank()) {
            ctx.disableDefaultConstraintViolation()
            ctx.buildConstraintViolationWithTemplate("beskrivelse må settes når harAndreBestemmelser=true")
                .addPropertyNode("beskrivelse").addConstraintViolation()
            return false
        }

        return true
    }
}
