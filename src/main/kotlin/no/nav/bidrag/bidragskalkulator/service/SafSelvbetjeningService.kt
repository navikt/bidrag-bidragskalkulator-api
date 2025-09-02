package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.consumer.SafSelvbetjeningConsumer
import no.nav.bidrag.bidragskalkulator.dto.minSide.MinSideDokumenterDto
import no.nav.bidrag.bidragskalkulator.mapper.SafSelvbetjeningMapper
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class SafSelvbetjeningService(
    private val consumer: SafSelvbetjeningConsumer,
    private val safSelvbetjeningMapper: SafSelvbetjeningMapper
) {

    fun hentSelvbetjeningJournalposter(ident: String): MinSideDokumenterDto {
        logger.info { "Henter dokumenter fra SAF for en bruker" }

        return consumer.hentDokumenterForIdent(ident)?.let {
            safSelvbetjeningMapper.mapSafSelvbetjeningRespons(it)
        } ?: MinSideDokumenterDto(emptyList())
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String) =
        consumer.hentDokument(journalpostId, dokumentInfoId, "ARKIV")
}
