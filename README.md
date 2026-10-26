# Verdant
> **"Catat kegiatan budidaya, ikuti panduan, dan lacak riwayat pertanian dengan mudah."**

Verdant adalah aplikasi pencatatan kegiatan budidaya dan panduan bertani berbasis web yang dirancang dengan pendekatan **User-Centered Design (UCD)** untuk petani dan mandor/penyuluh pertanian. Di balik antarmuka yang sederhana dan ramah pengguna, Verdant ditenagai oleh mesin *version-control* kekal (*immutable revision*) menggunakan paradigma **Functional Programming (FP)** murni di atas **Scala 3** dan **Cask**.

---

## 🌾 Filosofi Desain & Peran Pengguna

Verdant memisahkan kebutuhan dua kelompok pengguna tanpa membingungkan petani dengan istilah teknis rumit (tanpa *commit*, *diff*, *hash*, *branch*, *merge*, atau *repository*):

1. **Petani (Penyiram, Pemupuk, Pemanen di Lapangan):**
   - Mengakses aplikasi melalui ponsel pintar (*smartphone*).
   - Mencatat kegiatan harian di kebun dengan 1 ketukan tombol besar (**"Catat Kegiatan Baru"**).
   - Membaca instruksi dan takaran resmi tahap budidaya secara jelas dalam bahasa Indonesia.
2. **Mandor & Penyuluh Pertanian (Pengawas & Pembuat Kebijakan):**
   - Memperbarui rekomendasi takaran pupuk atau durasi tahap.
   - Meninjau ringkasan perubahan dalam bahasa manusia (**"Apa yang berubah?"**).
   - Menyetujui dan menerbitkan edisi panduan resmi baru (*Release*) untuk masa tanam mendatang.
   - Memeriksa silsilah perubahan dan memulihkan (*Revert*) panduan jika diperlukan.

---

## 🧭 Navigasi Berorientasi Tugas (Task-Oriented Navigation)

- **🏠 Beranda:** Dasbor praktis yang memprioritaskan masa tanam aktif, tombol aksi cepat *Catat Kegiatan*, tips agronomi hari ini, dan rangkuman kegiatan terkini petani.
- **📝 Catatan Budidaya:** Jurnal harian seluruh kegiatan petani di petak lahan dengan penyaringan (*filter*) berdasarkan masa tanam.
- **📖 Panduan Budidaya:** Katalog panduan budidaya per komoditas, petunjuk langkah demi langkah, serta area mandor untuk memperbarui draf panduan.
- **📅 Masa Tanam:** Pengelolaan siklus penanaman di lahan/greenhouse tertentu yang terkunci secara permanen pada edisi panduan resmi yang berlaku.
- **🕒 Riwayat:** Riwayat penerbitan edisi resmi dan silsilah revisi dengan tombol pemulihan versi (*Revert*).

---

## 🔄 Translasi Mekanisme Teknis Menjadi Nilai Pengguna

| Mekanisme Teknis (*Git/VCS*) | Istilah & Nilai di Verdant | Penjelasan Pengguna |
| :--- | :--- | :--- |
| **Commit** | **Simpan Revisi Draf** | Setiap perubahan panduan disimpan sebagai revisi internal yang abadi (*immutable*) tanpa menimpa data lama. |
| **Diff** | **"Apa yang Berubah?"** | Perbedaan parameter diterjemahkan menjadi kalimat ramah (contoh: *"Pada tahap Pembungaan, anjuran Konsentrasi Nutrisi EC dinaikkan dari 2.6 mS/cm menjadi 2.8 mS/cm"*). |
| **Review & Pull Request** | **Peninjauan Mandor Kebun** | Mandor membaca ringkasan perubahan sebelum panduan resmi berlaku di lapangan. |
| **Release / Tag** | **Publikasikan Panduan Resmi** | Mengunci revisi panduan menjadi edisi resmi (contoh: *Edisi 2026.1 - Musim Basah*). |
| **Revert** | **Kembalikan ke Versi Ini** | Memulihkan rekomendasi panduan ke versi terdahulu dengan satu klik konfirmasi. |
| **Batch Traceability** | **Masa Tanam Terkunci** | Setiap masa tanam (`MT-2026-TOMAT-01`) mengunci versi panduan yang digunakan sehingga catatan panen dapat diaudit (*audit-ready*). |

