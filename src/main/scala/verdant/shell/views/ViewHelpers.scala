package verdant.shell.views

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneId}

/**
 * ============================================================================
 * VIEW HELPERS: Utilitas Format Tanggal & Konstanta Tampilan
 * ============================================================================
 * 
 * Modul ini berisi fungsi-fungsi pembantu (helpers) untuk format tanggal, jam,
 * dan teks yang dipakai berulang-ulang di berbagai template tampilan.
 * 
 * KONSEP SCALA UNTUK PEMULA:
 * - `trait`: Seperti 'interface' di Java, tapi bisa berisi implementasi fungsi nyata.
 *            Trait ini nantinya di-mixin (digabung) ke object `Views`.
 * - `lazy val / val`: Variabel bernilai tetap (immutable). Sekali dihitung, nilainya tidak berubah.
 * - `def`: Deklarasi fungsi/method di Scala.
 */
trait ViewHelpers:

  /**
   * Formatter tanggal dengan standar Indonesia: contoh "27 Oktober 2026, 14:30"
   * Menggunakan zona waktu lokal perangkat/server.
   */
  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")
    .withZone(ZoneId.systemDefault())

  /**
   * Mengubah timestamp epoch millisecond (angka long) menjadi teks tanggal yang mudah dibaca.
   * 
   * Contoh:
   *   formatTs(1793088000000L) => "27 Oktober 2026, 07:00"
   */
  def formatTs(ts: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(ts))

  /**
   * Mengambil tanggal hari ini dalam format standar HTML input date: "YYYY-MM-DD"
   * Dipakai sebagai nilai bawaan (default value) pada form pencatatan tanggal kegiatan.
   */
  def todayStr: String = LocalDate.now().toString

