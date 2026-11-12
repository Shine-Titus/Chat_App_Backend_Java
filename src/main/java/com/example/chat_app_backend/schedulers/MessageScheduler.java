package com.example.chat_app_backend.schedulers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.chat_app_backend.repository.ScheduledMessageRepository;
import com.example.chat_app_backend.models.ScheduledMessage;
import com.example.chat_app_backend.service.MessageService;

import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service 
public class MessageScheduler {

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final MessageService messageService;

    public MessageScheduler(ScheduledMessageRepository scheduledMessageRepository,
        MessageService messageService
    ) {
        this.messageService = messageService;
        this.scheduledMessageRepository = scheduledMessageRepository;
    }

    @Scheduled(fixedDelay = 10000)
    public void checkAndSendScheduledMessages() {
        
        List<ScheduledMessage> due = scheduledMessageRepository.findByStatusAndSendAtLessThanEqualOrderBySendAtAsc(ScheduledMessage.Status.PENDING, Instant.now());
        log.info("Scheduler tick: {} due messages", due.size());

        for (ScheduledMessage m : due) {

            try {
                boolean ok = messageService.sendMessage(m.getSenderId(), m.getConversationId(), m.getContent());
                m.setStatus(ok ? ScheduledMessage.Status.SENT : ScheduledMessage.Status.FAILED);

            } catch(Exception e) {
                m.setStatus(ScheduledMessage.Status.FAILED);
            }
            scheduledMessageRepository.save(m);
        }

    }
    
}
