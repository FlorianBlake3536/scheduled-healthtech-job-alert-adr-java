package cc.infrai.healthtech.appointments.domain;

public record AppointmentJobFailure(
        String jobName,
        String runId,
        String appointmentId,
        String patientId,
        String patientFullName,
        String workflowStep,
        String failureReason,
        int retryCount
) {
}
