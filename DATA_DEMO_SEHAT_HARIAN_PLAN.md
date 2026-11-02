## Goal Description
Create a safe, isolated demo data set for SehatHarian that includes:
- A dedicated demo database (separate from production) with explicit configuration.
- One synthetic user account with BCrypt‑hashed password, authenticable via the existing AuthService.
- 90 days of deterministic activity, hydration, food, sleep, and challenge records that produce realistic, varied charts.
- A reusable seed utility (Java/Gradle task) that runs in a transaction, is idempotent, and provides a dry‑run summary.
- Documentation (`docs/LAPORAN_DATA_DEMO.md`) with sanitized target, period, record counts, verification steps, and screenshots.
- Update the register of pending jobs to reflect the new demo‑seed task.
All operations must **never** modify production data or schema.

---
## User Review Required
> [!IMPORTANT] The demo database name, credentials, and file path must be confirmed before seeding. Please verify the proposed values.
> - Database name: `sehatharian_demo_db`
> - JDBC URL (in `Backend2/src/main/resources/db_demo.properties`): `jdbc:mysql://127.0.0.1:13307/sehatharian_demo_db?serverTimezone=Asia/Jakarta&useSSL=false`
> - User: `demo_user` (no password for local dev, can be empty).
> - Ensure any existing database with this name is **not** a production instance.

---
## Open Questions
> [!WARNING] **Timezone handling** – The application uses `Asia/Jakarta`. Do you want the seed timestamps to be generated in Java using `ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))` or pre‑computed strings?
> 
> > Please confirm that using the system timezone (already set to Asia/Jakarta) is acceptable.

---
## Proposed Changes
### Database Configuration
#### [NEW] `Backend2/src/main/resources/db_demo.properties`
```properties
# Demo database configuration (isolated from production)
db.host=127.0.0.1
db.port=13307
db.name=sehatharian_demo_db
db.user=demo_user
db.password=
db.useSSL=false
db.allowPublicKeyRetrieval=true
db.serverTimezone=Asia/Jakarta
```
#### [MODIFY] `Backend2/src/main/java/com/sehatharian/config/DatabaseConfig.java`
Add a method `DataSource demoDataSource()` that loads the above properties when the environment variable `SEHAT_DEMO` is set to `true`. Existing `dataSource()` remains unchanged for production.

