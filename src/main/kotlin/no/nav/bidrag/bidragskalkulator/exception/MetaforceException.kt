package no.nav.bidrag.bidragskalkulator.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_GATEWAY)
class MetaforceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
