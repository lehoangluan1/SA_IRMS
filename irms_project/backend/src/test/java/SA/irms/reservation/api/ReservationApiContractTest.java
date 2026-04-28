package SA.irms.reservation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import SA.irms.common.error.DomainException;
import SA.irms.common.security.PermissionGuard;
import SA.irms.reservation.application.ReservationService;
import SA.irms.reservation.application.command.ReservationCommands;
import SA.irms.support.ApiContractTestSupport;
import SA.irms.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class ReservationApiContractTest extends ApiContractTestSupport {

    @Mock
    private ReservationService reservationService;
    @Mock
    private PermissionGuard permissionGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(permissionGuard.require(anyString())).thenReturn(TestFixtures.authenticatedUser("all"));
        mockMvc = mockMvcFor(
                new ReservationController(reservationService, permissionGuard),
                new TableController(reservationService, permissionGuard),
                new WaitlistController(reservationService, permissionGuard),
                new ReservationNotificationController(reservationService, permissionGuard)
        );
    }

    @Test
    void overviewAndRecommendationsMatchReservationContract() throws Exception {
        when(reservationService.load(any())).thenReturn(TestFixtures.reservationOverview());
        when(reservationService.recommend(anyString(), anyString(), any())).thenReturn(TestFixtures.recommendation());

        mockMvc.perform(get("/api/reservations/overview").queryParam("date", "2026-04-21"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.data.tables[0].number").value(12))
                .andExpect(jsonPath("$.data.waitlist[0].status").value("WAITING"));

        mockMvc.perform(get("/api/reservations/recommendations")
                        .queryParam("date", "2026-04-21")
                        .queryParam("time", "19:00")
                        .queryParam("party", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateTables[0].id").value(TestFixtures.TABLE_ID.toString()))
                .andExpect(jsonPath("$.data.waitlistRecommended").value(false));
    }

    @Test
    void createReservationMapsUpsertPayloadIncludingFallbackToWaitlist() throws Exception {
        when(reservationService.createReservation(any(), any(), anyString(), any())).thenReturn(TestFixtures.reservationView());

        mockMvc.perform(post("/api/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "guest": "Jamie Guest",
                                  "phone": "0123456789",
                                  "email": "guest@irms.local",
                                  "party": 4,
                                  "date": "2026-04-21",
                                  "time": "18:30",
                                  "notes": "Birthday",
                                  "tableId": "%s",
                                  "fallbackToWaitlist": true
                                }
                                """.formatted(TestFixtures.TABLE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        ArgumentCaptor<ReservationCommands.ReservationUpsert> captor = ArgumentCaptor.forClass(ReservationCommands.ReservationUpsert.class);
        verify(reservationService).createReservation(captor.capture(), any(), anyString(), any());
        ReservationCommands.ReservationUpsert request = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("Jamie Guest", request.guest());
        org.junit.jupiter.api.Assertions.assertTrue(request.fallbackToWaitlist());
    }

    @Test
    void reservationUpdateConfirmCheckInAndNoShowStayDeterministic() throws Exception {
        when(reservationService.updateReservation(any(), any())).thenReturn(TestFixtures.reservationView());
        when(reservationService.confirmReservation(any())).thenReturn(TestFixtures.reservationView());
        when(reservationService.checkIn(any(), any(), any(), anyString(), any())).thenReturn(TestFixtures.reservationView());
        when(reservationService.markNoShow(any(), anyString(), any(), any())).thenReturn(TestFixtures.reservationView());

        mockMvc.perform(put("/api/reservations/{reservationId}", TestFixtures.RESERVATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "guest": "Jamie Guest",
                                  "party": 5,
                                  "notes": "Window seat"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.guest").value("Jamie Guest"));

        mockMvc.perform(post("/api/reservations/{reservationId}/confirm", TestFixtures.RESERVATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/reservations/{reservationId}/check-in", TestFixtures.RESERVATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "approveRecovery": true,
                                  "actualPartySize": 4,
                                  "replacementTableId": "%s"
                                }
                                """.formatted(TestFixtures.TABLE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.RESERVATION_ID.toString()));

        mockMvc.perform(post("/api/reservations/{reservationId}/no-show", TestFixtures.RESERVATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.RESERVATION_ID.toString()));
    }

    @Test
    void checkInWithoutBodyFallsBackToEmptyRequestPayload() throws Exception {
        when(reservationService.checkIn(any(), any(), any(), anyString(), any())).thenReturn(TestFixtures.reservationView());

        mockMvc.perform(post("/api/reservations/{reservationId}/check-in", TestFixtures.RESERVATION_ID))
                .andExpect(status().isOk());

        ArgumentCaptor<ReservationCommands.CheckInRequest> captor = ArgumentCaptor.forClass(ReservationCommands.CheckInRequest.class);
        verify(reservationService).checkIn(any(), captor.capture(), any(), anyString(), any());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().approveRecovery());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().actualPartySize());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().replacementTableId());
    }

    @Test
    void tablesWaitlistAndNotificationsCoverHappyPathOperations() throws Exception {
        when(reservationService.createTable(any())).thenReturn(TestFixtures.tableView());
        when(reservationService.updateTable(any(), any())).thenReturn(TestFixtures.tableView());
        when(reservationService.updateTableStatus(any(), any(), any(), anyString(), any())).thenReturn(TestFixtures.tableActionResult());
        when(reservationService.createWaitlistEntry(any(), any(), anyString(), any())).thenReturn(TestFixtures.waitlistView());
        when(reservationService.notifyWaitlist(any(), any(), anyString(), any())).thenReturn(TestFixtures.waitlistView());
        when(reservationService.skipWaitlist(any(), any(), anyString(), any())).thenReturn(TestFixtures.waitlistView());
        when(reservationService.prioritizeWaitlist(any(), any(), anyString(), any())).thenReturn(TestFixtures.waitlistView());
        when(reservationService.seatWaitlist(any(), any(), anyString(), any())).thenReturn(TestFixtures.waitlistView());

        mockMvc.perform(post("/api/tables")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "number": 12,
                                  "capacity": 4,
                                  "notes": "Window"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.TABLE_ID.toString()));

        mockMvc.perform(put("/api/tables/{tableId}", TestFixtures.TABLE_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "number": 12,
                                  "capacity": 4,
                                  "notes": "Patio"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes").value("Window"));

        mockMvc.perform(post("/api/tables/{tableId}/status", TestFixtures.TABLE_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus": "SEATED",
                                  "reason": "Guests arrived"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SEATED"));

        mockMvc.perform(post("/api/waitlist")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Taylor Walk-in",
                                  "phone": "0987654321",
                                  "email": "walkin@irms.local",
                                  "notes": "Near bar",
                                  "party": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestFixtures.WAITLIST_ID.toString()));

        mockMvc.perform(post("/api/waitlist/{waitlistEntryId}/notify", TestFixtures.WAITLIST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        mockMvc.perform(post("/api/waitlist/{waitlistEntryId}/skip", TestFixtures.WAITLIST_ID))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/waitlist/{waitlistEntryId}/prioritize", TestFixtures.WAITLIST_ID))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/waitlist/{waitlistEntryId}/seat", TestFixtures.WAITLIST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedTable").value(12));

        mockMvc.perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationId": "%s",
                                  "title": "Table ready",
                                  "body": "Your table is ready.",
                                  "channel": "SMS"
                                }
                                """.formatted(TestFixtures.RESERVATION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("queued"));
    }

    @Test
    void reservationValidationAndSeatConflictsStayVisibleToClients() throws Exception {
        when(reservationService.seatWaitlist(any(), any(), anyString(), any()))
                .thenThrow(new DomainException(HttpStatus.CONFLICT, "table_conflict", "Table already assigned."));

        mockMvc.perform(post("/api/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "0123456789",
                                  "party": 4,
                                  "date": "2026-04-21",
                                  "time": "18:30"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("guest"));

        mockMvc.perform(post("/api/waitlist/{waitlistEntryId}/seat", TestFixtures.WAITLIST_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("table_conflict"))
                .andExpect(jsonPath("$.message").value("Table already assigned."));
    }
}
