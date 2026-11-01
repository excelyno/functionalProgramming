package verdant.core

import upickle.default.{ReadWriter, macroRW}

// ============================================================================
// IMMUTABLE CASE CLASSES (Verdant Agricultural Domain Models)
// ============================================================================

/** Peran Pengguna dalam Sistem Verdant */
enum Role:
  case Farmer, Supervisor

object Role:
  def fromString(s: String): Role =
    if s != null && (s.equalsIgnoreCase("supervisor") || s.equalsIgnoreCase("mandor")) then Supervisor
    else Farmer

/** Akun Pengguna Terotentikasi (Petani vs Mandor) */
case class User(
  id: String,
  username: String,
  password: String,
  fullName: String,
  role: String // "farmer" atau "supervisor"
):
  def userRole: Role = Role.fromString(role)
  def isSupervisor: Boolean = userRole == Role.Supervisor
  def isFarmer: Boolean = userRole == Role.Farmer

object User:
  implicit val rw: ReadWriter[User] = macroRW

/** Parameter teknis dan takaran budidaya pertanian */
case class AgronomicParam(
  name: String,        // e.g. "Konsentrasi Nutrisi EC", "Target pH Air", "Dosis Pupuk NPK"
  targetValue: Double, // Nilai anjuran
  minValue: Double,    // Batas minimum aman
  maxValue: Double,    // Batas maksimum aman
  unit: String         // e.g. "mS/cm", "pH", "kg/ha", "ml/L"
)
object AgronomicParam:
  implicit val rw: ReadWriter[AgronomicParam] = macroRW

/** Tahapan budidaya tanaman (cth: Persemaian, Vegetatif, Pembungaan, Panen) */
case class CultivationStage(
  name: String,                     // Nama tahap
  durationDays: Int,                // Estimasi durasi dalam hari
  instructions: String,             // Panduan praktis untuk petani
  parameters: List[AgronomicParam]  // Daftar anjuran takaran
)
object CultivationStage:
  implicit val rw: ReadWriter[CultivationStage] = macroRW

/** Dokumen panduan budidaya lengkap (Snapshot mutlak) */
case class GuideSnapshot(
  name: String,                     // e.g. "Panduan Budidaya Tomat Hidroponik Unggul"
  cropVariety: String,              // e.g. "Tomat Beef Roma"
  stages: List[CultivationStage]    // Urutan tahapan budidaya
)
object GuideSnapshot:
  implicit val rw: ReadWriter[GuideSnapshot] = macroRW

/** Catatan revisi panduan internal (Immutable Revision / Commit) */
case class GuideRevision(
  id: String,                       // Pengenal revisi (hash pendek)
  parentIds: List[String],          // Revisi sebelumnya
  snapshot: GuideSnapshot,          // Isi panduan lengkap pada revisi ini
  author: String,                   // Nama penyusun / penyuluh
  changeNotes: String,              // Alasan perubahan panduan
  timestamp: Long                   // Waktu penyimpanan
)
object GuideRevision:
  implicit val rw: ReadWriter[GuideRevision] = macroRW

/** Panduan resmi yang telah disetujui & diterbitkan untuk petani (Release) */
case class PublishedGuide(
  editionTag: String,               // e.g. "Edisi 2026.1", "Versi 1.0-Resmi"
  guideId: String,                  // ID panduan induk
  revisionId: String,               // ID revisi yang dikunci
  status: String,                   // "DITERBITKAN"
  approvedBy: String,               // Nama mandor / kepala kebun yang menyetujui
  notes: String,                    // Catatan persetujuan
  publishedAt: Long                 // Tanggal terbit
)
object PublishedGuide:
  implicit val rw: ReadWriter[PublishedGuide] = macroRW

/** Periode masa tanam di lahan/petak tertentu (Batch) */
case class CultivationPeriod(
  code: String,                     // Kode masa tanam, e.g. "MT-2026-TOMAT-01"
  guideId: String,                  // Panduan yang dipakai
  publishedEdition: String,         // Edisi panduan resmi
  revisionId: String,               // Revisi spesifik yang terkunci
  plotName: String,                 // Nama petak lahan / greenhouse, e.g. "Greenhouse Blok 2"
  cropVariety: String,              // Komoditas tanaman
  status: String,                   // "BERJALAN", "SELESAI", "PANEN"
  startDate: Long                   // Waktu mulai tanam
)
object CultivationPeriod:
  implicit val rw: ReadWriter[CultivationPeriod] = macroRW

/** Catatan harian kegiatan petani di lapangan (Activity Record) */
case class CultivationActivity(
  id: String,
  periodCode: String,               // Kode masa tanam terkait
  activityType: String,             // Penyiraman, Pemupukan, Pengendalian Hama, Pemangkasan, Panen
  activityDate: String,             // Tanggal kegiatan (YYYY-MM-DD)
  notes: String,                    // Catatan kegiatan
  quantity: Option[String],         // Takaran/volume (opsional, cth: "50 kg NPK", "200 Liter")
  farmerName: String,               // Nama petani yang mengerjakan
  createdAt: Long
)
object CultivationActivity:
  implicit val rw: ReadWriter[CultivationActivity] = macroRW

// ============================================================================
// HUMAN-READABLE CHANGE MODELS ("Apa yang berubah?")
// ============================================================================

case class ParamChange(
  stageName: String,
  paramName: String,
  changeType: String,               // "Diubah", "Ditambahkan", "Dihapus", "Sama"
  oldValue: Option[Double],
  newValue: Option[Double],
  unit: String,
  humanDescription: String          // Kalimat penjelas dalam bahasa Indonesia yang ramah
)

case class ChangeSummary(
  oldGuideName: String,
  newGuideName: String,
  changes: List[ParamChange],
  totalChanges: Int
)

/** Statistik ringkasan panduan */
case class GuideSummary(
  totalStages: Int,
  totalDurationDays: Int,
  totalParameters: Int,
  paramsByUnit: Map[String, Int],
  averageValueByUnit: Map[String, Double]
)
