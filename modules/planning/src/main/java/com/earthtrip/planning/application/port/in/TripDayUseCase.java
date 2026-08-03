package com.earthtrip.planning.application.port.in;
import java.time.LocalDate;import java.util.*;
public interface TripDayUseCase{
    List<DayResult> list(UUID tripId,UUID actorUserId);DayResult requireDay(UUID tripId,UUID dayId,UUID actorUserId);
    record DayResult(UUID dayId,LocalDate localDate,int dayNumber,String timeZone){}
}
