package org.tholian.alexa.tibber.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** KI-generiert: Zeitbudget – nie negativ, begrenzbar, Null-Budget ist sofort aufgebraucht. */
class DeadlineTest {

    @Test
    void after_zero_hasNoTimeRemaining() {
        assertThat(Deadline.after(Duration.ZERO).remaining()).isEqualTo(Duration.ZERO);
    }

    @Test
    void remaining_neverNegative_afterExpiry() throws InterruptedException {
        Deadline deadline = Deadline.after(Duration.ofMillis(5));
        Thread.sleep(20);

        assertThat(deadline.remaining()).isEqualTo(Duration.ZERO);
    }

    @Test
    void remaining_isAtMostTheBudget() {
        assertThat(Deadline.after(Duration.ofSeconds(5)).remaining())
                .isPositive().isLessThanOrEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void remainingAtMost_capsAtLimit() {
        Deadline deadline = Deadline.after(Duration.ofSeconds(5));

        assertThat(deadline.remainingAtMost(Duration.ofSeconds(1))).isEqualTo(Duration.ofSeconds(1));
        assertThat(deadline.remainingAtMost(Duration.ofSeconds(10))).isLessThanOrEqualTo(Duration.ofSeconds(5));
        assertThat(Deadline.after(Duration.ZERO).remainingAtMost(Duration.ofSeconds(1))).isEqualTo(Duration.ZERO);
    }
}
