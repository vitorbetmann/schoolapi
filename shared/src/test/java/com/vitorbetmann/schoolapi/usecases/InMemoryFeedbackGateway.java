package com.vitorbetmann.schoolapi.usecases;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.usecases.interfaces.FeedbackGateway;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class InMemoryFeedbackGateway implements FeedbackGateway {

    final List<Feedback> saved = new ArrayList<>();

    @Override
    public void save(Feedback feedback) {
        this.saved.add(feedback);
    }

    @Override
    public List<Feedback> findByDay(LocalDate day) {
        return this.saved.stream().filter(feedback -> feedback.day().equals(day)).toList();
    }
}
