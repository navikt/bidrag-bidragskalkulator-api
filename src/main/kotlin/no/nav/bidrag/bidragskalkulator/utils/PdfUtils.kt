package no.nav.bidrag.bidragskalkulator.utils

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.util.Matrix
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class PdfUtils {

    companion object {
        fun fileToByte(file: File): ByteArray {
            val inputStream = FileInputStream(file)
            val byteArray = ByteArray(file.length().toInt())
            inputStream.read(byteArray)
            inputStream.close()
            return byteArray
        }


        fun convertAllPagesToA4(file: File) {
            val pdd = PDDocument.load(file)
            pdd.pages.forEach { page -> page.mediaBox = PDRectangle.A4 }
            pdd.save(file)
        }
    }


}

@Throws(IOException::class)
fun PDDocument.skalerTilA4() {
    this.pages.forEach { page ->
        if (page.mediaBox.width <= PDRectangle.A4.width && page.mediaBox.height <= PDRectangle.A4.height) {
            // No scaling needed, pagealready fits within A4 dimensions
            return@forEach
        }
        val matrix = Matrix()
        val xScale = PDRectangle.A4.width / page.mediaBox.width
        val yScale = PDRectangle.A4.height / page.mediaBox.height
        matrix.scale(xScale, yScale)

        PDPageContentStream(this, page, PDPageContentStream.AppendMode.PREPEND, true).use {
            it.transform(matrix)
        }
        page.mediaBox = PDRectangle.A4
        page.cropBox = PDRectangle.A4
    }
}