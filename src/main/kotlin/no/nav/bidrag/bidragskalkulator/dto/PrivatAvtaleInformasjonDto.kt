package no.nav.bidrag.bidragskalkulator.dto

import no.nav.bidrag.domene.ident.Personident

data class PrivatAvtaleInformasjonDto (
    val ident: Personident,
    val fornavn: String,
    val etternavn: String,
)
