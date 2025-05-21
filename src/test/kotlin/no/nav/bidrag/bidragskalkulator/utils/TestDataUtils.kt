package no.nav.bidrag.bidragskalkulator.utils

import no.nav.bidrag.bidragskalkulator.service.BarnUnderholdskostnad
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.jetbrains.annotations.TestOnly
import java.math.BigDecimal

@TestOnly
object TestDataUtils {
    fun lagBarnUnderholdskostnad(motpartBarnRelasjon: MotpartBarnRelasjonDto): List<BarnUnderholdskostnad> =
        motpartBarnRelasjon.personensMotpartBarnRelasjon.flatMap { relasjon ->
            relasjon.fellesBarn.map { barn -> BarnUnderholdskostnad(barn.ident, BigDecimal(5490)) }
        }
}

