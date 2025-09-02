package no.nav.bidrag.bidragskalkulator.utils

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.commons.util.RequestContextAsyncContext
import no.nav.bidrag.commons.util.SecurityCoroutineContext
import no.nav.bidrag.commons.util.secureLogger

fun <T> CoroutineScope.asyncCatching(
    logger: KLogger,
    navn: String,
    block: suspend CoroutineScope.() -> T
) = async(BidragAwareContext) {
    runCatching { block() }
        .onFailure { e ->
            logger.error("Feil ved henting av $navn")
            secureLogger.error(e) { "Kall i $navn feilet: ${e.message}" }
        }
        .getOrThrow()
}

val BidragAwareContext = Dispatchers.IO + MDCContext() + SecurityCoroutineContext() + RequestContextAsyncContext()
