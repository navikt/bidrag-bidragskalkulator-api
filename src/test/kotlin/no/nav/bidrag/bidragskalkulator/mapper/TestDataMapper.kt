package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.dto.BarneRelasjonDto
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.jetbrains.annotations.TestOnly
import java.math.BigDecimal

@TestOnly
fun MotpartBarnRelasjonDto.mockBarnRelasjonMedUnderholdskostnad(): List<BarneRelasjonDto> =
    this.personensMotpartBarnRelasjon.mapNotNull { relasjon ->
        val motpart = relasjon.motpart ?: return@mapNotNull null

        val fellesBarn = relasjon.fellesBarn.map { it.tilBarnInformasjonDto(BigDecimal(5490)) }
        BarneRelasjonDto(motpart = motpart.tilPersonInformasjonDto(), fellesBarn = fellesBarn)
    }