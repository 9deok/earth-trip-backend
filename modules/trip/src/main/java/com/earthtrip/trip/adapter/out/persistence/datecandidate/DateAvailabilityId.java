package com.earthtrip.trip.adapter.out.persistence.datecandidate;
import java.io.Serializable;
import java.util.Objects;
@SuppressWarnings("serial") final class DateAvailabilityId implements Serializable{
    private String candidateId; private String userId; protected DateAvailabilityId(){}
    DateAvailabilityId(String c,String u){candidateId=c;userId=u;}
    @Override public boolean equals(Object o){return this==o||(o instanceof DateAvailabilityId that&&Objects.equals(candidateId,that.candidateId)&&Objects.equals(userId,that.userId));}
    @Override public int hashCode(){return Objects.hash(candidateId,userId);}
}
