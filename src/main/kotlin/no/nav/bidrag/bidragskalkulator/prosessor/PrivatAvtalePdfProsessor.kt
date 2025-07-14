package no.nav.bidrag.bidragskalkulator.prosessor

import no.nav.bidrag.bidragskalkulator.utils.PdfUtils
import no.nav.bidrag.bidragskalkulator.utils.PdfUtils.Companion.convertAllPagesToA4
import no.nav.bidrag.bidragskalkulator.utils.skalerTilA4
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.UUID

interface PdfProsessor {
    fun konverterPdfTilA4(pdf:  ByteArray): ByteArray?
    fun prosesserOgSlåSammenDokumenter(dokumentBytes: List<ByteArray>): ByteArray
}

class PrivatAvtalePdfProsessor : PdfProsessor {

    val logger = LoggerFactory.getLogger(PrivatAvtalePdfProsessor::class.java)

    override fun konverterPdfTilA4(pdf: ByteArray): ByteArray? {
        return prosesserOgSlåSammenDokumenter(listOf(pdf))
    }


    @Throws(IOException::class)
    override fun prosesserOgSlåSammenDokumenter(
        dokumentBytes: List<ByteArray>,
    ): ByteArray {
        val random = UUID.randomUUID().toString().take(8)
        val filename = "/tmp/privat-avtale-${random}.pdf"

        val tempFiles = mutableListOf<File>()

        val pdfMerger = PDFMergerUtility()
        pdfMerger.destinationFileName = filename
        pdfMerger.documentMergeMode = PDFMergerUtility.DocumentMergeMode.OPTIMIZE_RESOURCES_MODE

        try {
            for (dokument in dokumentBytes) {
                val tempFile = File.createTempFile("/tmp/${UUID.randomUUID()}", null)
                val pdd = PDDocument.load(dokument)
                pdd.skalerTilA4()
                pdd.save(tempFile)
                tempFiles.add(tempFile)
                pdfMerger.addSource(tempFile)
            }

            // TODO: temp file only på sikt. Mulig in memory fører til OOM feil
            pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

        } catch (e: Exception) {
            logger.error("Feil ved sammenslåing av PDF-filer: ${e.message}", e)
            throw e
        } finally {
            tempFiles.forEach { it.delete() }
        }

        convertAllPagesToA4(File(filename))

        return getByteDataAndDeleteFile(filename)

    }

    private fun getByteDataAndDeleteFile(filename: String): ByteArray {
        val file = File(filename)
        return try {
            PdfUtils.fileToByte(file)
        } finally {
            file.delete()
        }
    }
}