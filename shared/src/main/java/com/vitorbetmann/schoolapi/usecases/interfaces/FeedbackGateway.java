package com.vitorbetmann.schoolapi.usecases.interfaces;

import com.vitorbetmann.schoolapi.entities.Feedback;

import java.time.LocalDate;
import java.util.List;

/**
 * Persistence port for feedbacks.
 */
public interface FeedbackGateway {

    /**
     * Stores a new feedback.
     *
     * @param feedback the validated feedback to store
     */
    void save(Feedback feedback);

    /**
     * Returns every feedback whose {@link Feedback#day()} equals the given day.
     *
     * @param day the day to look up, in {@link Feedback#REPORTING_ZONE}
     * @return the feedbacks of that day, in any order; empty if there are none
     */
    List<Feedback> findByDay(LocalDate day);
}
