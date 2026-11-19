package com.vitorbetmann.schoolapi.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportPeriodTest {

    @Test
    void coversTheSevenDaysBeforeTheReferenceDate() {
        var period = ReportPeriod.weekBefore(LocalDate.parse("2026-11-23"));

        assertThat(period.start()).isEqualTo(LocalDate.parse("2026-11-16"));
        assertThat(period.end()).isEqualTo(LocalDate.parse("2026-11-22"));
        assertThat(period.days()).hasSize(7)
                .startsWith(LocalDate.parse("2026-11-16"))
                .endsWith(LocalDate.parse("2026-11-22"));
    }

    @Test
    void containsOnlyDaysInsideThePeriod() {
        var period = ReportPeriod.weekBefore(LocalDate.parse("2026-11-23"));

        assertThat(period.contains(LocalDate.parse("2026-11-16"))).isTrue();
        assertThat(period.contains(LocalDate.parse("2026-11-22"))).isTrue();
        assertThat(period.contains(LocalDate.parse("2026-11-15"))).isFalse();
        assertThat(period.contains(LocalDate.parse("2026-11-23"))).isFalse();
    }

    @Test
    void labelsPeriodWithIsoWeekOfItsStart() {
        assertThat(ReportPeriod.weekBefore(LocalDate.parse("2026-11-23")).isoWeek()).isEqualTo("2026-W47");
    }

    @Test
    void usesWeekBasedYearAroundNewYear() {
        var period = new ReportPeriod(LocalDate.parse("2027-01-01"), LocalDate.parse("2027-01-07"));

        assertThat(period.isoWeek()).isEqualTo("2026-W53");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> new ReportPeriod(LocalDate.parse("2026-11-22"), LocalDate.parse("2026-11-16")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Report period end is before start: 2026-11-22 > 2026-11-16");
    }
}
