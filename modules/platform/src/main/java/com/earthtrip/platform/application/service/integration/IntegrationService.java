package com.earthtrip.platform.application.service.integration;

import com.earthtrip.platform.application.port.in.IntegrationUseCase;
import com.earthtrip.platform.application.port.out.IntegrationStorePort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class IntegrationService implements IntegrationUseCase {

    private final IntegrationStorePort store;
    private final TripAccess access;
    private final Clock clock;

    IntegrationService(IntegrationStorePort store, TripAccess access, Clock clock) {
        this.store = store; this.access = access; this.clock = clock;
    }

    @Override @Transactional(readOnly=true)
    public List<ConnectionResult> connections(UUID userId,String kind){
        return store.connections(userId,kind(kind)).stream().map(IntegrationService::result).toList();
    }
    @Override public ConnectionResult createConnection(UUID userId,String kind,ConnectionCommand c){
        String normalizedKind=kind(kind);if(c==null||c.requestId()==null)throw bad("REQUEST_ID_REQUIRED","requestId가 필요합니다.");
        IntegrationStorePort.ConnectionRecord old=store.connection(c.requestId()).orElse(null);if(old!=null){scope(old,userId,normalizedKind);return result(old);}
        String provider=provider(c.provider());if(c.authorizationCode()!=null&&!c.authorizationCode().isBlank())throw EarthTripException.unavailable("INTEGRATION_PROVIDER_NOT_CONFIGURED","외부 연동 제공자 자격증명이 아직 설정되지 않았습니다.");
        Instant now=clock.instant();String state=Base64.getUrlEncoder().withoutPadding().encodeToString((c.requestId()+":"+UUID.randomUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return result(store.saveConnection(new IntegrationStorePort.ConnectionRecord(c.requestId(),userId,normalizedKind,provider,"AUTHORIZATION_REQUIRED",scopes(c.scopes()),map(c.metadata()),state,now.plus(Duration.ofMinutes(10)),null,"PROVIDER_NOT_CONFIGURED",now,now,null,0)));
    }
    @Override @Transactional(readOnly=true) public ConnectionResult connection(UUID userId,UUID id,String kind){return result(loadConnection(userId,id,kind(kind)));}
    @Override public void deleteConnection(UUID userId,UUID id,String kind,long baseVersion){IntegrationStorePort.ConnectionRecord c=loadConnection(userId,id,kind(kind));version(c.version(),baseVersion);Instant now=clock.instant();store.saveConnection(new IntegrationStorePort.ConnectionRecord(c.id(),c.userId(),c.kind(),c.provider(),"REVOKED",c.scopes(),c.metadata(),null,null,c.lastSuccessAt(),null,c.createdAt(),now,now,c.version()));}
    @Override public SyncJobResult syncConnection(UUID userId,UUID id,UUID requestId,Map<String,Object> payload){IntegrationStorePort.ConnectionRecord c=loadConnection(userId,id,"GENERAL");return sync(requestId,userId,id,null,"CONNECTION_SYNC",c,payload);}
    @Override @Transactional(readOnly=true) public SyncJobResult syncJob(UUID userId,UUID jobId){IntegrationStorePort.SyncRecord job=store.sync(jobId).filter(j->j.userId().equals(userId)).orElseThrow(IntegrationService::syncNotFound);return result(job);}
    @Override @Transactional(readOnly=true) public List<AliasResult> aliases(UUID userId){return store.aliases(userId).stream().map(IntegrationService::result).toList();}
    @Override public AliasResult createAlias(UUID userId,UUID requestId){if(requestId==null)throw bad("REQUEST_ID_REQUIRED","requestId가 필요합니다.");IntegrationStorePort.AliasRecord old=store.alias(requestId).orElse(null);if(old!=null){if(!old.userId().equals(userId))throw conflict();return result(old);}Instant now=clock.instant();String alias="trip-"+requestId.toString().replace("-","").substring(0,16)+"@inbound.earthtrip.app";return result(store.saveAlias(new IntegrationStorePort.AliasRecord(requestId,userId,alias,"PROVIDER_NOT_CONFIGURED",now,null,0)));}
    @Override public void deleteAlias(UUID userId,UUID aliasId,long baseVersion){IntegrationStorePort.AliasRecord a=store.alias(aliasId).filter(x->x.userId().equals(userId)&&x.revokedAt()==null).orElseThrow(()->badNotFound("INBOUND_ALIAS_NOT_FOUND","예약 메일 주소를 찾을 수 없습니다."));version(a.version(),baseVersion);store.saveAlias(new IntegrationStorePort.AliasRecord(a.id(),a.userId(),a.alias(),"REVOKED",a.createdAt(),clock.instant(),a.version()));}
    @Override public SyncJobResult reservationMailSync(UUID userId,UUID requestId,UUID connectionId,Map<String,Object> payload){IntegrationStorePort.ConnectionRecord c=connectionId==null?null:loadConnection(userId,connectionId,"GENERAL");return sync(requestId,userId,connectionId,null,"RESERVATION_MAIL_SYNC",c,payload);}
    @Override @Transactional(readOnly=true) public CalendarSyncResult calendar(UUID tripId,UUID actor){access.requireViewer(tripId,actor);return calendarResult(store.calendar(tripId).orElseThrow(()->badNotFound("CALENDAR_SYNC_NOT_FOUND","캘린더 동기화 설정을 찾을 수 없습니다.")));}
    @Override public CalendarSyncResult putCalendar(UUID tripId,UUID actor,CalendarCommand command){access.requireEditor(tripId,actor);if(command==null||command.connectionId()==null)throw bad("CALENDAR_CONNECTION_REQUIRED","캘린더 연결이 필요합니다.");IntegrationStorePort.ConnectionRecord c=loadConnection(actor,command.connectionId(),"GENERAL");IntegrationStorePort.CalendarRecord current=store.calendar(tripId).orElse(null);if(current!=null)version(current.version(),command.baseVersion());else version(0,command.baseVersion());Instant now=clock.instant();return calendarResult(store.saveCalendar(new IntegrationStorePort.CalendarRecord(tripId,c.id(),map(command.scopeConfig()),c.status().equals("ACTIVE")?"ACTIVE":"REAUTHORIZATION_REQUIRED",actor,current==null?now:current.createdAt(),now,current==null?0:current.version())));}
    @Override public void deleteCalendar(UUID tripId,UUID actor,long baseVersion){access.requireEditor(tripId,actor);IntegrationStorePort.CalendarRecord c=store.calendar(tripId).orElseThrow(()->badNotFound("CALENDAR_SYNC_NOT_FOUND","캘린더 동기화 설정을 찾을 수 없습니다."));version(c.version(),baseVersion);store.deleteCalendar(tripId);}
    @Override public SyncJobResult runCalendar(UUID tripId,UUID actor,UUID requestId){access.requireEditor(tripId,actor);IntegrationStorePort.CalendarRecord config=store.calendar(tripId).orElseThrow(()->badNotFound("CALENDAR_SYNC_NOT_FOUND","캘린더 동기화 설정을 찾을 수 없습니다."));IntegrationStorePort.ConnectionRecord c=loadConnection(actor,config.connectionId(),"GENERAL");return sync(requestId,actor,c.id(),tripId,"CALENDAR_SYNC",c,config.scopeConfig());}
    @Override @Transactional(readOnly=true) public SyncJobResult calendarRun(UUID tripId,UUID runId,UUID actor){access.requireViewer(tripId,actor);IntegrationStorePort.SyncRecord job=store.sync(runId).filter(j->tripId.equals(j.tripId())&&j.userId().equals(actor)&&j.jobType().equals("CALENDAR_SYNC")).orElseThrow(IntegrationService::syncNotFound);return result(job);}
    @Override public SyncJobResult providerStatementImport(UUID tripId,UUID actor,UUID requestId,UUID connectionId,Map<String,Object> payload){access.requireEditor(tripId,actor);IntegrationStorePort.ConnectionRecord c=loadConnection(actor,connectionId,"FINANCIAL");return sync(requestId,actor,connectionId,tripId,"PROVIDER_STATEMENT_IMPORT",c,payload);}
    private SyncJobResult sync(UUID requestId,UUID userId,UUID connectionId,UUID tripId,String type,IntegrationStorePort.ConnectionRecord c,Map<String,Object> payload){if(requestId==null)throw bad("REQUEST_ID_REQUIRED","requestId가 필요합니다.");IntegrationStorePort.SyncRecord old=store.sync(requestId).orElse(null);if(old!=null){if(!old.userId().equals(userId)||!old.jobType().equals(type))throw conflict();return result(old);}boolean active=c!=null&&c.status().equals("ACTIVE");Instant now=clock.instant();return result(store.saveSync(new IntegrationStorePort.SyncRecord(requestId,userId,connectionId,tripId,type,active?"QUEUED":"REAUTHORIZATION_REQUIRED",map(payload),Map.of(),active?null:"PROVIDER_NOT_CONFIGURED",1,now,now,0)));}
    private IntegrationStorePort.ConnectionRecord loadConnection(UUID userId,UUID id,String kind){return store.connection(id).filter(c->c.userId().equals(userId)&&c.kind().equals(kind)&&c.revokedAt()==null).orElseThrow(()->badNotFound("INTEGRATION_CONNECTION_NOT_FOUND","외부 연결을 찾을 수 없습니다."));}
    private static String kind(String k){String v=k==null?"":k.strip().toUpperCase(Locale.ROOT);if(!Set.of("GENERAL","FINANCIAL").contains(v))throw bad("INVALID_CONNECTION_KIND","지원하지 않는 외부 연결 종류입니다.");return v;}private static String provider(String p){if(p==null||p.isBlank()||p.strip().length()>40)throw bad("INVALID_INTEGRATION_PROVIDER","외부 연결 제공자를 확인해 주세요.");return p.strip().toUpperCase(Locale.ROOT);}private static Set<String>scopes(Set<String>s){if(s==null)return Set.of();Set<String>r=new LinkedHashSet<>();s.stream().filter(java.util.Objects::nonNull).map(x->x.strip().toUpperCase(Locale.ROOT)).forEach(r::add);return Set.copyOf(r);}private static Map<String,Object>map(Map<String,Object>m){return m==null?Map.of():java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(m));}
    private static void scope(IntegrationStorePort.ConnectionRecord c,UUID user,String kind){if(!c.userId().equals(user)||!c.kind().equals(kind))throw conflict();}private static void version(long s,long b){if(s!=b)throw new EarthTripException("VERSION_CONFLICT",409,"다른 연동 변경이 먼저 저장되었습니다.",Map.of("serverVersion",s));}private static EarthTripException conflict(){return EarthTripException.conflict("IDEMPOTENCY_KEY_REUSED","이미 다른 연동 요청에 사용된 ID입니다.");}private static EarthTripException bad(String c,String m){return EarthTripException.badRequest(c,m);}private static EarthTripException badNotFound(String c,String m){return EarthTripException.notFound(c,m);}private static EarthTripException syncNotFound(){return badNotFound("INTEGRATION_SYNC_JOB_NOT_FOUND","연동 작업을 찾을 수 없습니다.");}
    private static ConnectionResult result(IntegrationStorePort.ConnectionRecord c){return new ConnectionResult(c.id(),c.kind(),c.provider(),c.status(),c.scopes(),c.metadata(),c.authorizationState(),c.authorizationExpiresAt(),c.lastSuccessAt(),c.errorCode(),false,c.createdAt(),c.updatedAt(),c.version());}private static SyncJobResult result(IntegrationStorePort.SyncRecord j){return new SyncJobResult(j.id(),j.connectionId(),j.tripId(),j.jobType(),j.status(),j.result(),j.errorCode(),j.attemptCount(),j.createdAt(),j.updatedAt(),j.version());}private static AliasResult result(IntegrationStorePort.AliasRecord a){return new AliasResult(a.id(),a.alias(),a.status(),false,a.createdAt(),a.version());}private static CalendarSyncResult calendarResult(IntegrationStorePort.CalendarRecord c){return new CalendarSyncResult(c.tripId(),c.connectionId(),c.scopeConfig(),c.status(),c.updatedAt(),c.version());}
}
