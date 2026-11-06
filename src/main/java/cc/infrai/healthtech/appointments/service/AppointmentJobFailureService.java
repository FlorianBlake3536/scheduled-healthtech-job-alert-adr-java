package cc.infrai.healthtech.appointments.service;

import cc.infrai.healthtech.appointments.domain.AppointmentJobFailure;
import cc.infrai.healthtech.appointments.domain.NotificationSeverity;
import cc.infrai.healthtech.appointments.domain.OperatorNotification;
import cc.infrai.healthtech.appointments.infra.InfraiClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AppointmentJobFailureService {
    private final InfraiClient infrai;

    public AppointmentJobFailureService(InfraiClient infrai) {
        this.infrai = infrai;
    }

    public OperatorNotification handleFailure(AppointmentJobFailure failure) {
        OperatorNotification notification = toNotification(failure);
        captureFailure(failure, notification);
        return notification;
    }

    public OperatorNotification toNotification(AppointmentJobFailure failure) {
        NotificationSeverity severity = failure.retryCount() >= 3
                ? NotificationSeverity.HIGH
                : NotificationSeverity.MEDIUM;

        boolean escalationRequired = severity == NotificationSeverity.HIGH;
        String summary = escalationRequired
                ? "Appointment job failed and needs operator review"
                : "Appointment job failed and should be checked on the next support pass";

        String details = String.format(
                "job=%s, runId=%s, appointmentId=%s, patientId=%s, step=%s, retries=%d, reason=%s",
                failure.jobName(),
                failure.runId(),
                failure.appointmentId(),
                failure.patientId(),
                failure.workflowStep(),
                failure.retryCount(),
                failure.failureReason()
        );

        return new OperatorNotification(severity, summary, details, escalationRequired);
    }

    private void captureFailure(AppointmentJobFailure failure, OperatorNotification notification) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", failure.jobName() + " failed at " + failure.workflowStep());
        payload.put("message", notification.summary());
        payload.put("level", notification.severity() == NotificationSeverity.HIGH ? "error" : "warning");
        payload.put("fingerprint", List.of(failure.jobName(), failure.workflowStep()));
        payload.put("exception", failure.failureReason());

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("runId", failure.runId());
        context.put("appointmentId", failure.appointmentId());
        context.put("patientId", failure.patientId());
        context.put("workflowStep", failure.workflowStep());
        context.put("retryCount", failure.retryCount());
        context.put("summary", notification.summary());
        payload.put("context", context);

        infrai.errors.capture(payload);
    }
}
