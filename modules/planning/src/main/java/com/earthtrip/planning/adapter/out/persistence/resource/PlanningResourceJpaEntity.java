package com.earthtrip.planning.adapter.out.persistence.resource;
import com.earthtrip.planning.domain.PlanningResource;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
@Entity @Table(name="planning_resources") class PlanningResourceJpaEntity{
    @Id @Column(name="id",nullable=false,length=36)private String id;
    @Column(name="trip_id",nullable=false,length=36)private String tripId;
    @Column(name="resource_type",nullable=false,length=50)private String resourceType;
    @Column(name="parent_id",length=36)private String parentId;
    @Column(name="local_date")private LocalDate localDate;
    @Column(name="payload",nullable=false,columnDefinition="JSON")private String payload;
    @Column(name="status",nullable=false,length=40)private String status;
    @Column(name="sort_order",nullable=false)private int sortOrder;
    @Column(name="created_by",nullable=false,length=36)private String createdBy;
    @Column(name="updated_by",nullable=false,length=36)private String updatedBy;
    @Column(name="created_at",nullable=false)private Instant createdAt;
    @Column(name="updated_at",nullable=false)private Instant updatedAt;
    @Column(name="deleted_at")private Instant deletedAt;
    @Version @Column(name="version",nullable=false)private long version;
    protected PlanningResourceJpaEntity(){}
    PlanningResourceJpaEntity(PlanningResource r,String json){id=r.id().toString();apply(r,json);}
    void apply(PlanningResource r,String json){tripId=r.tripId().toString();resourceType=r.type();parentId=r.parentId()==null?null:r.parentId().toString();localDate=r.localDate();payload=json;status=r.status();sortOrder=r.sortOrder();createdBy=r.createdBy().toString();updatedBy=r.updatedBy().toString();createdAt=r.createdAt();updatedAt=r.updatedAt();deletedAt=r.deletedAt();}
    PlanningResource toDomain(Map<String,Object> data){return PlanningResource.restore(UUID.fromString(id),UUID.fromString(tripId),resourceType,parentId==null?null:UUID.fromString(parentId),localDate,data,status,sortOrder,UUID.fromString(createdBy),UUID.fromString(updatedBy),createdAt,updatedAt,deletedAt,version);}
    String payload(){return payload;}
}
