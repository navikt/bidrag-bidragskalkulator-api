package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.dto.Barn
import no.nav.bidrag.bidragskalkulator.dto.Bidragsmottaker
import no.nav.bidrag.bidragskalkulator.dto.Bidragspliktig
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import kotlin.time.measureTimedValue

@Service
class PrivatAvtalePdfService(val consumer: BidragDokumentProduksjonConsumer) {

    private val logger = LoggerFactory.getLogger(PrivatAvtalePdfService::class.java)

    fun genererPrivatAvtalePdf(): ByteArrayOutputStream {
        logger.info("Genererer privat avtale PDF")

        val privatAvtaleDto = PrivatAvtalePdfDto(
            innhold = "Dette er en mock",
            bidragsmottaker = Bidragsmottaker("Far", "Far", "12345678901"),
            bidragspliktig = Bidragspliktig("Mor", "Mor", "10987654321"),
            barn = Barn("Barn", "Ebarn", "12345678902", 1000.0),
            fraDato = "01-01-2025",
            nyAvtale = true,
            oppgjorsform = "Bankkonto"
        )

        val pdfOutputStream = measureTimedValue { consumer.genererPrivatAvtaleAPdf(privatAvtaleDto) }
            .also { logger.info("Privat avtale PDF generert på ${it.duration.inWholeMilliseconds} ms") }
            .value

        return pdfOutputStream

    }
}