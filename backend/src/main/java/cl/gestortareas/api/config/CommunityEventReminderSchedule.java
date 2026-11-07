package cl.gestortareas.api.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import cl.gestortareas.api.service.CommunityEventReminderService;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.event-reminders.enabled", havingValue = "true", matchIfMissing = true)
public class CommunityEventReminderSchedule {
    private static final Logger LOG = LoggerFactory.getLogger(CommunityEventReminderSchedule.class);
    private final CommunityEventReminderService reminders;

    public CommunityEventReminderSchedule(CommunityEventReminderService reminders) {
        this.reminders = reminders;
    }

    @Scheduled(fixedDelay = 30_000)
    void sendDueReminders() {
        for (var candidate : reminders.due(OffsetDateTime.now(ZoneOffset.UTC))) {
            try {
                reminders.deliver(candidate, OffsetDateTime.now(ZoneOffset.UTC));
            } catch (RuntimeException error) {
                LOG.warn("Could not send event reminder for event {}", candidate.eventId(), error);
            }
        }
    }
}