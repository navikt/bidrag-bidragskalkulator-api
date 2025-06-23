package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.coroutineScope
import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import kotlin.time.measureTimedValue

@Service
class PrivatAvtalePdfService(val bidragDokumentConsumer: BidragDokumentProduksjonConsumer, val foerstesideConsumer: FoerstesidegeneratorConsumer) {

    private val logger = LoggerFactory.getLogger(PrivatAvtalePdfService::class.java)

    fun genererPrivatAvtalePdf(privatAvtalePdfDto: PrivatAvtalePdfDto): ByteArrayOutputStream {
        logger.info("Genererer privat avtale PDF")

        val pdfOutputStream = measureTimedValue { bidragDokumentConsumer.genererPrivatAvtaleAPdf(privatAvtalePdfDto) }
            .also { logger.info("Privat avtale PDF generert på ${it.duration.inWholeMilliseconds} ms") }
            .value

        return pdfOutputStream

    }

    suspend fun genererForsideForInnsending(): ByteArrayOutputStream = coroutineScope {
        foerstesideConsumer.genererFoersteside()
    }

}