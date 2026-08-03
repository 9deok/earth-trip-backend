package com.earthtrip.planning.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PlanningResource {
    private final UUID id; private final UUID tripId; private final String type; private UUID parentId;
    private LocalDate localDate; private Map<String,Object> payload; private String status; private int sortOrder;
    private final UUID createdBy; private UUID updatedBy; private final Instant createdAt; private Instant updatedAt;
    private Instant deletedAt; private final long version;
    private PlanningResource(
        UUID id,UUID tripId,String type,UUID parentId,LocalDate localDate,Map<String,Object> payload,
        String status,int sortOrder,UUID createdBy,UUID updatedBy,Instant createdAt,Instant updatedAt,
        Instant deletedAt,long version
    ){
        this.id=Objects.requireNonNull(id);this.tripId=Objects.requireNonNull(tripId);this.type=text(type,50);
        this.parentId=parentId;this.createdBy=Objects.requireNonNull(createdBy);this.createdAt=Objects.requireNonNull(createdAt);
        this.version=version;apply(localDate,payload,status,sortOrder,updatedBy,updatedAt);this.deletedAt=deletedAt;
    }
    public static PlanningResource create(UUID id,UUID tripId,String type,UUID parent,LocalDate date,Map<String,Object> payload,String status,int order,UUID actor,Instant now){return new PlanningResource(id,tripId,type,parent,date,payload,status,order,actor,actor,now,now,null,0);}
    public static PlanningResource restore(UUID id,UUID tripId,String type,UUID parent,LocalDate date,Map<String,Object> payload,String status,int order,UUID createdBy,UUID updatedBy,Instant createdAt,Instant updatedAt,Instant deletedAt,long version){return new PlanningResource(id,tripId,type,parent,date,payload,status,order,createdBy,updatedBy,createdAt,updatedAt,deletedAt,version);}
    public void update(LocalDate date,Map<String,Object> data,String state,Integer order,UUID actor,Instant now){apply(date==null?localDate:date,data==null?payload:data,state==null?status:state,order==null?sortOrder:order,actor,now);}
    public void relocate(UUID parent,LocalDate date,int order,UUID actor,Instant now){parentId=Objects.requireNonNull(parent);apply(Objects.requireNonNull(date),payload,status,order,actor,now);}
    public void delete(UUID actor,Instant now){deletedAt=now;updatedBy=actor;updatedAt=now;}
    private void apply(LocalDate date,Map<String,Object> data,String state,int order,UUID actor,Instant now){if(order<0)throw new IllegalArgumentException("정렬 순서는 0 이상이어야 합니다.");localDate=date;payload=Map.copyOf(Objects.requireNonNull(data));status=text(state==null?"ACTIVE":state,40).toUpperCase(java.util.Locale.ROOT);sortOrder=order;updatedBy=Objects.requireNonNull(actor);updatedAt=Objects.requireNonNull(now);}
    private static String text(String value,int max){if(value==null||value.isBlank()||value.strip().length()>max)throw new IllegalArgumentException("필수 문자열 값을 확인해 주세요.");return value.strip();}
    public UUID id(){return id;}public UUID tripId(){return tripId;}public String type(){return type;}public UUID parentId(){return parentId;}public LocalDate localDate(){return localDate;}public Map<String,Object> payload(){return payload;}public String status(){return status;}public int sortOrder(){return sortOrder;}public UUID createdBy(){return createdBy;}public UUID updatedBy(){return updatedBy;}public Instant createdAt(){return createdAt;}public Instant updatedAt(){return updatedAt;}public Instant deletedAt(){return deletedAt;}public long version(){return version;}
}
