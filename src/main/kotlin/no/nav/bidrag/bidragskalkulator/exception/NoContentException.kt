package no.nav.bidrag.bidragskalkulator.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NO_CONTENT)
class NoContentException(message: String) : RuntimeException(message)