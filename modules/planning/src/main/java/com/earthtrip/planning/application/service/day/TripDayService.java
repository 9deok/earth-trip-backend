package com.earthtrip.planning.application.service.day;
import com.earthtrip.planning.application.port.in.TripDayUseCase;import com.earthtrip.sharedkernel.error.EarthTripException;import com.earthtrip.trip.api.TripAccess;import java.nio.charset.StandardCharsets;import java.time.LocalDate;import java.util.*;import java.util.stream.IntStream;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true) class TripDayService implements TripDayUseCase{
    private final TripAccess access;TripDayService(TripAccess a){access=a;}
    @Override public List<DayResult> list(UUID trip,UUID actor){access.requireViewer(trip,actor);TripAccess.PublicTripResult info=access.publicInfo(trip);if(info.startDate()==null)return List.of();long count=java.time.temporal.ChronoUnit.DAYS.between(info.startDate(),info.endDate())+1;return IntStream.range(0,(int)count).mapToObj(i->{LocalDate date=info.startDate().plusDays(i);return new DayResult(id(trip,date),date,i+1,info.timeZone());}).toList();}
    @Override public DayResult requireDay(UUID trip,UUID day,UUID actor){return list(trip,actor).stream().filter(d->d.dayId().equals(day)).findFirst().orElseThrow(()->EarthTripException.notFound("TRIP_DAY_NOT_FOUND","여행 날짜를 찾을 수 없습니다."));}
    private static UUID id(UUID trip,LocalDate date){return UUID.nameUUIDFromBytes((trip+":"+date).getBytes(StandardCharsets.UTF_8));}
}
