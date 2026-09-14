package com.mtcrm.notification;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskReminderJobTest {
    @Test
    void finalizesEachSuccessfulDeliveryIndependently() {
        TaskReminderQueue queue = mock(TaskReminderQueue.class);
        MailService mail = mock(MailService.class);
        var delivery = new TaskReminderQueue.Delivery(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var prepared = new TaskReminderQueue.PreparedDelivery("user@example.test", "Follow up");
        when(queue.claimBatch(any())).thenReturn(List.of(delivery));
        when(queue.prepareDelivery(org.mockito.ArgumentMatchers.eq(delivery), any())).thenReturn(java.util.Optional.of(prepared));
        when(mail.taskOverdue(prepared.email(), prepared.title())).thenReturn(true);

        new TaskReminderJob(queue, mail).sendOverdueReminders();

        verify(queue).markSent(org.mockito.ArgumentMatchers.eq(delivery), any(Instant.class));
        verify(queue, never()).markFailed(any(), any());
    }

    @Test
    void failedDeliveryIsReleasedWithBackoff() {
        TaskReminderQueue queue = mock(TaskReminderQueue.class);
        MailService mail = mock(MailService.class);
        var delivery = new TaskReminderQueue.Delivery(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var prepared = new TaskReminderQueue.PreparedDelivery("user@example.test", "Follow up");
        when(queue.claimBatch(any())).thenReturn(List.of(delivery));
        when(queue.prepareDelivery(org.mockito.ArgumentMatchers.eq(delivery), any())).thenReturn(java.util.Optional.of(prepared));
        when(mail.taskOverdue(prepared.email(), prepared.title())).thenReturn(false);

        new TaskReminderJob(queue, mail).sendOverdueReminders();

        verify(queue).markFailed(org.mockito.ArgumentMatchers.eq(delivery), any(Instant.class));
        verify(queue, never()).markSent(any(), any());
    }
}
