package cc.infrai.healthtech.appointments.domain;

public record OperatorNotification(
        NotificationSeverity severity,
        String summary,
        String details,
        boolean escalationRequired
) {
}