---

## 📐 Pemetaan Rubrik Pemrograman Fungsional (FP Rubric)

Seluruh logika inti Verdant mengikuti arsitektur **Functional Core / Imperative Shell** murni. Bagian inti (*Functional Core*) di `src/main/scala/verdant/core/` 100% bebas dari *side-effects* (tanpa I/O, tanpa database, tanpa dependensi waktu sistem).

| No | Persyaratan Rubrik FP | Implementasi Nyata di Verdant | Berkas & Lokasi Simbol |
| :--- | :--- | :--- | :--- |
| 1 | **Minimal 6 Fungsi Murni (*Pure Functions*)** | 1. `createGuideRevision` (Menghasilkan revisi panduan dengan SHA deterministik)<br/>2. `computeReadableDiff` (Menghitung perubahan draf dalam teks ramah)<br/>3. `restoreGuideRevision` (Menghitung pemulihan snapshot revisi lama)<br/>4. `getRevisionLineage` (Penelusuran silsilah mundur secara rekursif)<br/>5. `validateGuideForPublishing` (Validasi kelayakan edisi resmi)<br/>6. `summarizeGuide` (Agregasi metrik panduan dengan *foldLeft* & *groupBy*)<br/>7. `summarizeActivities` (Merangkum frekuensi kegiatan petani)<br/>8. `checkActivityCompliance` (Validasi takaran kegiatan terhadap batas aman) | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala) |
| 2 | **Minimal 3 *Immutable Case Classes*** | `AgronomicParam`, `CultivationStage`, `GuideSnapshot`, `GuideRevision`, `PublishedGuide`, `CultivationPeriod`, `CultivationActivity`, `ParamChange`, `ChangeSummary`, `GuideSummary` | [`Domain.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/Domain.scala) |
| 3 | **Nilai Fungsi Kelas-Pertama (*First-class function values*)** | `val isSafeParam: AgronomicParam => Boolean`<br/>`val isPositiveStage: CultivationStage => Boolean` | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala#L18-L24) |
| 4 | **Minimal 2 Ekspresi Lambda** | 1. `(s: CultivationStage) => s.durationDays`<br/>2. `(acc: Int, stage: CultivationStage) => acc + stage.durationDays`<br/>3. `(p: AgronomicParam) => p.name.toLowerCase` | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala#L244) |
| 5 | **Satu *Custom Higher-Order Function*** | 1. `def transformStages(guide)(f: CultivationStage => CultivationStage): GuideSnapshot`<br/>2. `def auditParameters(guide)(rule: AgronomicParam => Either[String, Unit]): List[String]` | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala#L44-L63) |
| 6 | **Komposisi Fungsi (*andThen* / *compose*)** | 1. `val formatCropName = cleanText.andThen(capitalizeFirst)`<br/>2. `val estimateDailyIrrigation = calculateDailyWaterNeed.andThen(formatWaterVolume)` | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala#L26-L40) |
| 7 | **Rantai Minimal 3 Operasi Koleksi** | `snapshot.stages.flatMap(_.parameters).filter(_.targetValue > 0.0).map(_.name.toLowerCase)` | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala#L238-L242) |
| 8 | **Penggunaan `map`, `filter`, `foldLeft`, dan `groupBy`** | • `map`: Transformasi parameter dan tahap<br/>• `filter`: Penyaringan anjuran aktif & parameter aman<br/>• `foldLeft`: Penjumlahan total durasi siklus dan jumlah kegiatan<br/>• `groupBy`: Pengelompokan anjuran berdasarkan satuan unit & kegiatan berdasarkan jenis | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala#L243-L278) |
| 9 | **Satu Fungsi Rekursif untuk Menelusuri Silsilah** | `def getRevisionLineage(currentRevisionId, revisionLookup, visited): List[GuideRevision]` (Menelusuri silsilah revisi panduan secara rekursif hingga ke revisi awal dengan akumulator pencegah siklus) | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala#L311-L327) |
| 10 | **Data Kekal & Penanganan Eror Eksplisit (`Either`)** | `Either[String, GuideSnapshot]`, `Either[String, PublishedGuide]`, `Either[String, CultivationPeriod]`, `Either[String, CultivationActivity]` digunakan tanpa melempar exception runtime tak tertangani | [`VerdantCore.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/core/VerdantCore.scala#L180-L215), [`Database.scala`](file:///c:/Users/excel%20yanto/Downloads/appGw/src/main/scala/verdant/shell/Database.scala) |

---

## 🛠️ Panduan Menjalankan Aplikasi & Pengujian

### Prasyarat
- Java Development Kit (JDK) 17 atau 21
- sbt 1.10.x (atau menggunakan `sbt` yang terpasang)

### 1. Menjalankan Seluruh Unit & Integration Test
Untuk memverifikasi kebenaran fungsional core dan persistensi database:
```bash
sbt test
```
*Hasil yang diharapkan: 16 test lulus tanpa galat (Passed: Total 16, Failed 0, Errors 0).*

### 2. Menjalankan Aplikasi Lokal
Untuk memulai server web Cask:
```bash
sbt run
```
Aplikasi akan aktif di: **[http://127.0.0.1:8080](http://127.0.0.1:8080)**.

### 3. Demonstrasi 2 Alur Utama:
- **Alur Petani (Catat Kegiatan):** Buka Beranda $\to$ Klik tombol besar *"Catat Kegiatan Baru"* $\to$ Pilih Masa Tanam (`MT-2026-TOMAT-01`), Jenis Kegiatan (*Pemupukan*), isi takaran dan catatan $\to$ Klik Simpan $\to$ Muncul konfirmasi hijau dan catatan tampil di jurnal.
- **Alur Mandor (Perbarui & Terbitkan Panduan):** Buka Panduan Budidaya $\to$ Buka *Panduan Tomat* $\to$ Buka area mandor *"Ubah Panduan"* $\to$ Ubah nilai target $\to$ Klik *"Simpan Draf & Tinjau"* $\to$ Baca ringkasan perubahan di halaman *"Apa yang berubah?"* $\to$ Klik *"Setujui & Publikasikan"* $\to$ Edisi resmi baru aktif untuk penanaman berikutnya.

---

## 📂 Struktur Berkas Proyek

```
verdant/
├── build.sbt                                 # Konfigurasi sbt (Scala 3.3.4, Cask, uPickle, Scalatags, SQLite)
├── project/
│   └── build.properties                      # Versi sbt (1.10.7)
├── test_verdant_workflows.ps1                # Skrip otomatisasi pengujian HTTP seluruh alur
├── src/
│   ├── main/
│   │   └── scala/
│   │       └── verdant/
│   │           ├── Main.scala                # Shell: Cask routes & HTTP handlers
│   │           ├── core/
│   │           │   ├── Domain.scala          # Core: Immutable Case Classes & Model Pertanian
│   │           │   └── VerdantCore.scala     # Core: 100% Pure Functional Logic & FP Rubric
│   │           └── shell/
│   │               ├── Database.scala        # Shell: SQLite persistence & realistic Indonesian seed
│   │               └── Views.scala           # Shell: Responsive mobile-first HTML (Bootstrap 5)
│   └── test/
│       └── scala/
│           └── verdant/
│               ├── VerdantCoreSpec.scala     # Unit test pemenuhan rubrik FP (MUnit)
│               └── DatabaseSpec.scala        # Integration test alur petani & mandor
```

---

## 🌾 Batasan & Catatan Desain
- **Kemandirian Dependensi:** Menggunakan Bootstrap 5 via CDN dan vanilla JavaScript modular tanpa memerlukan Node.js atau npm, sehingga seluruh proyek dapat dikompilasi langsung menggunakan `sbt`.
- **Database Embedded:** Menggunakan SQLite JDBC dengan mode Write-Ahead Logging (WAL) dan kunci asing (*Foreign Keys*) aktif untuk keandalan transaksi lokal.
