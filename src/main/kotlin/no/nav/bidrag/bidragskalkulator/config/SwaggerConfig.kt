package no.nav.bidrag.bidragskalkulator.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import no.nav.bidrag.bidragskalkulator.dto.Oppgjørsform
import no.nav.bidrag.bidragskalkulator.dto.Vedleggskrav
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.NavSkjemaId
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.Språkkode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(
    info = io.swagger.v3.oas.annotations.info.Info(title = "bidrag-bidragskalkulator-api", version = "v1"),
    security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)],
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
    bearerFormat = "JWT",
    name = SecurityConstants.BEARER_KEY,
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
)
@Configuration
class SwaggerConfig {

    @Bean
    fun customOpenApi(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi: OpenAPI ->
            val samværsklasseSchema = Schema<String>()
                .description("Samværsklasse for barnet")
                .example(Samværsklasse.SAMVÆRSKLASSE_1.name)
                .apply {
                    enum = Samværsklasse.entries.map { it.name }
                }

            val oppgjørsformSchema = Schema<String>()
                .description("Angir hvordan bidraget skal betales eller innkreves." +
                        "PRIVATE = bidraget gjøres opp privat, " +
                        "NAV_INNKREVING = bidraget betales via Skatteetaten/NAV Innkreving.")
                .example(Oppgjørsform.INNKREVING.name)
                .apply {
                    enum = Oppgjørsform.entries.map { it.name }
                }

            val vedleggskravSchema = Schema<String>()
                .description("Tilleggsdokumentasjon som følger saken")
                .example(Vedleggskrav.INGEN_EKSTRA_DOKUMENTASJON.name)
                .apply {
                    enum = Vedleggskrav.entries.map { it.name }
                }

            val språkkodeSchema = Schema<String>()
                .description("Språkkode som angir hvilket språk dokumentet skal genereres på")
                .example(Språkkode.NB.name)
                .apply {
                    enum = Språkkode.entries.map { it.name }
                }

            val navSkjemaIdSchema = Schema<String>()
                .description("NAV-skjema som benyttes, identifisert ved skjema-ID")
                .example(NavSkjemaId.AVTALE_OM_BARNEBIDRAG_UNDER_18.name)
                .apply {
                    enum = NavSkjemaId.entries.map { it.name }
                }

            openApi.components = (openApi.components ?: Components())
                .addSchemas("Samværsklasse", samværsklasseSchema)
                .addSchemas("Oppgjørsform", oppgjørsformSchema)
                .addSchemas("Vedleggskrav", vedleggskravSchema)
                .addSchemas("Språkkode", språkkodeSchema)
                .addSchemas("NavSkjemaId", navSkjemaIdSchema)
        }
    }
}
