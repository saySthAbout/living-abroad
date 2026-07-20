package com.livingabroad.backend.repository;

import com.livingabroad.backend.entity.ChatMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageSourceRepository extends JpaRepository<ChatMessageSource, Long> {

    List<ChatMessageSource> findByMessageIdIn(List<Long> messageIds);
}
