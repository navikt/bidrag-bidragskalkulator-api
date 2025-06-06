package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import kotlin.time.measureTimedValue

@Service
class PrivatAvtalePdfService(val consumer: BidragDokumentProduksjonConsumer) {

    private val logger = LoggerFactory.getLogger(PrivatAvtalePdfService::class.java)

    fun genererPrivatAvtalePdf(privatAvtalePdfDto: PrivatAvtalePdfDto): ByteArrayOutputStream {
        logger.info("Genererer privat avtale PDF")

        val pdfOutputStream = measureTimedValue { consumer.genererPrivatAvtaleAPdf(privatAvtalePdfDto) }
            .also { logger.info("Privat avtale PDF generert på ${it.duration.inWholeMilliseconds} ms") }
            .value

        return pdfOutputStream

    }
}