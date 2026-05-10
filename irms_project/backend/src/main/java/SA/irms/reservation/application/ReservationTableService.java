package SA.irms.reservation.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.context.RequestMetadata;
import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.reservation.application.port.out.ReservationTableCommandPort;
import SA.irms.reservation.application.port.out.TableQueryRepository;
import SA.irms.common.identity.BranchView;
import SA.irms.common.identity.SharedIdentityPolicyPort;

@Service
public class ReservationTableService {
    private final ReservationTableCommandPort tableCommandPort;
    private final TableQueryRepository tableReadService;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final ReservationTableStatusService reservationTableStatusService;

    public ReservationTableService(
            ReservationTableCommandPort tableCommandPort,
            TableQueryRepository tableReadService,
            SharedIdentityPolicyPort identityPolicyPort,
            ReservationTableStatusService reservationTableStatusService
    ) {
        this.tableCommandPort = tableCommandPort;
        this.tableReadService = tableReadService;
        this.identityPolicyPort = identityPolicyPort;
        this.reservationTableStatusService = reservationTableStatusService;
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.TableView createTable(SA.irms.reservation.application.command.ReservationCommands.TableUpsert request) {
        BranchView branch = identityPolicyPort.findDefaultBranch();
        UUID tableId = tableCommandPort.createTable(branch.branchId(), request.number(), request.capacity(), request.notes());
        return tableReadService.findTable(tableId);
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.TableView updateTable(UUID tableId, SA.irms.reservation.application.command.ReservationCommands.TableUpsert request) {
        tableCommandPort.updateTable(tableId, request.number(), request.capacity(), request.notes());
        return tableReadService.findTable(tableId);
    }

    @Transactional
    public void deleteTable(UUID tableId) {
        if (tableCommandPort.countActiveSessions(tableId) > 0) {
            throw new ConflictException("The table cannot be deleted while it has an active session.");
        }
        if (tableCommandPort.deleteTable(tableId) == 0) {
            throw new NotFoundException("Table was not found.");
        }
    }

    @Transactional
    public SA.irms.reservation.application.view.ReservationViews.TableActionResult updateTableStatus(
            UUID tableId,
            SA.irms.reservation.application.command.ReservationCommands.TableStatusUpdate request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata requestMetadata
    ) {
        return reservationTableStatusService.updateTableStatus(tableId, request, actor, correlationId, requestMetadata);
    }

    public SA.irms.reservation.application.view.ReservationViews.TableView findTable(UUID tableId) {
        return tableReadService.findTable(tableId);
    }
}
