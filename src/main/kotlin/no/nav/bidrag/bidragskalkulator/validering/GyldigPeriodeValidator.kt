package no.nav.bidrag.bidragskalkulator.validering

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import no.nav.bidrag.bidragskalkulator.dto.Bidrag
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [GyldigPeriodeValidator::class])
annotation class GyldigPeriode(
    val message: String = "tilDato kan ikke være før fraDato",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class GyldigPeriodeValidator : ConstraintValidator<GyldigPeriode, Bidrag> {
    override fun isValid(bidrag: Bidrag, context: ConstraintValidatorContext): Boolean {
        return bidrag.tilDato >= bidrag.fraDato
    }
}
