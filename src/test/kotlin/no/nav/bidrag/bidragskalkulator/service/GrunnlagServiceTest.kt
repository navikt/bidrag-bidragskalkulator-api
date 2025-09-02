package no.nav.bidrag.bidragskalkulator.service

import io.mockk.*
import no.nav.bidrag.bidragskalkulator.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.bidragskalkulator.exception.GrunnlagNotFoundException
import no.nav.bidrag.bidragskalkulator.exception.InntektTransformException
import no.nav.bidrag.bidragskalkulator.utils.JsonUtils.readJsonFile
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.inntekt.InntektApi
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.inntekt.request.TransformerInntekterRequest
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.*

class GrunnlagServiceTest {

    private lateinit var service: GrunnlagService
    private val mockConsumer: BidragGrunnlagConsumer = mockk()
    private val mockInntektApi: InntektApi = mockk()

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
        assertEquals(transformerInntekterRespons, response)
        verify(exactly = 1) { mockConsumer.hentGrunnlag("12345678910", any()) }
        verify(exactly = 1) { mockInntektApi.transformerInntekter(any()) }
        confirmVerified(mockConsumer, mockInntektApi)
    }

    @Test
    fun `skal kunne hente ut siste 12 mnd`() {
        val response = service.hentInntektsGrunnlag("12345678910")
        assertNotNull(response)
        val siste12 = response.summertÅrsinntektListe
            .first { it.inntektRapportering === Inntektsrapportering.AINNTEKT_BEREGNET_12MND }
            .sumInntekt
        assertEquals(BigDecimal(378000), siste12)
    }

    @Test
    fun `skal kaste GrunnlagNotFoundException når grunnlagsconsumer ikke finner grunnlag`() {
        every { mockConsumer.hentGrunnlag(any(), any()) } throws GrunnlagNotFoundException("Ingen grunnlag")

        assertThrows<GrunnlagNotFoundException> {
            service.hentInntektsGrunnlag("123")
        }
    }

    @Test
    fun `skal wrappe teknisk feil fra inntekt-api i InntektTransformException`() {
        every { mockInntektApi.transformerInntekter(any()) } throws RuntimeException("Feil")

        assertThrows<InntektTransformException> {
            service.hentInntektsGrunnlag("12345678910")
        }
    }

    @Test
    fun `skal beholde cause inni InntektTransformException`() {
        val root = IllegalStateException("Feil")
        every { mockInntektApi.transformerInntekter(any()) } throws root

        val ex = assertThrows<InntektTransformException> {
            service.hentInntektsGrunnlag("12345678910")
        }
        assertSame(root, ex.cause)
    }

    @Test
    fun `skal mappe HentGrunnlagDto korrekt til TransformerInntekterRequest`() {
        // Capture requesten som sendes til InntektApi
        val captured = slot<TransformerInntekterRequest>()
        every { mockInntektApi.transformerInntekter(capture(captured)) } returns transformerInntekterRespons

        val response = service.hentInntektsGrunnlag("12345678910")
        assertNotNull(response)

        val req = captured.captured
        // 1) ainntektHentetDato = hentetTidspunkt.toLocalDate()
        assertEquals(bidragGrunnlagRespons.hentetTidspunkt.toLocalDate(), req.ainntektHentetDato)

        // 2) ainntektsposter er mappet og ikke tom
        assertTrue(req.ainntektsposter.isNotEmpty(), "ainntektsposter skal være mappet og ikke tom")

        // 3) Lister som forventes å være tomme er tomme
        assertTrue(req.vedtakstidspunktOpprinneligeVedtak.isEmpty())
        assertTrue(req.barnetilleggsliste.isEmpty())
        assertTrue(req.kontantstøtteliste.isEmpty())
        assertTrue(req.skattegrunnlagsliste.isEmpty())
        assertTrue(req.småbarnstilleggliste.isEmpty())
        assertTrue(req.utvidetBarnetrygdliste.isEmpty())
    }
}