---
### Seed Utility
#### [NEW] `Backend2/src/main/java/com/sehatharian/util/DemoSeed.java`
```java
package com.sehatharian.util;

import com.sehatharian.service.*;
import com.sehatharian.model.*;
import com.sehatharian.repository.*;
import com.sehatharian.config.DatabaseConfig;
import java.time.*;
import java.util.*;

/**
 * Generates deterministic demo data for a single synthetic user.
 * All INSERTs are performed inside a single transaction; on failure the transaction rolls back.
 * The class can be executed from a Gradle task or directly via `java -cp ... DemoSeed`.
 */
public final class DemoSeed {
    private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
    private static final LocalDate END_DATE = LocalDate.now(ZONE);
    private static final LocalDate START_DATE = END_DATE.minusDays(89); // 90 days inclusive
    private static final String DEMO_EMAIL = "demo@sehatharian.local";
    private static final String DEMO_PASSWORD = "DemoPass123"; // will be BCrypt‑hashed

    public static void main(String[] args) {
        boolean dryRun = Arrays.asList(args).contains("--dry-run");
        DemoSeed seed = new DemoSeed();
        seed.run(dryRun);
    }

    private void run(boolean dryRun) {
        // Obtain demo DataSource via DatabaseConfig.demoDataSource()
        try (var ds = DatabaseConfig.demoDataSource()) {
            var tx = ds.getConnection().setAutoCommit(false);
            try {
                var userService = new PenggunaService(ds);
                var authService = new AuthService(ds);
                var aktivitasService = new AktivitasService(ds);
                var hidrasiService = new HidrasiService(ds);
                var makanService = new KebiasaanMakanService(ds);
                var tidurService = new TidurService(ds);
                var tantanganService = new TantanganService(ds);

                // 1. Create synthetic user if not exists
                Pengguna demoUser = userService.findByEmail(DEMO_EMAIL)
                        .orElseGet(() -> {
                            Pengguna p = new Pengguna();
                            p.setNama("Demo User");
                            p.setEmail(DEMO_EMAIL);
                            p.setPasswordHash(authService.hashPassword(DEMO_PASSWORD));
                            p.setGolonganUsia("dewasa");
                            p.setGolonganKesibukan("luar_rumah");
                            p.setWaktuLuangMenit(30);
                            return userService.save(p);
                        });

                // 2. Generate daily records
                Random rng = new Random(0); // deterministic seed
                LocalDate cur = START_DATE;
                while (!cur.isAfter(END_DATE)) {
                    // Randomly decide if the day has any records (10% gap days)
                    if (rng.nextInt(100) < 10) { cur = cur.plusDays(1); continue; }

                    // Aktivitas – varied intensity
                    if (rng.nextBoolean()) {
                        Aktivitas a = new Aktivitas();
                        a.setPenggunaId(demoUser.getId());
                        a.setTanggal(java.sql.Date.valueOf(cur));
                        a.setJenisAktivitas("Jalan Kaki");
                        a.setDurasiMenit(10 + rng.nextInt(21)); // 10‑30 min
                        a.setKeterangan("Demo record");
                        aktivitasService.save(a);
                    }

                    // Hidrasi – 2‑4 entries per day
                    int drinks = 2 + rng.nextInt(3);
                    for (int i = 0; i < drinks; i++) {
                        Hidrasi h = new Hidrasi();
                        h.setPenggunaId(demoUser.getId());
                        h.setTanggal(java.sql.Date.valueOf(cur));
                        h.setJumlahMl(200 + rng.nextInt(301)); // 200‑500 ml
                        h.setCatatan("Demo water");
                        hidrasiService.save(h);
                    }

                    // Kebiasaan Makan – breakfast, lunch, dinner, optional snack
                    String[] meals = {"sarapan", "makan_siang", "makan_malam"};
                    for (String meal : meals) {
                        KebiasaanMakan km = new KebiasaanMakan();
                        km.setPenggunaId(demoUser.getId());
                        km.setTanggal(java.sql.Date.valueOf(cur));
                        km.setWaktuMakan(meal);
                        km.setMenu(meal.equals("sarapan") ? "Bubur" : meal.equals("makan_siang") ? "Nasi Goreng" : "Ayam Bakar");
                        km.setKeterangan("Demo");
                        makanService.save(km);
                    }
                    // optional snack 30% chance
                    if (rng.nextInt(100) < 30) {
                        KebiasaanMakan snack = new KebiasaanMakan();
                        snack.setPenggunaId(demoUser.getId());
                        snack.setTanggal(java.sql.Date.valueOf(cur));
                        snack.setWaktuMakan("camilan");
                        snack.setMenu("Buah");
                        snack.setKeterangan("Demo snack");
                        makanService.save(snack);
                    }

                    // Tidur – cross‑midnight handling
                    LocalTime bedTime = LocalTime.of(22, 0).plusMinutes(rng.nextInt(61)); // 22:00‑23:00
                    int sleepHours = 5 + rng.nextInt(4); // 5‑8 hrs
                    LocalTime wakeTime = bedTime.plusHours(sleepHours).plusMinutes(rng.nextInt(31));
                    // Adjust date for wake‑time if it passes midnight
                    LocalDate wakeDate = wakeTime.isBefore(bedTime) ? cur.plusDays(1) : cur;
                    Tidur t = new Tidur();
                    t.setPenggunaId(demoUser.getId());
                    t.setTanggal(java.sql.Date.valueOf(cur));
                    t.setJamTidur(java.sql.Time.valueOf(bedTime));
                    t.setJamBangun(java.sql.Time.valueOf(wakeTime));
                    t.setDurasiJam((double) sleepHours + rng.nextDouble());
                    t.setMemenuhiStandar(t.getDurasiJam() >= 5.0);
                    tidurService.save(t);

                    // Tantangan – use existing challenge IDs (query first 5 IDs)
                    List<Integer> challengeIds = tantanganService.findFirstNIds(5);
                    for (int cid : challengeIds) {
                        RiwayatTantangan rt = new RiwayatTantangan();
                        rt.setPenggunaId(demoUser.getId());
                        rt.setTantanganId(cid);
                        rt.setTanggal(java.sql.Date.valueOf(cur));
                        rt.setDurasiTargetMenit(15);
                        // Status progression: early days "disarankan", middle improves, later more "selesai"
                        String status;
                        long dayIndex = ChronoUnit.DAYS.between(START_DATE, cur);
                        if (dayIndex < 30) status = "disarankan";
                        else if (dayIndex < 60) status = rng.nextBoolean() ? "selesai" : "disarankan";
                        else status = "selesai";
                        rt.setStatus(status);
                        tantanganService.saveRiwayat(rt);
                    }

                    cur = cur.plusDays(1);
                }

                if (dryRun) {
                    System.out.println("--- DRY RUN SUMMARY ---");
                    System.out.println("Demo user email: " + DEMO_EMAIL);
                    System.out.println("Period: " + START_DATE + " to " + END_DATE);
                    System.out.println("Records will be inserted inside one transaction.");
                } else {
                    ds.getConnection().commit();
                    System.out.println("Demo data seeded successfully.");
                }
            } catch (Exception e) {
                ds.getConnection().rollback();
                throw new RuntimeException("Demo seed failed, transaction rolled back", e);
            }
        }
    }
}
```

