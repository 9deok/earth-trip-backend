package com.earthtrip.platform.application.service.provider;

import com.earthtrip.planning.api.PlanningComparisonTarget;
import com.earthtrip.platform.application.port.in.ExternalTravelUseCase;
import com.earthtrip.platform.application.port.out.ExternalTravelProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.trip.api.TripStructureView;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class ExternalTravelService implements ExternalTravelUseCase {
    private final ExternalTravelProviderPort provider;private final TripAccess access;private final TripStructureView structures;private final PlanningComparisonTarget comparisons;private final Clock clock;
    ExternalTravelService(ExternalTravelProviderPort p,TripAccess a,TripStructureView s,PlanningComparisonTarget c,Clock clock){provider=p;access=a;structures=s;comparisons=c;this.clock=clock;}
    @Override public List<GeoResult>forward(String query,String language,Integer limit){String q=text(query,"GEOCODING_QUERY_REQUIRED","주소 검색어가 필요합니다.",300);int n=limit==null?10:limit;if(n<1||n>20)throw bad("INVALID_GEOCODING_LIMIT","지오코딩 결과는 1~20개까지 요청할 수 있습니다.");return provider.forward(q,language(language),n);}
    @Override public GeoResult reverse(BigDecimal lat,BigDecimal lon,String language){coordinates(lat,lon);return provider.reverse(lat,lon,language(language));}
    @Override public PlaceUrlResult resolvePlaceUrl(String url,String language){return provider.resolvePlaceUrl(safeUrl(url),language(language));}
    @Override public List<TimeZoneResult>timeZones(BigDecimal lat,BigDecimal lon,String query,Integer limit){if(lat!=null||lon!=null){coordinates(lat,lon);return List.of(provider.timeZone(lat,lon));}int n=limit==null?50:limit;if(n<1||n>200)throw bad("INVALID_TIME_ZONE_LIMIT","시간대 결과는 1~200개까지 요청할 수 있습니다.");String q=query==null?"":query.strip().toLowerCase(Locale.ROOT);return ZoneId.getAvailableZoneIds().stream().filter(id->q.isBlank()||id.toLowerCase(Locale.ROOT).contains(q)).sorted().limit(n).map(id->new TimeZoneResult(id,id,"IANA_TZDB")).toList();}
    @Override public List<TransportStatusResult>transportStatuses(List<String>references,Instant observedAt){if(references==null||references.isEmpty()||references.size()>20)throw bad("INVALID_TRANSPORT_REFERENCES","교통편 참조번호를 1~20개 입력해 주세요.");List<String>normalized=references.stream().map(v->text(v,"INVALID_TRANSPORT_REFERENCE","교통편 참조번호를 확인해 주세요.",80)).distinct().toList();return provider.transportStatuses(normalized,observedAt);}
    @Override public List<InformationResult>emergencyInformation(UUID trip,UUID actor,String language){access.requireViewer(trip,actor);List<String>countries=countries(trip,actor);return countries.isEmpty()?List.of():provider.emergency(countries,language(language));}
    @Override public List<InformationResult>travelAdvisories(UUID trip,UUID actor,String language){access.requireViewer(trip,actor);List<String>countries=countries(trip,actor);return countries.isEmpty()?List.of():provider.advisories(countries,language(language));}
    @Override public ComparisonRefreshResult refreshComparison(UUID trip,UUID option,UUID actor,long baseVersion){access.requireEditor(trip,actor);PlanningComparisonTarget.Target current=comparisons.get(trip,option,actor);if(current.version()!=baseVersion)throw new EarthTripException("VERSION_CONFLICT",409,"다른 비교안 변경이 먼저 저장되었습니다.",Map.of("serverVersion",current.version()));Map<String,Object>fields=provider.refreshComparison(current.payload());PlanningComparisonTarget.Target updated=comparisons.applyRefresh(trip,option,actor,fields,baseVersion);return new ComparisonRefreshResult(updated.optionId(),updated.payload(),updated.status(),updated.version(),clock.instant());}
    private List<String>countries(UUID trip,UUID actor){return structures.snapshot(trip,actor).segments().stream().map(TripStructureView.Segment::countryCode).filter(java.util.Objects::nonNull).map(v->v.strip().toUpperCase(Locale.ROOT)).filter(v->v.matches("[A-Z]{2}")).distinct().toList();}
    private static void coordinates(BigDecimal lat,BigDecimal lon){if(lat==null||lon==null||lat.compareTo(BigDecimal.valueOf(-90))<0||lat.compareTo(BigDecimal.valueOf(90))>0||lon.compareTo(BigDecimal.valueOf(-180))<0||lon.compareTo(BigDecimal.valueOf(180))>0)throw bad("INVALID_COORDINATES","위도는 -90~90, 경도는 -180~180 사이여야 합니다.");}
    private static String safeUrl(String value){try{URI uri=new URI(text(value,"URL_REQUIRED","장소 공유 URL이 필요합니다.",2048));String scheme=uri.getScheme()==null?"":uri.getScheme().toLowerCase(Locale.ROOT);String host=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.ROOT);if((!scheme.equals("http")&&!scheme.equals("https"))||host.isBlank()||uri.getUserInfo()!=null||host.equals("localhost")||host.endsWith(".local")||host.matches("127\\..*")||host.matches("10\\..*")||host.matches("192\\.168\\..*")||host.matches("169\\.254\\..*"))throw bad("UNSAFE_PLACE_URL","외부 HTTP(S) 장소 URL만 해석할 수 있습니다.");return uri.normalize().toString();}catch(URISyntaxException e){throw bad("INVALID_PLACE_URL","장소 URL 형식을 확인해 주세요.");}}
    private static String language(String v){String n=v==null||v.isBlank()?"ko":v.strip();if(!n.matches("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?"))throw bad("INVALID_LANGUAGE","언어 태그 형식을 확인해 주세요.");return n;}private static String text(String v,String c,String m,int max){if(v==null||v.isBlank()||v.strip().length()>max)throw bad(c,m);return v.strip();}private static EarthTripException bad(String c,String m){return EarthTripException.badRequest(c,m);}
}
