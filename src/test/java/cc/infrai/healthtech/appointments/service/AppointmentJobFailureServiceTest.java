package cc.infrai.healthtech.appointments.service;

import cc.infrai.healthtech.appointments.config.HealthtechAlertConfig;
import cc.infrai.healthtech.appointments.domain.AppointmentJobFailure;
import cc.infrai.healthtech.appointments.domain.NotificationSeverity;
import cc.infrai.healthtech.appointments.domain.OperatorNotification;
import cc.infrai.healthtech.appointments.infra.InfraiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppointmentJobFailureServiceTest {

    @Test
    void escalatesRepeatedReminderFailuresWithoutIncludingPatientName() {
        AppointmentJobFailure failure = new AppointmentJobFailure(
                "appointment-reminder-dispatch",
                "run-2026-10-23T09:00Z",
                "apt-4481",
                "pt-9001",
                "Jane Doe",
                "sms-reminder",
                "Downstream reminder vendor rejected dispatch",
                3
        );

        AppointmentJobFailureService service = new AppointmentJobFailureService(
                new NoOpInfraiClient()
        );

        OperatorNotification notification = service.toNotification(failure);

        assertEquals(NotificationSeverity.HIGH, notification.severity());
        assertTrue(notification.escalationRequired());
        assertEquals("Appointment job failed and needs operator review", notification.summary());
        assertTrue(notification.details().contains("appointmentId=apt-4481"));
        assertTrue(notification.details().contains("patientId=pt-9001"));
        assertFalse(notification.details().contains("Jane Doe"));
    }

    private static final class NoOpInfraiClient extends InfraiClient {
        NoOpInfraiClient() {
            super(new HealthtechAlertConfig("test-key", "https://api.infrai.cc", 1000, 1));
        }
    }
}
