package verdant.shell

import verdant.shell.views.*

/**
 * ============================================================================
 * PINTU GERBANG UTAMA TAMPILAN WEB (VIEWS FACADE)
 * ============================================================================
 * 
 * Halo! Sebelumnya, seluruh template HTML di Verdant (1.600+ baris kode)
 * ditumpuk di dalam satu file ini, sehingga sangat sulit dipelajari oleh pemula.
 * 
 * Sekarang, kode tampilan telah dipecah (dimodularisasi) ke dalam folder:
 * `src/main/scala/verdant/shell/views/`
 * 
 * ============================================================================
 * PETA STRUKTUR FOLDER & MODUL VIEW:
 * ============================================================================
 * 
 * 1. LayoutView.scala
 *    -> Mengatur bingkai HTML utama (<head>, CSS Bootstrap, Navbar,
 *       tombol ganti peran Petani/Mandor, alert notifikasi, dan halaman 403 Forbidden).
 * 
 * 2. FarmerViews.scala
 *    -> Menangani seluruh halaman khusus Petani:
 *       - farmerBerandaView         : Dashboard petak lahan & aksi cepat hari ini
 *       - farmerMasaTanamListView   : Daftar seluruh siklus masa tanam
 *       - farmerMasaTanamDetailView : Garis waktu aktivitas & cek kepatuhan batas aman
 *       - farmerPanduanListView     : Katalog panduan budidaya resmi
 *       - farmerPanduanDetailView   : Rincian tahapan budidaya resmi (read-only)
 * 
 * 3. SupervisorViews.scala
 *    -> Menangani seluruh halaman kendali mutu Mandor:
 *       - supervisorBerandaView              : Dashboard mandor & daftar draf menunggu persetujuan
 *       - supervisorKelolaPanduanView        : Tabel master kelola panduan agronomi
 *       - supervisorPemeriksaanPerubahanView : Ringkasan perbedaan draf (Human-readable Diff)
 *       - supervisorPanduanDetailView        : Editor parameter draf tanpa merusak data lama
 *       - supervisorTinjauPerubahanView      : Pengambilan keputusan: Terbitkan Edisi Resmi / Tolak
 *       - supervisorRiwayatView              : Jejak audit silsilah revisi & pemulihan versi (Rollback)
 * 
 * 4. JournalViews.scala
 *    -> Menangani rekapitulasi jurnal harian petani:
 *       - catatanView               : Tabel riwayat log kegiatan dengan fitur filter petak
 * 
 * 5. SharedModals.scala
 *    -> Dialog pop-up yang dipakai bersama di berbagai halaman:
 *       - catatKegiatanModal        : Form pop-up catat penyiraman, pemupukan, dll
 *       - bukaMasaTanamModal        : Form pop-up mulai masa tanam baru di lahan
 *       - tambahPanduanModal         : Form pop-up daftarkan SOP komoditas baru
 * 
 * 6. ViewHelpers.scala
 *    -> Utilitas pembantu format tanggal (`formatTs`) dan tanggal hari ini (`todayStr`).
 * 
 * ============================================================================
 * ALUR APLIKASI (DARI MANA KE MANA?):
 * ============================================================================
 * 
 *     Browser Pengguna (Klik Menu / Kirim Form)
 *                   │
 *                   ▼
 *         1. Main.scala (HTTP Routes / Cask Framework)
 *            - Memeriksa hak akses peran (Role.Farmer vs Role.Supervisor)
 *            - Menerima parameter GET / form POST
 *                   │
 *                   ▼
 *         2. Database.scala (SQLite Persistence)
 *            - Mengambil data dari tabel SQLite (panduan, revisi, aktivitas)
 *                   │
 *                   ▼
 *         3. VerdantCore.scala (Functional Core Murni)
 *            - Memvalidasi batas aman takaran pupuk/air
 *            - Menghitung perbedaan teks ("Apa yang berubah?")
 *                   │
 *                   ▼
 *         4. Views (Modul di folder views/)
 *            - Merakit data menjadi tag HTML Scalatags
 *            - Dibungkus oleh LayoutView.layout(...)
 *                   │
 *                   ▼
 *     Browser Menerima HTML Rapi (HTTP 200 OK)
 * 
 * ============================================================================
 * MENGAPA MENGGUNAKAN TEKNIK MIXIN TRAIT INI?
 * ============================================================================
 * Dengan `object Views extends ...`, seluruh method di atas tetap bisa dipanggil
 * dengan cara yang sama persis seperti sebelumnya: `Views.farmerBerandaView(...)`
 * sehingga file pemanggil seperti `Main.scala` TIDAK PERLU DIUBAH SAMA SEKALI,
 * namun kode sumber sekarang tersusun rapi, bersih, dan sangat mudah dipelajari!
 */
object Views
    extends ViewHelpers
    with SharedModals
    with LayoutView
    with FarmerViews
    with SupervisorViews
    with JournalViews
