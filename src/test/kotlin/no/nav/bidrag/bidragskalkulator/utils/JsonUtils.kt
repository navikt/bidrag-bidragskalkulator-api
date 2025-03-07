package no.nav.bidrag.bidragskalkulator.utils

import java.nio.file.Files
import java.nio.file.Paths
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.jetbrains.annotations.TestOnly

@TestOnly
object JsonUtils {

    inline fun <reified T> readJsonFile(fileName: String): T {
        val path = getFilePath(fileName)
        val jsonContent = Files.readString(path)
        return commonObjectmapper.readValue(jsonContent)
    }

    fun getFilePath(fileName: String) =
        this::class.java.classLoader.getResource("testdata/$fileName")?.toURI()?.let { Paths.get(it) }
            ?: throw IllegalArgumentException("Filen $fileName ble ikke funnet i testdata-mappen.")
}