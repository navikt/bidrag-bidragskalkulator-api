package no.nav.bidrag.bidragskalkulator.validering

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import no.nav.bidrag.bidragskalkulator.dto.Oppgjør
import kotlin.reflect.KClass

// Oppgjør-regel: Ved endring må oppgjørsformIdag settes
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [OppgjørValidator::class])
annotation class ValidOppgjør(
    val message: String = "Ugyldig kombinasjon for Oppgjør",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class OppgjørValidator : ConstraintValidator<ValidOppgjør, Oppgjør> {
    override fun isValid(oppgjør: Oppgjør?, ctx: ConstraintValidatorContext): Boolean {
        if (oppgjør == null) {
            return true
        }

        if (!oppgjør.nyAvtale && oppgjør.oppgjørsformIdag == null) {
            ctx.disableDefaultConstraintViolation()
            ctx.buildConstraintViolationWithTemplate("oppgjørsformIdag må settes når nyAvtale=false")
                .addPropertyNode("oppgjørsformIdag").addConstraintViolation()
            return false
        }

        return true
    }
}
