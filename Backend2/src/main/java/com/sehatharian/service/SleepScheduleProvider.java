package com.sehatharian.service;
import java.time.*;
@FunctionalInterface
public interface SleepScheduleProvider {
 record Schedule(LocalTime bedtime,boolean enabled,LocalDate from,LocalTime wake,String identity,boolean sound,double volume){
  public Schedule(LocalTime bedtime,boolean enabled){this(bedtime,enabled,LocalDate.of(1970,1,1),null,bedtime.toString(),true,1);}
  public Schedule{java.util.Objects.requireNonNull(bedtime);java.util.Objects.requireNonNull(from);java.util.Objects.requireNonNull(identity);}
 }
 Schedule latest(int userId)throws Exception;
 default boolean claim(int userId,Schedule schedule,LocalDateTime occurrence)throws Exception{return true;}
}
