package com.vitorbetmann.schoolapi.usecases;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.usecases.interfaces.NotificationGateway;
import com.vitorbetmann.schoolapi.usecases.messages.PtBrMessages;

/**
 * Sends the administrators an urgent notice about a critical feedback.
 */
public class NotifyCriticalFeedback {

    private final NotificationGateway notificationGateway;

    public NotifyCriticalFeedback(NotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    /**
     * Publishes the pt-BR notice (description, urgency and submission date) for a critical feedback.
     *
     * @param feedback the stored feedback
     * @return {@code true} if a notice was published; {@code false} if the feedback is not critical
     */
    public boolean execute(Feedback feedback) {
        if (feedback.urgency() != Urgency.CRITICAL) {
            return false;
        }
        this.notificationGateway.publish(PtBrMessages.CRITICAL_FEEDBACK_SUBJECT,
                PtBrMessages.criticalFeedbackMessage(feedback));
        return true;
    }
}
