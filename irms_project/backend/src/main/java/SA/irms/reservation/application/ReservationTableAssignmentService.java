package SA.irms.reservation.application;

import SA.irms.reservation.application.query.ReservationRow;
import SA.irms.reservation.application.query.TableCandidate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.reservation.application.port.out.ReservationQueryRepository;
import SA.irms.reservation.application.port.out.ReservationTableAssignmentRepository;

@Service
public class ReservationTableAssignmentService {
    private final ReservationTableAssignmentRepository tableAssignmentRepository;
    private final ReservationQueryRepository reservationReadService;

    public ReservationTableAssignmentService(
            ReservationTableAssignmentRepository tableAssignmentRepository,
            ReservationQueryRepository reservationReadService
    ) {
        this.tableAssignmentRepository = tableAssignmentRepository;
        this.reservationReadService = reservationReadService;
    }

    public int estimateQuotedWaitMinutes() {
        return 15 + (tableAssignmentRepository.countActiveWaitlistEntries() * 10);
    }

    public List<TableCandidate> findCandidateTables(int partySize) {
        return tableAssignmentRepository.loadTablesByMinimumCapacity(partySize).stream()
                .filter(candidate -> "available".equals(candidate.status()))
                .sorted(Comparator.comparingInt(TableCandidate::capacity).thenComparingInt(TableCandidate::number))
                .toList();
    }

    public UUID resolveCheckInTableId(UUID reservationId, int actualPartySize, UUID replacementTableId) {
        if (replacementTableId != null) {
            TableCandidate selectedTable = loadCandidateTable(replacementTableId)
                    .orElseThrow(() -> new NotFoundException("The replacement table was not found."));
            validateAssignedTable(selectedTable, actualPartySize);
            return selectedTable.tableId();
        }

        UUID assignedTableId = findAssignedTableId(reservationId).orElse(null);
        if (assignedTableId != null) {
            TableCandidate assignedTable = loadCandidateTable(assignedTableId)
                    .orElseThrow(() -> new NotFoundException("The assigned table was not found."));
            if (assignedTable.capacity() >= actualPartySize && List.of("available", "reserved").contains(assignedTable.status())) {
                return assignedTable.tableId();
            }
        }

        return findCandidateTables(actualPartySize).stream()
                .map(TableCandidate::tableId)
                .findFirst()
                .orElseThrow(() -> new ConflictException("No suitable table is currently available."));
    }

    public void alignReservationAssignment(UUID reservationId, UUID tableId) {
        UUID assignedTableId = findAssignedTableId(reservationId).orElse(null);
        if (assignedTableId != null && !assignedTableId.equals(tableId)) {
            tableAssignmentRepository.releaseSpecificAssignment(reservationId, assignedTableId, "Check-in reassigned to a replacement table");
            tableAssignmentRepository.markTableAvailableIfReserved(assignedTableId);
        }
        reserveTableForReservation(reservationId, tableId);
    }

    public Optional<TableCandidate> loadCandidateTable(UUID tableId) {
        return tableAssignmentRepository.loadTablesByMinimumCapacity(1).stream()
                .filter(candidate -> candidate.tableId().equals(tableId))
                .findFirst();
    }

    public void validateAssignedTable(TableCandidate table, int partySize) {
        if (table.capacity() < partySize) {
            throw new ConflictException("The selected table cannot seat the requested party size.");
        }
        if (!"available".equals(table.status())) {
            throw new ConflictException("The selected table is not available for reservation assignment.");
        }
    }

    public Optional<UUID> findAssignedTableId(UUID reservationId) {
        return tableAssignmentRepository.findAssignedTableId(reservationId);
    }

    public Optional<UUID> findAvailableTableIdForReservation(UUID reservationId) {
        ReservationRow reservation = reservationReadService.loadReservationRow(reservationId);
        return findCandidateTables(reservation.partySize()).stream()
                .map(TableCandidate::tableId)
                .findFirst();
    }

    public void reserveTableForReservation(UUID reservationId, UUID tableId) {
        tableAssignmentRepository.reserveTableForReservation(reservationId, tableId);
    }

    public void releaseReservationTable(UUID reservationId) {
        findAssignedTableId(reservationId).ifPresent(tableId ->
                tableAssignmentRepository.releaseSpecificAssignment(reservationId, tableId, "Reservation closed"));
    }
}
