package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.SafSelvbetjeningConsumer
import no.nav.bidrag.bidragskalkulator.dto.minSide.MinSideDokumenterDto
import no.nav.bidrag.bidragskalkulator.mapper.SafSelvbetjeningMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SafSelvbetjeningService(
    private val consumer: SafSelvbetjeningConsumer,
    private val safSelvbetjeningMapper: SafSelvbetjeningMapper
) {
    private val logger = LoggerFactory.getLogger(SafSelvbetjeningService::class.java)

    fun hentSelvbetjeningJournalposter(ident: String): MinSideDokumenterDto {
        logger.info("Henter dokumenter fra SAF for en bruker")

        return consumer.hentDokumenterForIdent(ident)?.let {
            safSelvbetjeningMapper.mapSafSelvbetjeningRespons(it)
        } ?: MinSideDokumenterDto(emptyList())
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String) =
        consumer.hentDokument(journalpostId, dokumentInfoId, "ARKIV")
}