---
### Gradle Task
#### [MODIFY] `Backend2/build.gradle`
Add a task `demoSeed` that runs the above class.
```groovy
task demoSeed(type: JavaExec) {
    group = "demo"
    description = "Seed isolated demo database with deterministic data"
    classpath = sourceSets.main.runtimeClasspath
    mainClass.set('com.sehatharian.util.DemoSeed')
    args '--dry-run' // default to dry‑run; user can pass "--execute" to actually write
    environment "SEHAT_DEMO", "true"
}
```

---
### Documentation
#### [NEW] `docs/LAPORAN_DATA_DEMO.md`
Will contain:
- Demo DB connection details (sanitized).
- Period (START‑END) and total record counts per table (generated by dry‑run).
- Screenshots of UI (desktop & JPro mobile) after seeding (to be captured manually).
- Limitations & known issues.
- Instructions to run seed (`./gradlew :Backend2:demoSeed --args='--execute'`).
- How to revert to production config.

---
### Register Update
#### [MODIFY] `scripts/prepare-repdb.py` (or appropriate register file)
Add an entry for the new demo‑seed task under "Pending Jobs" with description and command.

---
## Verification Plan
### Automated Tests
1. Run existing unit tests to ensure no regression:
```
./gradlew test
```
2. Add a new integration test `DemoSeedIntegrationTest` that:
   - Starts an in‑memory MariaDB (or Docker container) with the demo schema.
   - Executes `DemoSeed.main(new String[]{"--dry-run"})` and asserts the printed summary matches expected counts.
   - Executes with `--execute` and verifies row counts via repository reads.
### Manual Verification
1. Start the application with demo DB:
```
export SEHAT_DEMO=true
./gradlew :Frontend2:run
```
2. Log in using `demo@sehatharian.local` / `DemoPass123`.
3. Navigate to the weekly consistency chart and verify:
   - Data covers the full 90‑day period.
   - Early days show irregular entries, middle improves, later more consistent.
   - Days without records appear as gaps, not zero values.
4. Capture screenshots (desktop & JPro mobile) and place them under `Frontend2/docs/screenshots/demo/`.
5. Ensure the UI displays a label “Data demo sintetis” in the status bar.

---
## Walkthrough (to be created after implementation)
Will summarise changes, test results, screenshots, and any remaining limitations.
