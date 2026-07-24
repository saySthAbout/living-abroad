package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // countryCode/keyword는 필터를 안 걸 때 null 대신 빈 문자열("")로 넘어온다 — null을 바인딩하면
    // Hibernate가 파라미터의 실제 타입(String)을 값에서 추론하지 못해, PostgreSQL이 이 파라미터를
    // bytea로 오추론해 "function lower(bytea) does not exist" 에러가 나는 문제가 있었다(ChatService
    // 참고). keyword는 세션 제목뿐 아니라 세션에 속한 어떤 메시지의 내용이라도 일치하면 매치되도록
    // EXISTS 서브쿼리로 검색한다.
    @Query("""
        SELECT s FROM ChatSession s
        WHERE s.userId = :userId
        AND (:countryCode = '' OR s.countryCode = :countryCode)
        AND (:keyword = '' OR LOWER(s.sessionTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR EXISTS (
                 SELECT 1 FROM ChatMessage m
                 WHERE m.sessionId = s.sessionId AND LOWER(m.messageContent) LIKE LOWER(CONCAT('%', :keyword, '%'))
             ))
        ORDER BY s.updatedAt DESC
        """)
    Page<ChatSession> search(
        @Param("userId") Long userId,
        @Param("countryCode") String countryCode,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
