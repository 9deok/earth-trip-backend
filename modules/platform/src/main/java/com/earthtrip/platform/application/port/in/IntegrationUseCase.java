package com.earthtrip.platform.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IntegrationUseCase {
    List<ConnectionResult> connections(UUID userId,String kind);
    ConnectionResult createConnection(UUID userId,String kind,ConnectionCommand command);
    ConnectionResult connection(UUID userId,UUID connectionId,String kind);
    void deleteConnection(UUID userId,UUID connectionId,String kind,long baseVersion);
    SyncJobResult syncConnection(UUID userId,UUID connectionId,UUID requestId,Map<String,Object> payload);
    SyncJobResult syncJob(UUID userId,UUID jobId);
    List<AliasResult> aliases(UUID userId);
    AliasResult createAlias(UUID userId,UUID requestId);
    void deleteAlias(UUID userId,UUID aliasId,long baseVersion);
    SyncJobResult reservationMailSync(UUID userId,UUID requestId,UUID connectionId,Map<String,Object> payload);
    CalendarSyncResult calendar(UUID tripId,UUID actorUserId);
    CalendarSyncResult putCalendar(UUID tripId,UUID actorUserId,CalendarCommand command);
    void deleteCalendar(UUID tripId,UUID actorUserId,long baseVersion);
    SyncJobResult runCalendar(UUID tripId,UUID actorUserId,UUID requestId);
    SyncJobResult calendarRun(UUID tripId,UUID runId,UUID actorUserId);
    SyncJobResult providerStatementImport(UUID tripId,UUID actorUserId,UUID requestId,UUID connectionId,Map<String,Object> payload);
    record ConnectionCommand(UUID requestId,String provider,Set<String> scopes,Map<String,Object> metadata,String authorizationCode,String redirectUri,String codeVerifier){}
    record ConnectionResult(UUID connectionId,String kind,String provider,String status,Set<String> scopes,Map<String,Object> metadata,String authorizationState,Instant authorizationExpiresAt,Instant lastSuccessAt,String errorCode,boolean providerConfigured,Instant createdAt,Instant updatedAt,long version){}
    record SyncJobResult(UUID jobId,UUID connectionId,UUID tripId,String jobType,String status,Map<String,Object> result,String errorCode,int attemptCount,Instant createdAt,Instant updatedAt,long version){}
    record AliasResult(UUID aliasId,String alias,String status,boolean providerConfigured,Instant createdAt,long version){}
    record CalendarCommand(UUID connectionId,Map<String,Object> scopeConfig,long baseVersion){}
    record CalendarSyncResult(UUID tripId,UUID connectionId,Map<String,Object> scopeConfig,String status,Instant updatedAt,long version){}
}
