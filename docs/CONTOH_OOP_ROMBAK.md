# OOP dari kode yang dipakai

| Konsep | Kelas dan metode | Cara menunjukkan |
| --- | --- | --- |
| Enkapsulasi | `Backend2/.../training/TrainingSession.java`: field private `phase`, `elapsed`, `active`, `steps`; `pause()`, `resume()`, `advance(long,boolean)` | UI tidak mengubah field timer langsung. Method memeriksa revision/phase dan menyediakan snapshot list immutable. |
| Enkapsulasi otorisasi | `model/SessionAccess.java`: `owner()`, `requireAdmin()`, `close()` | Owner/generation sesi tidak diterima dari form. Handler lama ditolak sesudah logout. |
| Overriding | `repository/ActualSleepRepository.java`: `getByRange(int,LocalDate,LocalDate)` dan `getRiwayatByPengguna(int,int)` memakai `@Override` | EvaluasiService bergantung antarmuka repository jadwal yang sudah ada, tetapi konstruksi produksi memasok adapter aktual. Jadwal legacy tetap di tabel semula. |
| Overloading | `nutrition/NutritionService.java`: `save(LocalDate,String,String,double)` dan `save(LocalDate,String,String,double,String)` | Jalur UI memasok expectedVersion untuk menolak katalog yang berubah sejak ditinjau. Overload empat argumen mendelegasikan ke implementasi bersama. |
| Overloading autentikasi | `service/AuthService.java`: `register` empat dan lima argumen | Varian empat argumen mendelegasikan validasi ke varian lima argumen dengan persetujuan. Tidak memberi parameter role. |
| Abstraksi dan pewarisan | `repository/OwnedRepository.java`: class abstract; `open()` | NutritionService, ContentService, HabitPreferences, HabitInsights, HabitChallengeService memakai jalur koneksi/otorisasi bersama. |
| Polimorfisme lifecycle | `AutoCloseable.close()` pada TrainingDialog, TrainingVideo, AdminController dan controller lain | SceneNavigator melepas controller melalui `instanceof AutoCloseable`, tanpa mengetahui detail player/editor. |
| Value object | `nutrition/Nutrients.java` dan `content/ContentService.java` record `Program`/`Step` | Map/list disalin; nilai hilang tetap hilang, riwayat memakai snapshot komposisi/sumber/versi. |

Path tabel disingkat dari `Backend2/src/main/java/com/sehatharian/`; kelas UI ada pada `Frontend2/src/main/java/com/sehatharian/`. Tunjukkan call site `TrainingDialog.render()`, `NutrisiController.review()`, `SceneNavigator.releaseController()` dan konstruktor `EvaluasiService` agar contoh terlihat benar-benar dipakai.
