package no.nav.bidrag.bidragskalkulator.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.commons.util.RequestContextAsyncContext
import no.nav.bidrag.commons.util.SecurityCoroutineContext
import org.slf4j.Logger

fun <T> CoroutineScope.asyncCatching(
    logger: Logger,
    navn: String,
    block: suspend CoroutineScope.() -> T
) = async(BidragAwareContext) {
    runCatching { block() }
        .onFailure { logger.error("Feil ved henting av $navn", it) }
        .getOrThrow()
}

val BidragAwareContext = Dispatchers.IO + MDCContext() + SecurityCoroutineContext() + RequestContextAsyncContext()