package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnOver18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnUnder18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdf
import no.nav.bidrag.bidragskalkulator.mapper.skalFørstesideGenereres
import no.nav.bidrag.bidragskalkulator.mapper.tilGenererFoerstesideRequestDto
import no.nav.bidrag.bidragskalkulator.prosessor.PdfProsessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.time.measureTimedValue

@Service
class PrivatAvtalePdfService(
    val bidragDokumentConsumer: BidragDokumentProduksjonConsumer,
    val foerstesideConsumer: FoerstesidegeneratorConsumer,
    val pdfProcessor: PdfProsessor
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Throws(IOException::class)
    fun genererPrivatAvtalePdf(
        innsenderIdent: String,
        dto: PrivatAvtalePdf
    ): ByteArrayOutputStream {
        val (label, dto) = when (dto) {
            is PrivatAvtaleBarnUnder18RequestDto -> "under 18 år" to dto.medNorskeDatoer()
            is PrivatAvtaleBarnOver18RequestDto -> "over 18 år" to dto
        }

        logger.info("Privat avtale for barn $label: Starter generering av PDF for privat avtale")

        val hovedDokument = measureTimedValue { bidragDokumentConsumer
            .genererPrivatAvtaleAPdf(dto) }
            .also { logger
                .info("Privat avtale for barn $label: Hoveddokument generert på ${it.duration.inWholeMilliseconds} ms") }
            .value.toByteArray()

        val dokumenter = mutableListOf(hovedDokument)

        if(dto.oppgjør.skalFørstesideGenereres()) {
            val request = dto.tilGenererFoerstesideRequestDto(innsenderIdent)
            val førsteside = measureTimedValue { foerstesideConsumer.genererFoersteside(request).foersteside }
                .also { logger
                    .info("Privat avtale for barn $label: Førsteside generert på ${it.duration.inWholeMilliseconds} ms") }
                .value
            
              dokumenter.add(0, førsteside)
        }

        val sammenslaatt = pdfProcessor.prosesserOgSlåSammenDokumenter(dokumenter)
        
        return ByteArrayOutputStream().apply {
            write(sammenslaatt)
        }
    }
}
