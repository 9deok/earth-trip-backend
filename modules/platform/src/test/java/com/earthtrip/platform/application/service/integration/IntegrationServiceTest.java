package com.earthtrip.platform.application.service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.platform.application.port.out.ExternalAccountProviderPort;
import com.earthtrip.platform.application.port.out.IntegrationStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrationServiceTest {

    @Test
    void verifiesActiveConnectionAndPersistsSuccessfulSyncJob() {
        Fixture fixture = fixture();
        when(fixture.provider().configured()).thenReturn(true);
        when(fixture.provider().checkConnection(any()))
                .thenReturn(
                        new ExternalAccountProviderPort.ConnectionCheckResult(
                                "ACTIVE", Map.of("tokenRefresh", "SUCCEEDED")));

        IntegrationUseCase.SyncJobResult result =
                fixture.service()
                        .syncConnection(
                                fixture.userId(),
                                fixture.connectionId(),
                                UUID.randomUUID(),
                                Map.of());

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.result()).containsEntry("tokenRefresh", "SUCCEEDED");
        verify(fixture.provider()).checkConnection(any());
        verify(fixture.store()).saveConnection(any());
        verify(fixture.store()).saveSync(any());
    }

    @Test
    void marksConnectionForReauthorizationWhenRefreshGrantIsRejected() {
        Fixture fixture = fixture();
        when(fixture.provider().configured()).thenReturn(true);
        when(fixture.provider().checkConnection(any()))
                .thenThrow(
                        new EarthTripException(
                                "GOOGLE_CALENDAR_REAUTHORIZATION_REQUIRED",
                                409,
                                "다시 연결해야 합니다.",
                                Map.of()));

        IntegrationUseCase.SyncJobResult result =
                fixture.service()
                        .syncConnection(
                                fixture.userId(),
                                fixture.connectionId(),
                                UUID.randomUUID(),
                                Map.of());

        assertThat(result.status()).isEqualTo("REAUTHORIZATION_REQUIRED");
        assertThat(result.errorCode()).isEqualTo("GOOGLE_CALENDAR_REAUTHORIZATION_REQUIRED");
        verify(fixture.store())
                .saveConnection(
                        org.mockito.ArgumentMatchers.argThat(
                                connection ->
                                        connection.status().equals("REAUTHORIZATION_REQUIRED")
                                                && connection
                                                        .errorCode()
                                                        .equals(
                                                                "GOOGLE_CALENDAR_REAUTHORIZATION_REQUIRED")));
    }

    @Test
    void completesPendingOAuthConnectionWithSameIdempotencyKey() {
        IntegrationStorePort store = mock(IntegrationStorePort.class);
        IntegrationProviderRegistry providers = mock(IntegrationProviderRegistry.class);
        ExternalAccountProviderPort provider = mock(ExternalAccountProviderPort.class);
        CalendarSynchronizationService calendars = mock(CalendarSynchronizationService.class);
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-03T00:00:00Z");
        IntegrationStorePort.ConnectionRecord pending =
                new IntegrationStorePort.ConnectionRecord(
                        requestId,
                        userId,
                        "GENERAL",
                        "GOOGLE_CALENDAR",
                        "AUTHORIZATION_REQUIRED",
                        Set.of("CALENDAR_APP_CREATED"),
                        Map.of("displayName", "Google Calendar"),
                        "state",
                        createdAt.plusSeconds(600),
                        null,
                        null,
                        createdAt,
                        createdAt,
                        null,
                        0);
        when(store.connection(requestId)).thenReturn(Optional.of(pending));
        when(store.saveConnection(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providers.require("GOOGLE_CALENDAR")).thenReturn(provider);
        when(providers.configured("GOOGLE_CALENDAR")).thenReturn(true);
        when(provider.authorize(any()))
                .thenReturn(
                        new ExternalAccountProviderPort.AuthorizationResult(
                                Set.of("CALENDAR_APP_CREATED"),
                                Map.of("_refreshToken", "encrypted")));
        IntegrationService service =
                new IntegrationService(
                        store,
                        mock(TripAccess.class),
                        providers,
                        calendars,
                        Clock.fixed(createdAt.plusSeconds(30), ZoneOffset.UTC));

        IntegrationUseCase.ConnectionResult result =
                service.createConnection(
                        userId,
                        "GENERAL",
                        new IntegrationUseCase.ConnectionCommand(
                                requestId,
                                "GOOGLE_CALENDAR",
                                Set.of(),
                                Map.of(),
                                "authorization-code",
                                "https://app.earthtrip.test/oauth/calendar",
                                "verifier"));

        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.metadata())
                .containsEntry("displayName", "Google Calendar")
                .doesNotContainKey("_refreshToken");
        verify(provider).authorize(any());
        verify(store).saveConnection(any());
    }

    private static Fixture fixture() {
        IntegrationStorePort store = mock(IntegrationStorePort.class);
        IntegrationProviderRegistry providers = mock(IntegrationProviderRegistry.class);
        ExternalAccountProviderPort provider = mock(ExternalAccountProviderPort.class);
        UUID connectionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-04T00:00:00Z");
        IntegrationStorePort.ConnectionRecord connection =
                new IntegrationStorePort.ConnectionRecord(
                        connectionId,
                        userId,
                        "GENERAL",
                        "GOOGLE_CALENDAR",
                        "ACTIVE",
                        Set.of("CALENDAR_APP_CREATED"),
                        Map.of("_refreshToken", "encrypted"),
                        null,
                        null,
                        now.minusSeconds(300),
                        null,
                        now.minusSeconds(600),
                        now.minusSeconds(300),
                        null,
                        1);
        when(store.connection(connectionId)).thenReturn(Optional.of(connection));
        when(store.saveConnection(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(store.sync(any())).thenReturn(Optional.empty());
        when(store.saveSync(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providers.require("GOOGLE_CALENDAR")).thenReturn(provider);
        IntegrationService service =
                new IntegrationService(
                        store,
                        mock(TripAccess.class),
                        providers,
                        mock(CalendarSynchronizationService.class),
                        Clock.fixed(now, ZoneOffset.UTC));
        return new Fixture(service, store, provider, connectionId, userId);
    }

    private record Fixture(
            IntegrationService service,
            IntegrationStorePort store,
            ExternalAccountProviderPort provider,
            UUID connectionId,
            UUID userId) {}
}
