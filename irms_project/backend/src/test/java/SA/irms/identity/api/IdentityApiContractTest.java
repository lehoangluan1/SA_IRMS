package SA.irms.identity.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import SA.irms.common.error.DomainException;
import SA.irms.common.error.ForbiddenException;
import SA.irms.common.security.CurrentUser;
import SA.irms.common.security.PermissionGuard;
import SA.irms.identity.application.AuthService;
import SA.irms.identity.application.SettingsService;
import SA.irms.identity.application.StaffService;
import SA.irms.identity.audit.AuditService;
import SA.irms.identity.persistence.IdentityRepository;
import SA.irms.support.ApiContractTestSupport;
import SA.irms.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class IdentityApiContractTest extends ApiContractTestSupport {

    @Mock
    private AuthService authService;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private StaffService staffService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private AuditService auditService;
    @Mock
    private PermissionGuard permissionGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(currentUser.require()).thenReturn(TestFixtures.authenticatedUser("all"));
        lenient().when(permissionGuard.require(anyString())).thenReturn(TestFixtures.authenticatedUser("all"));
        mockMvc = mockMvcFor(
                new AuthController(authService, currentUser),
                new StaffController(staffService, permissionGuard),
                new SettingsController(settingsService, permissionGuard),
                new AuditController(auditService, permissionGuard, new ObjectMapper().findAndRegisterModules())
        );
    }

    @Test
    void loginReturnsEnvelopeAndFallsBackToUserAgentForDeviceId() throws Exception {
        when(authService.login(eq("manager@irms.local"), eq("Password123!"), eq("manager"), eq("Mozilla/5.0"), anyString()))
                .thenReturn(TestFixtures.loginResult());

        mockMvc.perform(post("/api/auth/login")
                        .header("User-Agent", "Mozilla/5.0")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "manager@irms.local",
                                  "password": "Password123!",
                                  "role": "manager"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.displayName").value("Alex Manager"))
                .andExpect(jsonPath("$.meta.correlationId").isNotEmpty());

        verify(authService).login(eq("manager@irms.local"), eq("Password123!"), eq("manager"), eq("Mozilla/5.0"), anyString());
    }

    @Test
    void logoutAndMeReturnCurrentSessionState() throws Exception {
        when(authService.currentUser(any())).thenReturn(TestFixtures.userView());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("signed_out"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("manager@irms.local"))
                .andExpect(jsonPath("$.data.sessionId").value(TestFixtures.SESSION_ID.toString()));
    }

    @Test
    void staffListCreateShiftAndRoleUpdateFollowCurrentContract() throws Exception {
        when(staffService.loadStaff(anyString(), any())).thenReturn(TestFixtures.staffView());
        when(staffService.createShift(any(), any(), anyString(), any())).thenReturn(TestFixtures.shiftView());
        when(staffService.updateRoles(any(), any(), anyString(), any())).thenReturn(TestFixtures.staffRowView());

        mockMvc.perform(get("/api/staff")
                        .queryParam("search", "alex")
                        .queryParam("shiftDate", LocalDate.of(2026, 4, 21).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staff").isArray())
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.shifts").isArray());

        mockMvc.perform(post("/api/shifts")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "shiftDate": "2026-04-21",
                                  "startAt": "09:00:00",
                                  "endAt": "17:00:00",
                                  "position": "Floor",
                                  "zone": "A"
                                }
                                """.formatted(TestFixtures.USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shiftAssignmentId").value(TestFixtures.SHIFT_ID.toString()))
                .andExpect(jsonPath("$.data.position").value("Floor"));

        mockMvc.perform(patch("/api/staff/{userId}/roles", TestFixtures.USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "roleIds": ["%s"]
                                }
                                """.formatted(UUID.fromString("27272727-2727-2727-2727-272727272727"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(TestFixtures.USER_ID.toString()))
                .andExpect(jsonPath("$.data.displayName").value("Alex Manager"));
    }

    @Test
    void createShiftValidationErrorsUseStandardEnvelope() throws Exception {
        mockMvc.perform(post("/api/shifts")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "shiftDate": "2026-04-21",
                                  "startAt": "09:00:00",
                                  "endAt": "17:00:00",
                                  "position": "Floor"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.code").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("userId"));
    }

    @Test
    void settingsEndpointsReturnMapsAndSupportPatch() throws Exception {
        when(settingsService.loadSettings()).thenReturn(TestFixtures.settings());
        when(settingsService.updateSettings(any(), any(), anyString(), any())).thenReturn(TestFixtures.settings());

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waitlistHoldMinutes").value(15));

        mockMvc.perform(patch("/api/settings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "waitlistHoldMinutes": 15,
                                  "reservationGraceMinutes": 10,
                                  "kitchenRushThresholdMinutes": 20,
                                  "kitchenLateThresholdMinutes": 30,
                                  "refundWindowHours": 24
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundWindowHours").value(24));
    }

    @Test
    void auditLogEndpointsCoverSearchExportAndFollowUp() throws Exception {
        var auditRows = java.util.List.of(new IdentityRepository.AuditRow(
                TestFixtures.AUDIT_ID,
                TestFixtures.NOW,
                TestFixtures.USER_ID,
                "Alex Manager",
                java.util.Set.of("manager"),
                "audit.log.viewed",
                "AuditLog",
                "audit-log",
                "corr-1",
                "Viewed the audit log",
                false,
                "127.0.0.1",
                java.util.Map.of(),
                java.util.Map.of("limit", "50")
        ));
        when(auditService.search(
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(),
                any(),
                nullable(Integer.class),
                any()))
                .thenReturn(auditRows);

        mockMvc.perform(get("/api/audit/logs").queryParam("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("audit.log.viewed"));

        mockMvc.perform(get("/api/audit/export").queryParam("format", "json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"audit.log.viewed\"")));

        mockMvc.perform(patch("/api/audit/logs/{auditLogId}/follow-up", TestFixtures.AUDIT_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "followUp": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditLogId").value(TestFixtures.AUDIT_ID.toString()))
                .andExpect(jsonPath("$.data.followUp").value(true));
    }

    @Test
    void forbiddenOperationsKeepStandardDomainErrorShape() throws Exception {
        when(permissionGuard.require("settings.view")).thenThrow(new ForbiddenException("Missing settings.view permission."));

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.message").value("Missing settings.view permission."))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void auditServiceConflictsFlowBackWithControllerAdvice() throws Exception {
        when(auditService.search(
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(),
                any(),
                nullable(Integer.class),
                any()))
                .thenThrow(new DomainException(HttpStatus.CONFLICT, "audit_export_conflict", "Audit export is already running."));

        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("audit_export_conflict"))
                .andExpect(jsonPath("$.message").value("Audit export is already running."));
    }
}
