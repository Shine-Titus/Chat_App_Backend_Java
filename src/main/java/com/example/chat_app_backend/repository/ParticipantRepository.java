package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.chat_app_backend.models.Participant;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

import com.example.chat_app_backend.dto.MemberView;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    boolean existsByConversationIdAndUserId(Long ConversationId, Long UserId);

    List<Participant> findByConversationId(Long conversationId);

    List<Participant> findByConversationIdAndRemovedAtIsNull(Long conversationId);

    List<Participant> findByUserId(Long userId);

    Participant findFirstByConversationIdAndUserIdNot(Long conversationId, Long userId);

    Optional<Participant> findByConversationIdAndUserId(Long convervationId, Long userId);

    boolean existsByConversationIdAndUserIdAndRemovedAtIsNull(Long conversationId, Long userId);

    List<Participant> findByUserIdAndConversationIdIn(Long userId, Collection<Long> conversationIds);

    @Query("""
        SELECT p.userId AS userId, u.username AS username
        FROM Participant p
        JOIN User u ON p.userId = u.id
        WHERE p.conversationId = :cid AND p.removedAt IS NULL
        """)
    List<MemberView> findActiveMembers(@Param("cid") Long conversationId);

}