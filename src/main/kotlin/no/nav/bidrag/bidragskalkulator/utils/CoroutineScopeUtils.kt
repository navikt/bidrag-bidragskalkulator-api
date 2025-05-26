package no.nav.bidrag.bidragskalkulator.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.slf4j.Logger

fun <T> CoroutineScope.asyncCatching(
    logger: Logger,
    navn: String,
    block: suspend CoroutineScope.() -> T
) = async {
    runCatching { block() }
        .onFailure { logger.error("Feil ved henting av $navn", it) }
        .getOrThrow()
}
