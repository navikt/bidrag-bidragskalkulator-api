package no.nav.bidrag.bidragskalkulator.featureflag

import no.nav.bidrag.bidragskalkulator.controller.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.context.annotation.Import

@SpringJUnitConfig(
    classes = [
        PrivatAvtaleController::class,
        MinSideController::class,
        BeregningController::class,
        BidragskalkulatorGrunnlagController::class
    ]
)
@Import(FeatureflagTestBeans::class)
@ActiveProfiles("dev")
@TestPropertySource(
    properties = [
        "feature.privat-avtale.enabled=true",
        "feature.minside.enabled=true"
    ]
)
class FeatureflagDevTest {

    @Autowired
    lateinit var context: ApplicationContext

    @Test
    fun `PrivatAvtaleController skal være tilgjengelig i dev`() {
        assertThat(context.getBeansOfType(PrivatAvtaleController::class.java)).isNotEmpty
    }

    @Test
    fun `MinSideController skal være tilgjengelig i dev`() {
        assertThat(context.getBeansOfType(MinSideController::class.java)).isNotEmpty
    }

    @Test
    fun `BeregningController skal være tilgjengelig i dev`() {
        assertThat(context.getBeansOfType(BeregningController::class.java)).isNotEmpty
    }

    @Test
    fun `BidragskalkulatorGrunnlagController skal være tilgjengelig i dev`() {
        assertThat(context.getBeansOfType(BidragskalkulatorGrunnlagController::class.java)).isNotEmpty
    }
}
