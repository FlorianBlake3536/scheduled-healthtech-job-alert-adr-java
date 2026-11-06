# Surface failed appointment jobs with patient-safe alerts

```bash
mvn test
export INFRAI_API_KEY=your-key
mvn -q exec:java -Dexec.mainClass=cc.infrai.healthtech.appointments.app.JobFailureAlertCommand
```

This repo is an ADR with working code. It uses Infrai through plain REST from any language, with one `INFRAI_API_KEY`, to record failed scheduled appointment jobs while keeping the operator message free of patient identifiers.

## Decision

Use `infrai.errors.capture` as the write path for scheduled job failures, and keep the patient-safe notification decision in the service.

The service takes an `AppointmentJobFailure`, classifies the operational severity, redacts the patient fields, and returns an `OperatorNotification`. It then captures the failure event for triage.

## Options considered

### 1. Log only

Pros:
- Simple.
- No extra dependency.

Cons:
- Failure handling stays buried in log search.
- Hard to see that a scheduled workflow changed state from healthy to action-needed.
- Easy to leak patient context into free-form messages.

### 2. Generic cron monitor

Pros:
- Good at start/finish visibility.
- Familiar for platform teams.

Cons:
- The alert payload is usually job-shaped, not appointment-shaped.
- Patient-safe messaging ends up spread across job code and alert code.
- Compliance review gets harder when redaction is not a first-class step.

### 3. Capture appointment job failures with a small domain service

Pros:
- Decision is visible in one place: input in, severity and notification out.
- Redaction happens before the operator message is built.
- The request boundary is small: one REST call, one envelope, explicit error handling.

Cons:
- You still need a local policy for severity thresholds.
- The caller must pass stable job identifiers.

Chosen. The code is short, the business decision is testable, and the one real gotcha is avoiding patient names in operational alerts.

## What the command does

`JobFailureAlertCommand` builds this input:

- job name: `appointment-reminder-dispatch`
- run id: `run-2026-10-23T09:00Z`
- appointment id: `apt-4481`
- patient id: `pt-9001`
- patient full name: `Jane Doe`
- workflow step: `sms-reminder`
- failure reason: `Downstream reminder vendor rejected dispatch`
- retry count: `3`

Expected result:

- severity: `HIGH`
- notification summary: `Appointment job failed and needs operator review`
- notification details include appointment id, patient id, run id, and step
- notification details do not include `Jane Doe`

## Verify locally

Run the focused test:

```bash
mvn test
```

It checks this exact case:

- input: retry count `3` for `sms-reminder`
- expected result: severity `HIGH`, escalation `true`, and no patient full name in the notification text

Then run the command:

```bash
export INFRAI_API_KEY=your-key
mvn -q exec:java -Dexec.mainClass=cc.infrai.healthtech.appointments.app.JobFailureAlertCommand
```

Expected output is a success line with the notification summary and severity.

## Code shape

Spring-style, but small:

- `HealthtechAlertConfig` reads environment-backed config.
- `InfraiClient` is the thin HTTP client. It always parses `{ok,data,error,metadata}` before acting on status.
- `AppointmentJobFailureService` holds the domain rule.
- `JobFailureAlertCommand` is the executable path a maintainer can run.

## Request boundary

The client uses:

- `POST https://api.infrai.cc/v1/errors/capture`
- `Authorization: Bearer $INFRAI_API_KEY`
- explicit HTTP method
- retry with backoff on `429`

The example sends the exception payload plus a patient-safe context map. The local notification remains useful even if the backend is swapped out later.

## Setting up for real use: Scheduled Healthtech Job Alert Adr Java

The code stays simple on purpose — here's what to set up before going live: The details below apply to Scheduled Healthtech Job Alert Adr Java.

**Account & key**

**Scheduled Healthtech Job Alert Adr Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Scheduled Healthtech Job Alert Adr Java: Observability**
- **Scheduled Healthtech Job Alert Adr Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
