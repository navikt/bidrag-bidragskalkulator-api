package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.bidragskalkulator.web.ApiRequestLoggingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val apiRequestLoggingInterceptor: ApiRequestLoggingInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(apiRequestLoggingInterceptor)
            .addPathPatterns("/api/v1/privat-avtale/**")         // begrens til dine APIer
            .excludePathPatterns("/actuator/**", "/swagger/**", "/v3/api-docs/**")
    }
}
