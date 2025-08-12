package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.bidragskalkulator.consumer.FoerstesidegeneratorConsumer
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtalePdfDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.GenererFoerstesideRequestDto
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.NavSkjemaId
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.Språkkode
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
        privatAvtalePdfDto: PrivatAvtalePdfDto
    ): ByteArrayOutputStream {
        logger.info("Starter generering av PDF for privat avtale")

        val hoveddokument = measureTimedValue {
            bidragDokumentConsumer.genererPrivatAvtaleAPdf(privatAvtalePdfDto.tilNorskDatoFormat())
        }.also {
            logger.info("Hoveddokument generert på ${it.duration.inWholeMilliseconds} ms")
        }.value

        val dokumenter = mutableListOf(hoveddokument.toByteArray())

        if (privatAvtalePdfDto.tilInnsending) {
            val foersteside = measureTimedValue {
                genererForsideForInnsending(innsenderIdent, privatAvtalePdfDto.språk)
            }.also {
                logger.info("Førsteside generert på ${it.duration.inWholeMilliseconds} ms")
            }.value

            dokumenter.add(0, foersteside)
        }

        val sammenslaatt = pdfProcessor.prosesserOgSlåSammenDokumenter(dokumenter)

        return ByteArrayOutputStream().apply {
            write(sammenslaatt)
        }
    }

    fun genererForsideForInnsending(navIdent: String, språk: Språkkode): ByteArray =
        foerstesideConsumer.genererFoersteside(
            GenererFoerstesideRequestDto(
                ident = navIdent,
                navSkjemaId = NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18,
                arkivtittel = "Avtale om barnebidrag",
                enhetsnummer = "1234",
                språkkode = språk
            )
        ).foersteside

}
