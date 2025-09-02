package no.nav.bidrag.bidragskalkulator.web

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val logger = KotlinLogging.logger {}

@Component
class ApiRequestLoggingInterceptor : HandlerInterceptor {

    // Hjelpenøkkel for å lagre starttid i request-attributter
    private val startTimeKey = "requestStartTimeMs"

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.setAttribute("requestStartTimeMs", System.currentTimeMillis())
        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        val start = request.getAttribute(startTimeKey) as? Long
        val duration = if (start != null) System.currentTimeMillis() - start else -1L

        val method = request.method
        val path = request.requestURI
        val status = response.status
        val cid = MDC.get("correlationId")

        if (ex == null) {
            // Suksess/forventede svar → INFO
            logger.info {
                "HTTP $method $path fullført (status=$status, varighet_ms=$duration" +
                        (cid?.let { ", correlationId=$it" } ?: "") + ")"
            }
        } else {
            // Uventet feil → ERROR + melding
            logger.error{
                "HTTP $method $path feilet (status=$status, varighet_ms=$duration" +
                        (cid?.let { ", correlationId=$it" } ?: "") + "): ${ex.message}"
            }
        }
    }
}
