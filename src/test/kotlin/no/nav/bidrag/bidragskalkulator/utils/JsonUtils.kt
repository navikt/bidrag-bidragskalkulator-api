package no.nav.bidrag.bidragskalkulator.utils

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.bidrag.generer.testdata.person.genererAktørid
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.jetbrains.annotations.TestOnly
import java.nio.file.Files
import java.nio.file.Paths

@TestOnly
object JsonUtils {

    inline fun <reified T> lesJsonFil(
        filnavn: String,
        personFnr: String = genererFødselsnummer(),
        personAktørId: String = genererAktørid(),
        motpart1Fnr: String = genererFødselsnummer(),
        motpart1Aktørid: String = genererAktørid(),
        motpart2Fnr: String = genererFødselsnummer(),
        motpart2Aktørid: String = genererAktørid(),
        motpart3Fnr: String = genererFødselsnummer(),
        motpart3Aktørid: String = genererAktørid(),
        barn1Fnr: String = genererFødselsnummer(),
        barn1Aktørid: String = genererAktørid(),
        barn2Fnr: String = genererFødselsnummer(),
        barn2Aktørid: String = genererAktørid(),
        barn3Fnr: String = genererFødselsnummer(),
        barn3Aktørid: String = genererAktørid(),
        barn4Fnr: String = genererFødselsnummer(),
        barn4Aktørid: String = genererAktørid(),

        ): T {
        val path = getFilePath(filnavn)
        val jsonContent = Files.readString(path)
            .replace("{PERSON_FNR}", personFnr)
            .replace("{PERSON_AKTOERID}", personAktørId)
            .replace("{MOTPART1_FNR}", motpart1Fnr)
            .replace("{MOTPART1_AKTOERID}", motpart1Aktørid)
            .replace("{MOTPART2_FNR}", motpart2Fnr)
            .replace("{MOTPART2_AKTOERID}", motpart2Aktørid)
            .replace("{MOTPART3_FNR}", motpart3Fnr)
            .replace("{MOTPART3_AKTOERID}", motpart3Aktørid)
            .replace("{BARN1_FNR}", barn1Fnr)
            .replace("{BARN1_AKTOERID}", barn1Aktørid)
            .replace("{BARN2_FNR}", barn2Fnr)
            .replace("{BARN2_AKTOERID}", barn2Aktørid)
            .replace("{BARN3_FNR}", barn3Fnr)
            .replace("{BARN3_AKTOERID}", barn3Aktørid)
            .replace("{BARN4_FNR}", barn4Fnr)
            .replace("{BARN4_AKTOERID}", barn4Aktørid)
        return commonObjectmapper.readValue(jsonContent)
    }

    fun getFilePath(fileName: String) =
        this::class.java.classLoader.getResource("testdata/$fileName")?.toURI()?.let { Paths.get(it) }
            ?: throw IllegalArgumentException("Filen $fileName ble ikke funnet i testdata-mappen.")
}
