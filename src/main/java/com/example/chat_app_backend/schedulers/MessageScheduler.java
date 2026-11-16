package com.example.chat_app_backend.schedulers;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.chat_app_backend.repository.ScheduledMessageRepository;
import com.example.chat_app_backend.schemas.request.ChatMessageRequest;
import com.example.chat_app_backend.models.ScheduledMessage;
import com.example.chat_app_backend.service.MessageService;
import java.util.concurrent.CompletableFuture;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;

import com.example.chat_app_backend.models.Message;

@Slf4j
@Service 
public class MessageScheduler {

    private static final int BATCH_SIZE = 100;

    private final ScheduledMessageRepository scheduledMessageRepository;
    private final MessageService messageService;
    private final Executor workExecutor;


    public MessageScheduler(ScheduledMessageRepository scheduledMessageRepository,
        MessageService messageService,
        @Qualifier("messageWorkerExecutor") Executor workExecutor
    ) {
        this.messageService = messageService;
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.workExecutor = workExecutor;
    }

    @Scheduled(fixedDelay = 2000)
    public void tick() {
        while (true) {
            List<ScheduledMessage> due = scheduledMessageRepository.findByStatusAndSendAtLessThanEqualOrderBySendAtAsc(

            ScheduledMessage.Status.PENDING, Instant.now(), PageRequest.of(0, BATCH_SIZE));

            if (due.isEmpty()) {
                log.info("No scheduled messages to send.");
                return;
            }

            List<CompletableFuture<Void>> futures = due.stream()
                .map(message -> CompletableFuture.runAsync(() -> process(message), workExecutor))
                .toList();

            // next tick fires only after the full batch finishes 
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            if (due.size() < BATCH_SIZE) {
                // log.info("Processed a full batch of {} scheduled messages, checking for more...", BATCH_SIZE);
                // tick(); // recursively call tick to process the next batch
                return; // let the next scheduled tick handle the next batch
            }
        }
    }

    public void process(ScheduledMessage message) {

        try {
            ChatMessageRequest req = new ChatMessageRequest();
            req.setSenderId(message.getSenderId());
            req.setConversationId(message.getConversationId());
            req.setContent(message.getContent());
            req.setType(Message.Type.TEXT);

            boolean ok = messageService.sendMessage(req, null);
            message.setStatus(ok ? ScheduledMessage.Status.SENT : ScheduledMessage.Status.FAILED);

        } catch(Exception e) {
                log.error("Scheduled send failed", e);
                message.setStatus(ScheduledMessage.Status.FAILED);
            }
            scheduledMessageRepository.save(message);
        }
}
