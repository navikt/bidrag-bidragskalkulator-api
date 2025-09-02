package no.nav.bidrag.bidragskalkulator.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

open class BidragKalkulatorException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class GrunnlagNotFoundException(
    message: String,
    cause: Throwable? = null
) : BidragKalkulatorException(message, cause)

class InntektTransformException(
    message: String,
    cause: Throwable? = null
) : BidragKalkulatorException(message, cause)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class UgyldigBeregningRequestException(
    message: String
) : BidragKalkulatorException(message)
