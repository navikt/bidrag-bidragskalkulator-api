package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FørstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnOver18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnUnder18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdf
import no.nav.bidrag.bidragskalkulator.mapper.normalisert
import no.nav.bidrag.bidragskalkulator.mapper.skalFørstesideGenereres
import no.nav.bidrag.bidragskalkulator.mapper.tilGenererFørstesideRequestDto
import no.nav.bidrag.bidragskalkulator.mapper.tilGenererPrivatAvtalePdfRequest
import no.nav.bidrag.bidragskalkulator.prosessor.PdfProsessor
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Service
class PrivatAvtalePdfService(
    val bidragDokumentConsumer: BidragDokumentProduksjonConsumer,
    val førstesideConsumer: FørstesidegeneratorConsumer,
    val pdfProcessor: PdfProsessor
) {

    @Throws(IOException::class)
    fun genererPrivatAvtalePdf(
        innsenderIdent: String,
        dto: PrivatAvtalePdf
    ): ByteArrayOutputStream {
        val normalisertDto = when (dto) {
            is PrivatAvtaleBarnUnder18RequestDto -> dto.normalisert()
            is PrivatAvtaleBarnOver18RequestDto -> dto.normalisert()
        }

        logger.info { "Generere hoveddokument – kaller bidrag-dokument-produksjon" }
        val hoveddokument = bidragDokumentConsumer
            .genererPrivatAvtaleAPdf(normalisertDto.tilGenererPrivatAvtalePdfRequest())
        hoveddokument.toByteArray()

        val dokumenter = mutableListOf(hoveddokument)

        if(normalisertDto.oppgjør.skalFørstesideGenereres()) {
            logger.info { "Førsteside kreves – kaller førstesidegenerator" }
            val request = normalisertDto.tilGenererFørstesideRequestDto(innsenderIdent)
            val førstesideBytes = førstesideConsumer.genererFørsteside(request).foersteside
            val førstesideStream = ByteArrayOutputStream().apply { write(førstesideBytes) }

            dokumenter.add(0, førstesideStream)
        }

        val sammenslått = pdfProcessor.prosesserOgSlåSammenDokumenter(dokumenter.map { it.toByteArray() })
        return ByteArrayOutputStream().apply { write(sammenslått) }
    }
}
