package SA.irms.reservation.application;

import SA.irms.reservation.application.support.ReservationTimeSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.context.RequestMetadata;

@Service
public class ReservationService {
    private final SA.irms.reservation.application.port.out.ReservationQueryRepository reservationReadService;
    private final ReservationLifecycleService reservationLifecycleService;
    private final ReservationTableAssignmentService tableAssignmentService;
    private final ReservationTableService reservationTableService;
    private final ReservationWaitlistService reservationWaitlistService;
    private final ReservationNotificationCoordinator reservationNotificationCoordinator;
    private final ReservationAutomationService reservationAutomationService;

    public ReservationService(
            SA.irms.reservation.application.port.out.ReservationQueryRepository reservationReadService,
            ReservationLifecycleService reservationLifecycleService,
            ReservationTableAssignmentService tableAssignmentService,
            ReservationTableService reservationTableService,
            ReservationWaitlistService reservationWaitlistService,
            ReservationNotificationCoordinator reservationNotificationCoordinator,
            ReservationAutomationService reservationAutomationService
    ) {
        this.reservationReadService = reservationReadService;
        this.reservationLifecycleService = reservationLifecycleService;
        this.tableAssignmentService = tableAssignmentService;
        this.reservationTableService = reservationTableService;
        this.reservationWaitlistService = reservationWaitlistService;
        this.reservationNotificationCoordinator = reservationNotificationCoordinator;
        this.reservationAutomationService = reservationAutomationService;
    }

    public SA.irms.reservation.application.view.ReservationViews.ReservationOverview load(LocalDate date) {
        return reservationReadService.load(date);
    }

    public SA.irms.reservation.application.view.ReservationViews.ReservationRecommendation recommend(String date, String time, Integer party) {
        if (party == null || party < 1) {
            throw new ConflictException("Party size must be at least 1.");
        }
        Instant arrivalAt = ReservationTimeSupport.toArrivalInstant(date, time);
        List<SA.irms.reservation.application.view.ReservationViews.RecommendedTable> candidateTables = tableAssignmentService.findCandidateTables(party).stream()
                .map(candidate -> new SA.irms.reservation.application.view.ReservationViews.RecommendedTable(candidate.tableId(), candidate.number(), candidate.capacity()))
                .toList();
        int quotedWait = tableAssignmentService.estimateQuotedWaitMinutes();
        return new SA.irms.reservation.application.view.ReservationViews.ReservationRecommendation(
                arrivalAt.atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString(),
                ReservationTimeSupport.formatTime(arrivalAt),
                party,
                candidateTables,
                candidateTables.isEmpty(),
                quotedWait
        );
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView createReservation(
            SA.irms.reservation.application.command.ReservationCommands.ReservationUpsert request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return reservationLifecycleService.createReservation(request, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView updateReservation(UUID reservationId, SA.irms.reservation.application.command.ReservationCommands.ReservationPatch request) {
        return reservationLifecycleService.updateReservation(reservationId, request);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView confirmReservation(UUID reservationId) {
        return reservationLifecycleService.confirmReservation(reservationId);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView markNoShow(UUID reservationId, String correlationId, AuthenticatedUser actor, RequestMetadata httpServletRequest) {
        return reservationLifecycleService.markNoShow(reservationId, correlationId, actor, httpServletRequest);
    }

    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void processReservationTimers() {
        reservationAutomationService.processReservationTimers();
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.ReservationView checkIn(
            UUID reservationId,
            SA.irms.reservation.application.command.ReservationCommands.CheckInRequest request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return reservationLifecycleService.checkIn(reservationId, request, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.TableView createTable(SA.irms.reservation.application.command.ReservationCommands.TableUpsert request) {
        return reservationTableService.createTable(request);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.TableView updateTable(UUID tableId, SA.irms.reservation.application.command.ReservationCommands.TableUpsert request) {
        return reservationTableService.updateTable(tableId, request);
    }

    @Transactional
    public void deleteTable(UUID tableId) {
        reservationTableService.deleteTable(tableId);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.TableActionResult updateTableStatus(
            UUID tableId,
            SA.irms.reservation.application.command.ReservationCommands.TableStatusUpdate request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return reservationTableService.updateTableStatus(tableId, request, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView createWaitlistEntry(
            SA.irms.reservation.application.command.ReservationCommands.WaitlistUpsert request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return reservationWaitlistService.createWaitlistEntry(request, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView notifyWaitlist(
            UUID waitlistEntryId,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return reservationWaitlistService.notifyWaitlist(waitlistEntryId, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView skipWaitlist(
            UUID waitlistEntryId,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return reservationWaitlistService.skipWaitlist(waitlistEntryId, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView prioritizeWaitlist(
            UUID waitlistEntryId,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return reservationWaitlistService.prioritizeWaitlist(waitlistEntryId, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.WaitlistView seatWaitlist(
            UUID waitlistEntryId,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return reservationWaitlistService.seatWaitlist(waitlistEntryId, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public void sendNotification(UUID reservationId, UUID waitlistEntryId, String title, String body, String channel) {
        reservationNotificationCoordinator.sendNotification(reservationId, waitlistEntryId, title, body, channel);
    }

}
