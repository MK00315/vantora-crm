package com.mtcrm.notification;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TaskReminderJob {
    private final TaskReminderQueue queue;
    private final MailService mail;

    public TaskReminderJob(TaskReminderQueue queue, MailService mail) {
        this.queue = queue;
        this.mail = mail;
    }

    @Scheduled(cron = "${app.reminders.cron:0 0 * * * *}")
    @SchedulerLock(name = "overdue-task-reminders", lockAtMostFor = "30m", lockAtLeastFor = "5s")
    public void sendOverdueReminders() {
        for (TaskReminderQueue.Delivery delivery : queue.claimBatch(Instant.now())) {
            queue.prepareDelivery(delivery, Instant.now()).ifPresent(prepared -> {
                boolean delivered = mail.taskOverdue(prepared.email(), prepared.title());
                if (delivered) queue.markSent(delivery, Instant.now());
                else queue.markFailed(delivery, Instant.now());
            });
        }
    }
}
