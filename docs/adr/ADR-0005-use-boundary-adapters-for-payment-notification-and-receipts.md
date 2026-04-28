# ADR-0005: Use boundary adapters for payment, notification and receipts

Date: 2026-04-28

## Status
Accepted

## Context
The code defines internal interfaces and adapters for payment, notification channels and receipt delivery. Current code does not show a configured external payment provider, external SMS/email provider or physical printer integration.

Rationale inferred from current implementation and constraints.

## Decision
Keep payment, notification and receipt variations behind internal adapter interfaces: `PaymentGateway`, `NotificationChannelAdapter` and `ReceiptDeliveryAdapter`.

## Consequences
Positive:
- Core billing and notification workflows depend on stable internal ports.
- Future provider-specific implementations can be added without changing domain flow diagrams.

Negative:
- Documentation must avoid describing these adapters as real external providers.
- Adapter behavior is limited by what current implementations actually do.

Neutral / trade-offs:
- Email/SMS/print labels represent internal channel adapters, not proof of external delivery infrastructure.

## Alternatives considered
- Direct provider calls in application services: simpler initially but couples domain flow to provider details.
- Model provider systems in current diagrams: rejected because current code/config does not prove they exist.

## Evidence from code/config
- `irms_project/backend/src/main/java/SA/irms/billing/application/PaymentGateway.java`
- `irms_project/backend/src/main/java/SA/irms/billing/infrastructure/payment/CashPaymentGateway.java`
- `irms_project/backend/src/main/java/SA/irms/billing/infrastructure/payment/ExternalReferencePaymentGateway.java`
- `irms_project/backend/src/main/java/SA/irms/billing/application/ReceiptDeliveryAdapter.java`
- `irms_project/backend/src/main/java/SA/irms/billing/application/DigitalReceiptDeliveryAdapter.java`
- `irms_project/backend/src/main/java/SA/irms/billing/application/PrintReceiptDeliveryAdapter.java`
- `irms_project/backend/src/main/java/SA/irms/notification/application/port/out/NotificationChannelAdapter.java`

## Related documentation and diagrams
- `sections/02_system_modeling.tex`
- `sections/03_software_architecture.tex`
- `sections/04_detailed_design.tex`
- `sections/05_appendix.tex`
- `assets/diagrams/source/design/30_uml_class_diagram_inventory_and_notification_module.dot`
- `assets/diagrams/source/modeling/13_sequence_diagram_checkout_and_payment.dot`
