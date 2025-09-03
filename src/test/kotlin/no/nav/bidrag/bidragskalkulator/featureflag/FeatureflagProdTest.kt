package no.nav.bidrag.bidragskalkulator.featureflag


import no.nav.bidrag.bidragskalkulator.controller.BeregningController
import no.nav.bidrag.bidragskalkulator.controller.BidragskalkulatorGrunnlagController
import no.nav.bidrag.bidragskalkulator.controller.MinSideController
import no.nav.bidrag.bidragskalkulator.controller.PrivatAvtaleController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import


@SpringJUnitConfig(
    classes = [
        PrivatAvtaleController::class,
        MinSideController::class,
        BeregningController::class,
        BidragskalkulatorGrunnlagController::class
    ]
)
@Import(TestBeans::class)
@ActiveProfiles("prod")
@TestPropertySource(
    properties = [
        "feature.privat-avtale.enabled=false",
        "feature.minside.enabled=false"
    ]
)
class FeatureflagProdTest {

    @Autowired lateinit var context: ApplicationContext

    @Test fun `PrivatAvtaleController skal IKKE være tilgjengelig i prod`() {
        assertThat(context.getBeansOfType(PrivatAvtaleController::class.java)).isEmpty()
    }

    @Test fun `MinSideController skal IKKE være tilgjengelig i prod`() {
        assertThat(context.getBeansOfType(MinSideController::class.java)).isEmpty()
    }

    @Test fun `BeregningController skal være tilgjengelig i prod`() {
        assertThat(context.getBeansOfType(BeregningController::class.java)).isNotEmpty
    }

    @Test fun `BidragskalkulatorGrunnlagController skal være tilgjengelig i prod`() {
        assertThat(context.getBeansOfType(BidragskalkulatorGrunnlagController::class.java)).isNotEmpty
    }
}
