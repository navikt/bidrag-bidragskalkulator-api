package no.nav.bidrag.bidragskalkulator.utils

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InnloggetBrukerUtilsTest {
    private val oidcTokenManager = mockk<OidcTokenManager>()
    private val innloggetBrukerUtils = InnloggetBrukerUtils(oidcTokenManager)

    @Test
    fun `skal hente pid fra gyldig token`() {
        val pid = genererFødselsnummer()
        val claims = JWTClaimsSet.Builder().claim("pid", pid).build()
        val header = JWSHeader(JWSAlgorithm.HS256)
        val jwt = SignedJWT(header, claims)
        jwt.sign(MACSigner(ByteArray(32)))
        val token = jwt.serialize()

        every { oidcTokenManager.hentToken() } returns token

        assertEquals(pid, innloggetBrukerUtils.hentPåloggetPersonIdent())
    }

    @Test
    fun `skal returnere null hvis token mangler`() {
        every { oidcTokenManager.hentToken() } returns null

        assertNull(innloggetBrukerUtils.hentPåloggetPersonIdent())
    }

    @Test
    fun `skal kaste IllegalStateException hvis pid mangler i token`() {
        val claims = JWTClaimsSet.Builder().build()
        val header = JWSHeader(JWSAlgorithm.HS256)
        val jwt = SignedJWT(header, claims)
        jwt.sign(MACSigner(ByteArray(32)))
        val token = jwt.serialize()

        every { oidcTokenManager.hentToken() } returns token

        val ex = assertThrows<IllegalStateException> {
            innloggetBrukerUtils.hentPåloggetPersonIdent()
        }
        assertEquals("Token mangler 'pid'-claim", ex.message)
    }
}
