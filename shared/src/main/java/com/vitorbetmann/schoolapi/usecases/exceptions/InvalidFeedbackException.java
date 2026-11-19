package com.vitorbetmann.schoolapi.usecases.exceptions;

import java.util.List;

public class InvalidFeedbackException extends RuntimeException {

    private final List<String> violations;

    public InvalidFeedbackException(List<String> violations) {
        super("Invalid feedback: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return this.violations;
    }
}
