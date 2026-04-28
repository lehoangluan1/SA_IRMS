package SA.irms.reservation.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.identity.BranchView;
import SA.irms.common.context.RequestMetadata;

@Service
public class ReservationWaitlistService {
    private final ReservationWaitlistCreationService creationService;
    private final ReservationWaitlistNotificationFlowService notificationFlowService;
    private final ReservationWaitlistSeatingService seatingService;
    private final ReservationWaitlistExpiryService expiryService;
    private final SA.irms.reservation.application.port.out.WaitlistQueryRepository readService;

    ReservationWaitlistService(
            ReservationWaitlistCreationService creationService,
            ReservationWaitlistNotificationFlowService notificationFlowService,
            ReservationWaitlistSeatingService seatingService,
            ReservationWaitlistExpiryService expiryService,
            SA.irms.reservation.application.port.out.WaitlistQueryRepository readService
    ) {
        this.creationService = creationService;
        this.notificationFlowService = notificationFlowService;
        this.seatingService = seatingService;
        this.expiryService = expiryService;
        this.readService = readService;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView createWaitlistEntry(SA.irms.reservation.application.command.ReservationCommands.WaitlistUpsert request, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        return creationService.createWaitlistEntry(request, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView notifyWaitlist(UUID waitlistEntryId, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        return notificationFlowService.notifyWaitlist(waitlistEntryId, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView skipWaitlist(UUID waitlistEntryId, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        return notificationFlowService.skipWaitlist(waitlistEntryId, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView prioritizeWaitlist(UUID waitlistEntryId, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        return notificationFlowService.prioritizeWaitlist(waitlistEntryId, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView seatWaitlist(UUID waitlistEntryId, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        return seatingService.seatWaitlist(waitlistEntryId, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public void expireOverdueWaitlistEntries() {
        expiryService.expireOverdueWaitlistEntries();
    }

    @Transactional
    public void createWaitlistFromReservation(BranchView branch, String guest, String phone, int party, String notes, UUID reservationId) {
        creationService.createWaitlistFromReservation(branch, guest, phone, party, notes, reservationId);
    }

    public SA.irms.reservation.application.view.ReservationViews.WaitlistView findWaitlistEntry(UUID waitlistEntryId) {
        return readService.findWaitlistEntry(waitlistEntryId);
    }
}
