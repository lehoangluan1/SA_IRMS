package SA.irms.reservation.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.error.ConflictException;
import SA.irms.reservation.application.port.out.ReservationNotificationTargetRepository;
import SA.irms.reservation.application.query.ReservationNotificationTarget;
import SA.irms.reservation.application.support.ReservationNotificationMessageFactory;
import SA.irms.common.notification.NotificationCommandPublisher;

@Service
public class ReservationNotificationCoordinator {
    private final ReservationNotificationTargetRepository targetRepository;
    private final ReservationNotificationMessageFactory messageFactory;
    private final NotificationCommandPublisher notificationCommandPublisher;

    public ReservationNotificationCoordinator(
            ReservationNotificationTargetRepository targetRepository,
            ReservationNotificationMessageFactory messageFactory,
            NotificationCommandPublisher notificationCommandPublisher
    ) {
        this.targetRepository = targetRepository;
        this.messageFactory = messageFactory;
        this.notificationCommandPublisher = notificationCommandPublisher;
    }

    public void sendNotification(UUID reservationId, UUID waitlistEntryId, String title, String body, String channel) {
        if (reservationId == null && waitlistEntryId == null) {
            throw new ConflictException("A reservation or waitlist target is required for notification delivery.");
        }
        ReservationNotificationTarget target = reservationId != null
                ? targetRepository.loadReservationNotificationTarget(reservationId)
                : targetRepository.loadWaitlistNotificationTarget(waitlistEntryId);
        UUID aggregateId = reservationId == null ? waitlistEntryId : reservationId;
        notificationCommandPublisher.enqueue(
                messageFactory.customerNotification(reservationId, waitlistEntryId, target, title, body, channel),
                reservationId == null ? "WaitlistEntry" : "Reservation",
                aggregateId.toString(),
                "reservation-notification-" + aggregateId
        );
    }

    public void queueStaffReservationNotification(
            UUID reservationId,
            String type,
            String templateCode,
            String title,
            String body
    ) {
        for (var command : messageFactory.staffNotifications(reservationId, type, templateCode, title, body)) {
            notificationCommandPublisher.enqueue(command, "Reservation", reservationId.toString(), "reservation-" + reservationId);
        }
    }
}
