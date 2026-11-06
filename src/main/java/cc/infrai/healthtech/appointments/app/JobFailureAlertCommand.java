package cc.infrai.healthtech.appointments.app;

import cc.infrai.healthtech.appointments.config.HealthtechAlertConfig;
import cc.infrai.healthtech.appointments.domain.AppointmentJobFailure;
import cc.infrai.healthtech.appointments.domain.OperatorNotification;
import cc.infrai.healthtech.appointments.infra.InfraiClient;
import cc.infrai.healthtech.appointments.service.AppointmentJobFailureService;

public final class JobFailureAlertCommand {
    public static void main(String[] args) {
        HealthtechAlertConfig config = HealthtechAlertConfig.fromEnv();
        InfraiClient infrai = new InfraiClient(config);
        AppointmentJobFailureService service = new AppointmentJobFailureService(infrai);

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

        OperatorNotification notification = service.handleFailure(failure);
        System.out.println(notification.summary() + " | severity=" + notification.severity());
        System.out.println(notification.details());
    }
}
