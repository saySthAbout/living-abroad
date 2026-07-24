package com.livingabroad.backend.service;

import com.livingabroad.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenCleanupScheduler scheduler;

    @Test
    void cleanupDeletesExpiredOrRevokedTokensAsOfNow() {
        when(refreshTokenRepository.deleteExpiredOrRevoked(any())).thenReturn(3);

        scheduler.cleanup();

        ArgumentCaptor<OffsetDateTime> nowCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(refreshTokenRepository).deleteExpiredOrRevoked(nowCaptor.capture());
        assertThat(nowCaptor.getValue()).isCloseTo(OffsetDateTime.now(), org.assertj.core.api.Assertions.within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void cleanupDoesNotThrowWhenNothingToDelete() {
        when(refreshTokenRepository.deleteExpiredOrRevoked(any())).thenReturn(0);

        scheduler.cleanup();

        verify(refreshTokenRepository).deleteExpiredOrRevoked(any());
    }
}
