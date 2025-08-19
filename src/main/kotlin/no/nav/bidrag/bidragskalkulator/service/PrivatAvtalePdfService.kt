package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnOver18RequestDto
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleBarnUnder18RequestDto
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
    fun genererPrivatAvtalePdfForBarnUnder18(
        innsenderIdent: String,
        dto: PrivatAvtaleBarnUnder18RequestDto
    ): ByteArrayOutputStream {
        logger.info("Starter generering av PDF for privat avtale")

        val hoveddokument = measureTimedValue {
            bidragDokumentConsumer.genererPrivatAvtaleAPdf(dto.medNorskeDatoer())
        }.also {
            logger.info("Hoveddokument generert på ${it.duration.inWholeMilliseconds} ms")
        }.value

        val dokumenter = mutableListOf(hoveddokument.toByteArray())

        if (dto.oppgjør.skalFørstesideGenereres()) {
            val førsteside = measureTimedValue {
                val genererFørstesideRequestDto = dto.tilGenererFoerstesideRequestDto(innsenderIdent)
                foerstesideConsumer.genererFoersteside(genererFørstesideRequestDto).foersteside
            }.also {
                logger.info("Førsteside generert på ${it.duration.inWholeMilliseconds} ms")
            }.value

            dokumenter.add(0, førsteside)
        }

        val sammenslaatt = pdfProcessor.prosesserOgSlåSammenDokumenter(dokumenter)

        return ByteArrayOutputStream().apply {
            write(sammenslaatt)
        }
    }

    @Throws(IOException::class)
    fun genererPrivatAvtalePdfForBarnOver18(
        innsenderIdent: String,
        dto: PrivatAvtaleBarnOver18RequestDto
    ): ByteArrayOutputStream {
        logger.info("Privat avtale for barn over 28år: Starter generering av PDF for privat avtale")

        val hoveddokument = measureTimedValue {
            bidragDokumentConsumer.genererPrivatAvtaleAPdf(dto)
        }.also {
            logger.info("Privat avtale for barn over 28år: Hoveddokument generert på ${it.duration.inWholeMilliseconds} ms")
        }.value

        val dokumenter = mutableListOf(hoveddokument.toByteArray())

        if (dto.oppgjør.skalFørstesideGenereres()) {
            val førsteside = measureTimedValue {
                val genererFørstesideRequestDto = dto.tilGenererFoerstesideRequestDto(innsenderIdent)
                foerstesideConsumer.genererFoersteside(genererFørstesideRequestDto).foersteside
            }.also {
                logger.info("Privat avtale for barn over 28år: Førsteside generert på ${it.duration.inWholeMilliseconds} ms")
            }.value

            dokumenter.add(0, førsteside)
        }

        val sammenslaatt = pdfProcessor.prosesserOgSlåSammenDokumenter(dokumenter)

        return ByteArrayOutputStream().apply {
            write(sammenslaatt)
        }
    }

}
