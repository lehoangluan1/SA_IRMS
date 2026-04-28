package SA.irms.reservation.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcWaitlistCommandRepository {
    private final JdbcClient jdbcClient;

    JdbcWaitlistCommandRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    UUID createWaitlistEntry(UUID branchId, String name, String phone, String email, int party, int quotedWaitMinutes, String notes) {
        UUID waitlistEntryId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into waitlist_entries (
                            waitlist_entry_id,
                            branch_id,
                            contact_name,
                            contact_phone,
                            contact_email,
                            notification_channel,
                            party_size,
                            quoted_wait_min,
                            status,
                            notes,
                            priority,
                            added_at
                        ) values (
                            :waitlistEntryId,
                            :branchId,
                            :contactName,
                            :contactPhone,
                            :contactEmail,
                            'sms',
                            :partySize,
                            :quotedWaitMin,
                            'waiting',
                            :notes,
                            0,
                            now()
                        )
                        """)
                .param("waitlistEntryId", waitlistEntryId)
                .param("branchId", branchId)
                .param("contactName", name)
                .param("contactPhone", phone)
                .param("contactEmail", email)
                .param("partySize", party)
                .param("quotedWaitMin", quotedWaitMinutes)
                .param("notes", notes)
                .update();
        return waitlistEntryId;
    }

    void markNotified(UUID waitlistEntryId, int maxHoldMinutes) {
        jdbcClient.sql("""
                        update waitlist_entries
                        set status = 'notified',
                            notified_at = now(),
                            hold_expires_at = now() + make_interval(mins => :maxHoldMinutes),
                            updated_at = now()
                        where waitlist_entry_id = :waitlistEntryId
                        """)
                .param("maxHoldMinutes", maxHoldMinutes)
                .param("waitlistEntryId", waitlistEntryId)
                .update();
    }

    void markSkipped(UUID waitlistEntryId) {
        jdbcClient.sql("""
                        update waitlist_entries
                        set status = 'skipped',
                            updated_at = now()
                        where waitlist_entry_id = :waitlistEntryId
                        """)
                .param("waitlistEntryId", waitlistEntryId)
                .update();
    }

    int nextPriority() {
        return jdbcClient.sql("""
                        select coalesce(max(priority), 0) + 1
                        from waitlist_entries
                        where status in ('waiting', 'notified', 'skipped', 'expired')
                        """)
                .query(Integer.class)
                .single();
    }

    void updatePriority(UUID waitlistEntryId, int priority) {
        jdbcClient.sql("""
                        update waitlist_entries
                        set priority = :priority,
                            updated_at = now()
                        where waitlist_entry_id = :waitlistEntryId
                        """)
                .param("priority", priority)
                .param("waitlistEntryId", waitlistEntryId)
                .update();
    }

    List<UUID> loadExpiredNotifiedWaitlistIds() {
        return jdbcClient.sql("""
                        select waitlist_entry_id
                        from waitlist_entries
                        where status = 'notified'
                          and hold_expires_at is not null
                          and hold_expires_at < now()
                        """)
                .query(UUID.class)
                .list();
    }

    boolean expireNotifiedWaitlistEntry(UUID waitlistEntryId) {
        int updated = jdbcClient.sql("""
                        update waitlist_entries
                        set status = 'expired',
                            updated_at = now()
                        where waitlist_entry_id = :waitlistEntryId
                          and status = 'notified'
                        """)
                .param("waitlistEntryId", waitlistEntryId)
                .update();
        return updated > 0;
    }

    void markWaitlistSeated(UUID waitlistEntryId) {
        jdbcClient.sql("""
                        update waitlist_entries
                        set status = 'seated',
                            seated_at = now(),
                            updated_at = now()
                        where waitlist_entry_id = :waitlistEntryId
                        """)
                .param("waitlistEntryId", waitlistEntryId)
                .update();
    }
}
