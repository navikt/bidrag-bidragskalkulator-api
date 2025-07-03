package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.GenererFoerstesideRequestDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.NavSkjemaId
import no.nav.bidrag.bidragskalkulator.prosessor.PdfProsessor
import no.nav.bidrag.bidragskalkulator.utils.PdfUtils
import no.nav.bidrag.bidragskalkulator.utils.PdfUtils.Companion.convertAllPagesToA4
import no.nav.bidrag.bidragskalkulator.utils.skalerTilA4
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.time.measureTimedValue

@Service
class PrivatAvtalePdfService(
    val bidragDokumentConsumer: BidragDokumentProduksjonConsumer,
    val foerstesideConsumer: FoerstesidegeneratorConsumer,
    val pdfProcessor: PdfProsessor
) {

    private val logger = LoggerFactory.getLogger(PrivatAvtalePdfService::class.java)

    @Throws(IOException::class)
    suspend fun genererPrivatAvtalePdf(
        innsenderIdent: String,
        privatAvtalePdfDto: PrivatAvtalePdfDto
    ): ByteArrayOutputStream {
        logger.info("Genererer privat avtale PDF")

        val pdfOutputStream = measureTimedValue { bidragDokumentConsumer.genererPrivatAvtaleAPdf(privatAvtalePdfDto) }
            .also { logger.info("Privat avtale PDF generert på ${it.duration.inWholeMilliseconds} ms") }
            .value

        val dokumenter = mutableListOf<ByteArray>(pdfOutputStream.toByteArray())
        if (privatAvtalePdfDto.tilInnsending) {
            val foersteside = measureTimedValue { genererForsideForInnsending(innsenderIdent) }
                .also { logger.info("Foersteside PDF generert på ${it.duration.inWholeMilliseconds} ms") }
                .value
            dokumenter.addFirst(foersteside.toByteArray())
        }

        val ferdigDokument = pdfProcessor.prosesserOgSlåSammenDokumenter(dokumenter)

        return ByteArrayOutputStream().apply {
            write(ferdigDokument)
        }
    }

    suspend fun genererForsideForInnsending(navIdent: String): ByteArrayOutputStream = coroutineScope {
        foerstesideConsumer.genererFoersteside(
            GenererFoerstesideRequestDto(
                ident = navIdent,
                navSkjemaId = NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18,
                arkivtittel = "Avtale om barnebidrag",
                enhetsnummer = "1234"
            )
        )
    }


}