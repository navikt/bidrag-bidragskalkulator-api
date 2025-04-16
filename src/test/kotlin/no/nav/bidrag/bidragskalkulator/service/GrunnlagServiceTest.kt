package no.nav.bidrag.bidragskalkulator.service

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils.readJsonFile
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.inntekt.InntektApi
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class GrunnlagServiceTest {

    private lateinit var service: GrunnlagService
    private var mockConsumer:BidragGrunnlagConsumer = mockk()
    private var mockInntektApi: InntektApi = mockk()

    private val bidragGrunnlagRespons: HentGrunnlagDto by lazy {
        readJsonFile("/grunnlag/hent_grunnlag_respons.json")
    }

    private val transformerInntekterRespons: TransformerInntekterResponse by lazy {
        readJsonFile("/grunnlag/transformer_inntekter_respons.json")
    }

    @BeforeEach
    fun setup() {
        service = GrunnlagService(mockConsumer, mockInntektApi)
        every { mockConsumer.hentGrunnlag(any(), any()) } returns bidragGrunnlagRespons
        every { mockInntektApi.transformerInntekter(any()) } returns transformerInntekterRespons
    }

    @Test
    fun `skal hente og kalkulere inntektsgrunnlag riktig`() {
        val response = service.hentInntektsGrunnlag("12345678910")
        assertEquals(response, transformerInntekterRespons)
    }

    @Test
    fun `skal kunne hente ut siste 12 mnd`() {
        val response = service.hentInntektsGrunnlag("12345678910")
        checkNotNull(response)
        assertEquals(BigDecimal(378000), response
            .summertÅrsinntektListe
            .filter { it.inntektRapportering === Inntektsrapportering.AINNTEKT_BEREGNET_12MND }
            .first()
            .sumInntekt)
    }
}