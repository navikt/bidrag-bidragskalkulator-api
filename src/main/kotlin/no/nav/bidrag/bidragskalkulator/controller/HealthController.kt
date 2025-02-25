package no.nav.bidrag.bidragskalkulator.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/internal")
class HealthController {

    @GetMapping("/isalive")
    fun isAlive(): ResponseEntity<Unit> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/isready")
    fun isReady(): ResponseEntity<Unit> {
        return ResponseEntity.ok().build()
    }
}
