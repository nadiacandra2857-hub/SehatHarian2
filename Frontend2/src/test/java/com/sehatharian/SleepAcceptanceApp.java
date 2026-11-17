package com.sehatharian;
import com.sehatharian.context.ApplicationContext;
import com.sehatharian.repository.*;
import com.sehatharian.service.*;
import com.sehatharian.util.InstanceAudioOutput;
import java.time.*;
import java.nio.file.*;
/** Explicit test launcher only: controlled reminder clock, real SQL and real browser audio. */
public final class SleepAcceptanceApp extends MainApp {
 @Override protected ApplicationContext createContext(PenggunaRepository accounts){
  if(!System.getenv("DB_NAME").startsWith("sehatharian_test_sleep_"))throw new SecurityException("Test schema required");
  var file=Path.of(System.getenv("SLEEP_TEST_CLOCK"));
  Clock clock=new Clock(){public ZoneId getZone(){return PengingatTidurService.ZONE;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){try{return Instant.parse(Files.readString(file).trim());}catch(Exception error){throw new IllegalStateException("Test clock unavailable");}}};
  // Faster polling only in this guarded acceptance launcher; production stays at five seconds.
  ReminderScheduler scheduler=new ReminderScheduler(){
   final java.util.concurrent.ScheduledExecutorService worker=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
   public void execute(Runnable work){worker.execute(work);}
   public Runnable everyFiveSeconds(Runnable work){var task=worker.scheduleWithFixedDelay(work,200,200,java.util.concurrent.TimeUnit.MILLISECONDS);return ()->task.cancel(false);}
   public void close(){worker.shutdownNow();}
  };
  return ApplicationContext.withScheduleFactory(accounts,accounts,SqlSleepScheduleProvider::new,scheduler,clock,javafx.application.Platform::runLater,new InstanceAudioOutput());
 }
}
