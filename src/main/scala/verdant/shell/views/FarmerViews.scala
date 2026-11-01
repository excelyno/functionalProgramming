package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.nav
import verdant.core.*
import verdant.shell.Database

/**
 * ============================================================================
 * FARMER VIEWS: Ruang Kerja Petani (Masa Tanam & Log Lapangan)
 * ============================================================================
 * 
 * Modul ini menangani seluruh antarmuka yang dilihat oleh Petani:
 * 1. farmerBerandaView        -> Beranda Petani: status masa tanam aktif, catatan terkini, dan panduan.
 * 2. farmerMasaTanamListView  -> Daftar seluruh petak/lahan masa tanam yang sedang berlangsung.
 * 3. farmerMasaTanamDetailView-> Garis waktu harian, kepatuhan batas aman, dan detail instruksi tahapan.
 * 4. farmerPanduanListView    -> Katalog panduan budidaya resmi yang telah diterbitkan mandor.
 * 5. farmerPanduanDetailView  -> Detail SOP budidaya lengkap per tahapan tanam (Read-Only).
 * 
 * ALUR DATA (DARI MANA KE MANA):
 * - Route di Main.scala menerima request GET dari browser petani (misal: GET /masa-tanam).
 * - Main.scala mengambil data dari Database.scala (misal: db.listPeriods()).
 * - Main.scala memanggil fungsi di modul ini: Views.farmerMasaTanamListView(periods, guides).
 * - Fungsi ini merakit elemen HTML Scalatags dan mengembalikannya ke LayoutView untuk dibungkus.
 * 
 * KONSEP SCALA UNTUK PEMULA:
 * - `List[(CultivationPeriod, String)]`: List yang berisi pasangan Tuple (Objek Masa Tanam, Nama Panduan).
 * - `periods.map { case (p, gName) => ... }`: Perulangan fungsional (seperti foreach) yang mengubah setiap data menjadi elemen HTML.
 * - `Option[String]`: Tipe data yang bisa bernilai `Some("teks")` atau `None` (mencegah error NullPointerException).
 */
trait FarmerViews extends ViewHelpers with SharedModals:

  // ==========================================================================
  // 1. BERANDA PETANI (FOCUSED ON NEXT ACTIONS & ACTIVE CROPS)
  // Dipanggil dari: Main.scala baris 103 (GET /)
  // ==========================================================================
  def farmerBerandaView(
    periods: List[(CultivationPeriod, String)],
    recentActivities: List[CultivationActivity],
    publishedGuides: List[(Database#GuideRow, PublishedGuide, GuideSnapshot)]
  ): Modifier =
    div(
      // Header Halaman & Tombol Aksi Cepat
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Beranda Petani"),
          p(cls := "text-muted small mb-0", "Pantau masa tanam aktif di petak lahan Anda dan catat kegiatan budidaya hari ini.")
        ),
        div(cls := "d-flex gap-2")(
          if periods.nonEmpty then
            button(
              cls := "btn btn-verdant",
              data("bs-toggle") := "modal",
              data("bs-target") := "#catatKegiatanModal"
            )(i(cls := "bi bi-pencil-square me-1"), "Catat Kegiatan")
          else modifier(),
          button(
            cls := "btn btn-verdant-outline",
            data("bs-toggle") := "modal",
            data("bs-target") := "#bukaMasaTanamModal"
          )(i(cls := "bi bi-plus-lg me-1"), "Mulai Masa Tanam")
        )
      ),

      // KONDISI 1: Belum Ada Masa Tanam (Tampilan Onboarding/Panduan Awal)
      if periods.isEmpty then
        div(cls := "p-4 p-md-5 border rounded-3 bg-white text-center mb-4")(
          div(cls := "text-muted mb-3")(i(cls := "bi bi-sprout", style := "font-size: 3rem; color: #1b4332;")),
          h2(cls := "h5 fw-bold text-dark mb-2", "Selamat Datang! Belum Ada Masa Tanam yang Berjalan"),
          p(cls := "text-secondary small mb-4 mx-auto", style := "max-width: 500px;",
            "Masa tanam mengikat tanaman Anda pada panduan budidaya resmi agar takaran pupuk dan air di lapangan memiliki acuan yang jelas."
          ),
          div(cls := "row g-3 text-start mx-auto mb-4", style := "max-width: 680px;")(
            div(cls := "col-sm-6 col-md-3")(
              div(cls := "p-3 border rounded-3 bg-light h-100")(
                span(cls := "badge bg-dark text-white mb-2", "Langkah 1"),
                div(cls := "fw-semibold small text-dark mb-1", "Pilih Komoditas"),
                div(cls := "text-muted", style := "font-size: 12px;", "Pilih tanaman dari katalog panduan resmi.")
              )
            ),
            div(cls := "col-sm-6 col-md-3")(
              div(cls := "p-3 border rounded-3 bg-light h-100")(
                span(cls := "badge bg-dark text-white mb-2", "Langkah 2"),
                div(cls := "fw-semibold small text-dark mb-1", "Tentukan Lahan"),
                div(cls := "text-muted", style := "font-size: 12px;", "Beri nama petak atau greenhouse.")
              )
            ),
            div(cls := "col-sm-6 col-md-3")(
              div(cls := "p-3 border rounded-3 bg-light h-100")(
                span(cls := "badge bg-dark text-white mb-2", "Langkah 3"),
                div(cls := "fw-semibold small text-dark mb-1", "Kunci Panduan"),
                div(cls := "text-muted", style := "font-size: 12px;", "Edisi resmi terkunci permanen.")
              )
            ),
            div(cls := "col-sm-6 col-md-3")(
              div(cls := "p-3 border rounded-3 bg-light h-100")(
                span(cls := "badge bg-dark text-white mb-2", "Langkah 4"),
                div(cls := "fw-semibold small text-dark mb-1", "Catat Harian"),
                div(cls := "text-muted", style := "font-size: 12px;", "Catat penyiraman & pemupukan.")
              )
            )
          ),
          button(
            cls := "btn btn-verdant px-4 py-2",
            data("bs-toggle") := "modal",
            data("bs-target") := "#bukaMasaTanamModal"
          )(i(cls := "bi bi-plus-circle me-2"), "Mulai Masa Tanam Pertama Sekarang")
        )
      else
        // KONDISI 2: Sudah Ada Masa Tanam Berjalan (Kartu Status Utama)
        val (activePeriod, guideName) = periods.head
        div(cls := "mb-4")(
          div(cls := "panel-verdant bg-light border-success-subtle mb-4")(
            div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 mb-3")(
              div(
                div(cls := "d-flex align-items-center gap-2 mb-1")(
                  span(cls := "badge bg-success text-white small", "Masa Tanam Aktif"),
                  span(cls := "small text-muted fw-medium", activePeriod.code)
                ),
                h2(cls := "h4 fw-bold text-dark mb-1", s"${activePeriod.cropVariety} — ${activePeriod.plotName}"),
                div(cls := "small text-secondary")(
                  span("Panduan Resmi Terkunci: "),
                  strong(cls := "text-dark", activePeriod.publishedEdition),
                  span(s" (Mulai: ${formatTs(activePeriod.startDate)})")
                )
              ),
              div(cls := "d-flex gap-2 flex-wrap")(
                button(
                  cls := "btn btn-verdant text-nowrap",
                  data("bs-toggle") := "modal",
                  data("bs-target") := "#catatKegiatanModal"
                )(i(cls := "bi bi-plus-lg me-1"), "Catat Kegiatan"),
                a(
                  cls := "btn btn-verdant-outline text-nowrap",
                  href := s"/masa-tanam/${activePeriod.code}"
                )("Buka Garis Waktu & Catatan →")
              )
            ),
            div(cls := "pt-2 border-top d-flex gap-3 small text-muted flex-wrap")(
              a(cls := "text-decoration-none text-success fw-medium", href := s"/panduan/${activePeriod.guideId}")(
                i(cls := "bi bi-book me-1"), "Lihat Instruksi Lengkap Panduan Budidaya Ini →"
              ),
              if periods.length > 1 then
                a(cls := "text-decoration-none text-muted", href := "/masa-tanam")(
                  s"Lihat ${periods.length - 1} masa tanam lainnya →"
                )
              else modifier()
            )
          ),

          // Layout 2 Kolom: Jurnal Terkini vs Panduan Tersedia
          div(cls := "row g-4")(
            // Kolom Kiri: Catatan Kegiatan Terbaru
            div(cls := "col-lg-7")(
              div(cls := "d-flex justify-content-between align-items-center mb-3")(
                h3(cls := "section-label mb-0", "Catatan Kegiatan Terkini"),
                a(cls := "small text-decoration-none text-success fw-medium", href := s"/masa-tanam/${activePeriod.code}", "Semua Catatan →")
              ),
              if recentActivities.isEmpty then
                div(cls := "p-4 text-center text-muted border rounded-3 bg-white small")(
                  p(cls := "mb-2", "Belum ada catatan kegiatan yang dimasukkan."),
                  button(cls := "btn btn-sm btn-verdant", data("bs-toggle") := "modal", data("bs-target") := "#catatKegiatanModal")("Catat Kegiatan Sekarang")
                )
              else
                div(cls := "border rounded-3 overflow-hidden bg-white")(
                  div(cls := "divide-y")(
                    recentActivities.take(5).map { act =>
                      div(cls := "p-3 border-bottom")(
                        div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                          div(
                            span(cls := "fw-semibold text-dark me-2", act.activityType),
                            act.quantity.map(q => span(cls := "badge bg-light text-dark border", q)).getOrElse(modifier())
                          ),
                          span(cls := "small text-muted", act.activityDate)
                        ),
                        p(cls := "mb-1 text-secondary small", act.notes),
                        div(cls := "small text-muted", s"Lahan: ${act.periodCode} · Pelaksana: ${act.farmerName}")
                      )
                    }
                  )
                )
            ),

            // Kolom Kanan: Panduan Resmi Tersedia
            div(cls := "col-lg-5")(
              div(cls := "mb-4")(
                h3(cls := "section-label mb-3", "Panduan Budidaya Tersedia"),
                if publishedGuides.isEmpty then
                  div(cls := "p-3 border rounded-3 bg-white text-muted small", "Belum ada panduan resmi diterbitkan.")
                else
                  div(cls := "border rounded-3 overflow-hidden bg-white")(
                    publishedGuides.map { case (g, pub, snap) =>
                      div(cls := "p-3 border-bottom d-flex justify-content-between align-items-center")(
                        div(
                          a(cls := "fw-medium text-dark text-decoration-none small d-block mb-1", href := s"/panduan/${g.id}", g.name),
                          div(cls := "small text-muted", s"${g.cropVariety} · ${pub.editionTag}")
                        ),
                        a(cls := "btn btn-sm btn-verdant-outline py-1 px-2 text-nowrap", href := s"/panduan/${g.id}", "Buka")
                      )
                    }
                  )
              ),
              div(cls := "p-3 rounded-3 bg-light border")(
                h4(cls := "small fw-bold text-dark text-uppercase tracking-wide mb-1", "💡 Prinsip Pencatatan"),
                p(cls := "small text-muted mb-0", "Seluruh takaran pupuk dan air yang Anda catat akan otomatis diverifikasi terhadap rentang batas aman pada panduan resmi yang terkunci.")
              )
            )
          )
        )
      ,

      catatKegiatanModal(periods),
      bukaMasaTanamModal(publishedGuides.map { case (g, pub, _) => (pub, g.name) })
    )

  // ==========================================================================
  // 2. DAFTAR MASA TANAM SAYA
  // Dipanggil dari: Main.scala baris 127 (GET /masa-tanam)
  // ==========================================================================
  def farmerMasaTanamListView(
    periods: List[(CultivationPeriod, String)],
    publishedGuides: List[(PublishedGuide, String)]
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Masa Tanam Saya"),
          p(cls := "text-muted small mb-0", "Daftar siklus budidaya Anda di setiap lahan/greenhouse yang terkunci pada panduan resmi.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#bukaMasaTanamModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Mulai Masa Tanam Baru")
      ),

      if periods.isEmpty then
        div(cls := "p-5 text-center text-muted border rounded-3 bg-white")(
          p(cls := "mb-3", "Belum ada masa tanam yang terdaftar."),
          button(
            cls := "btn btn-verdant",
            data("bs-toggle") := "modal",
            data("bs-target") := "#bukaMasaTanamModal"
          )("Mulai Masa Tanam Sekarang")
        )
      else
        div(cls := "row g-3")(
          periods.map { case (periodItem, gName) =>
            div(cls := "col-md-6")(
              div(cls := "panel-verdant h-100 d-flex flex-column justify-content-between")(
                div(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                    span(cls := "small text-muted fw-medium", periodItem.code),
                    span(cls := "badge bg-success-subtle text-success border border-success-subtle", periodItem.status)
                  ),
                  h2(cls := "h5 fw-bold text-dark mb-1", periodItem.cropVariety),
                  p(cls := "small text-muted mb-2", s"Lokasi: ${periodItem.plotName}"),
                  div(cls := "small text-secondary mb-3",
                    span("Panduan: "),
                    strong(cls := "text-dark", periodItem.publishedEdition)
                  )
                ),
                div(cls := "pt-3 border-top d-flex gap-2")(
                  a(cls := "btn btn-sm btn-verdant flex-grow-1", href := s"/masa-tanam/${periodItem.code}", "Buka Garis Waktu & Catatan"),
                  a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${periodItem.guideId}", "Panduan")
                )
              )
            )
          }
        )
      ,

      catatKegiatanModal(periods),
      bukaMasaTanamModal(publishedGuides)
    )

  // ==========================================================================
  // 3. DETAIL MASA TANAM & GARIS WAKTU KEGIATAN
  // Dipanggil dari: Main.scala baris 161 (GET /masa-tanam/:code)
  // ==========================================================================
  def farmerMasaTanamDetailView(
    period: CultivationPeriod,
    guide: Database#GuideRow,
    revision: GuideRevision,
    publishedGuide: PublishedGuide,
    activities: List[CultivationActivity]
  ): Modifier =
    val snap = revision.snapshot
    val allParams = snap.stages.flatMap(_.parameters)
    div(
      nav(aria.label := "breadcrumb", cls := "mb-3")(
        ol(cls := "breadcrumb small text-muted mb-0")(
          li(cls := "breadcrumb-item")(a(href := "/masa-tanam", cls := "text-decoration-none text-muted", "Masa Tanam")),
          li(cls := "breadcrumb-item active text-dark", period.code)
        )
      ),

      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            h1(cls := "h4 fw-bold text-dark mb-0", s"${period.cropVariety} — ${period.plotName}"),
            span(cls := "badge bg-success text-white small", period.status)
          ),
          div(cls := "small text-muted")(
            span(s"Kode: ${period.code} · Dimulai: ${formatTs(period.startDate)}")
          )
        ),
        button(
          cls := "btn btn-verdant text-nowrap",
          data("bs-toggle") := "modal",
          data("bs-target") := "#catatKegiatanModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Catat Kegiatan di Lahan Ini")
      ),

      // Kotak Info: Acuan Panduan Resmi yang Terkunci
      div(cls := "p-3 rounded-3 bg-light border mb-4")(
        div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-2")(
          div(
            div(cls := "small fw-semibold text-dark", s"🔒 Terkunci pada Panduan Resmi: ${period.publishedEdition}"),
            div(cls := "small text-muted",
              s"Revisi #${revision.id.take(8)} disetujui oleh ${publishedGuide.approvedBy}. Meskipun mandor memperbarui draf di masa depan, masa tanam ini tetap menggunakan acuan ini."
            )
          ),
          a(cls := "btn btn-sm btn-verdant-outline text-nowrap", href := s"/panduan/${guide.id}")(
            "Lihat Panduan Budidaya Lengkap →"
          )
        )
      ),

      div(cls := "row g-4")(
        // Kolom Kiri: Garis Waktu Kegiatan
        div(cls := "col-lg-7")(
          div(cls := "d-flex justify-content-between align-items-center mb-3")(
            h2(cls := "section-label mb-0", "Garis Waktu Kegiatan Lapangan"),
            span(cls := "small text-muted", s"${activities.length} kegiatan dicatat")
          ),
          if activities.isEmpty then
            div(cls := "p-4 text-center text-muted border rounded-3 bg-white small")(
              p(cls := "mb-2", "Belum ada kegiatan yang dicatat pada masa tanam ini."),
              button(
                cls := "btn btn-sm btn-verdant",
                data("bs-toggle") := "modal",
                data("bs-target") := "#catatKegiatanModal"
              )("Catat Kegiatan Pertama")
            )
          else
            div(cls := "border rounded-3 overflow-hidden bg-white")(
              activities.map { act =>
                // PURE FUNCTION AUDIT: Periksa kepatuhan dosis terhadap rentang aman
                val complianceWarning = VerdantCore.checkActivityCompliance(act, allParams)
                div(cls := "p-3 border-bottom")(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                    div(
                      span(cls := "fw-semibold text-dark me-2", act.activityType),
                      act.quantity.map(q => span(cls := "badge bg-light text-dark border", q)).getOrElse(modifier())
                    ),
                    span(cls := "small text-muted", act.activityDate)
                  ),
                  p(cls := "mb-1 text-secondary small", act.notes),
                  div(cls := "d-flex justify-content-between align-items-center small")(
                    span(cls := "text-muted", s"Pelaksana: ${act.farmerName}"),
                    complianceWarning match
                      case Some(warning) =>
                        span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle", attr("title") := warning)(
                          i(cls := "bi bi-exclamation-triangle me-1"), "Perlu Perhatian Takaran"
                        )
                      case None => modifier()
                  )
                )
              }
            )
        ),

        // Kolom Kanan: Tahapan Panduan yang Berlaku
        div(cls := "col-lg-5")(
          div(cls := "p-3 p-md-4 rounded-3 bg-light border")(
            h2(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-1", "Tahapan Panduan yang Berlaku"),
            p(cls := "small text-muted mb-3", s"Rekomendasi teknis ${period.publishedEdition}"),
            div(
              snap.stages.zipWithIndex.map { case (s, idx) =>
                div(cls := s"py-3 ${if idx > 0 then "border-top" else ""}")(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                    span(cls := "fw-semibold text-dark small", s"${idx + 1}. ${s.name}"),
                    span(cls := "small text-muted", s"${s.durationDays} Hari")
                  ),
                  p(cls := "small text-secondary mb-2", s.instructions),
                  ul(cls := "list-unstyled mb-0 small text-muted")(
                    s.parameters.map { p =>
                      li(
                        span(cls := "fw-medium text-dark", p.name),
                        s": ${p.targetValue} ${p.unit} ",
                        span(cls := "text-muted", s"[${p.minValue}–${p.maxValue}]")
                      )
                    }
                  )
                )
              }
            )
          )
        )
      ),

      catatKegiatanModal(List((period, guide.name)), Some(period.code))
    )

  // ==========================================================================
  // 4. KATALOG PANDUAN BUDIDAYA RESMI (PETANI)
  // Dipanggil dari: Main.scala baris 177 (GET /panduan)
  // ==========================================================================
  def farmerPanduanListView(
    guides: List[(Database#GuideRow, PublishedGuide, GuideSnapshot)]
  ): Modifier =
    div(
      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Panduan Budidaya Resmi"),
        p(cls := "text-muted small mb-0", "Pelajari instruksi budidaya dan takaran rekomendasi resmi untuk setiap komoditas tanaman.")
      ),

      if guides.isEmpty then
        div(cls := "p-5 text-center text-muted border rounded-3 bg-white")(
          "Belum ada panduan resmi yang diterbitkan oleh mandor."
        )
      else
        div(cls := "row g-3")(
          guides.map { case (g, pub, snap) =>
            val summary = VerdantCore.summarizeGuide(snap)
            div(cls := "col-md-6")(
              div(cls := "panel-verdant h-100 d-flex flex-column justify-content-between")(
                div(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                    span(cls := "small text-muted fw-medium", g.cropVariety),
                    span(cls := "badge bg-success-subtle text-success border border-success-subtle", pub.editionTag)
                  ),
                  h2(cls := "h5 fw-bold text-dark mb-1", g.name),
                  p(cls := "small text-muted mb-3", s"Total Durasi: ${summary.totalDurationDays} Hari · ${summary.totalStages} Tahapan Budidaya")
                ),
                div(cls := "pt-3 border-top d-flex gap-2")(
                  a(cls := "btn btn-sm btn-verdant flex-grow-1", href := s"/panduan/${g.id}", "Buka Panduan"),
                  button(
                    cls := "btn btn-sm btn-verdant-outline",
                    data("bs-toggle") := "modal",
                    data("bs-target") := "#bukaMasaTanamModal"
                  )("Gunakan di Lahan")
                )
              )
            )
          }
        )
      ,

      bukaMasaTanamModal(guides.map { case (g, pub, _) => (pub, g.name) })
    )

  // ==========================================================================
  // 5. DETAIL PANDUAN BUDIDAYA RESMI (PETANI - READ ONLY)
  // Dipanggil dari: Main.scala baris 204 (GET /panduan/:id)
  // ==========================================================================
  def farmerPanduanDetailView(
    guide: Database#GuideRow,
    published: PublishedGuide,
    snapshot: GuideSnapshot,
    summary: GuideSummary
  ): Modifier =
    div(
      nav(aria.label := "breadcrumb", cls := "mb-3")(
        ol(cls := "breadcrumb small text-muted mb-0")(
          li(cls := "breadcrumb-item")(a(href := "/panduan", cls := "text-decoration-none text-muted", "Panduan Budidaya")),
          li(cls := "breadcrumb-item active text-dark", guide.name)
        )
      ),

      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            h1(cls := "h4 fw-bold text-dark mb-0", guide.name),
            span(cls := "badge bg-success-subtle text-success border border-success-subtle", published.editionTag)
          ),
          div(cls := "small text-muted", s"Komoditas: ${guide.cropVariety} · ${summary.totalStages} Tahapan · Total Durasi: ${summary.totalDurationDays} Hari")
        ),
        button(
          cls := "btn btn-verdant text-nowrap",
          data("bs-toggle") := "modal",
          data("bs-target") := "#bukaMasaTanamModal"
        )(i(cls := "bi bi-play-circle me-1"), "Mulai Masa Tanam Baru")
      ),

      // Info Pengesahan Edisi Resmi
      div(cls := "p-3 rounded-3 bg-light border mb-4")(
        div(cls := "small text-muted")(
          strong(cls := "text-dark", s"Edisi Resmi: ${published.editionTag}"),
          span(s" · Disetujui oleh: ${published.approvedBy} · Catatan: ${published.notes}")
        )
      ),

      // Daftar Tahapan Budidaya
      div(cls := "mb-4")(
        h2(cls := "section-label mb-3", "Tahapan Budidaya & Petunjuk Lapangan"),
        div(cls := "border-top")(
          snapshot.stages.zipWithIndex.map { case (stage, idx) =>
            div(cls := "py-4 border-bottom")(
              div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                h3(cls := "h6 fw-bold text-dark mb-0", s"${idx + 1}. ${stage.name}"),
                span(cls := "small text-muted", s"Durasi: ${stage.durationDays} Hari")
              ),
              p(cls := "small text-secondary mb-3",
                strong(cls := "text-dark", "Petunjuk Lapangan: "),
                stage.instructions
              ),
              div(cls := "border rounded-3 overflow-hidden bg-white")(
                table(cls := "table table-sm table-borderless mb-0 small")(
                  thead(cls := "table-light text-muted border-bottom")(
                    tr(
                      th(cls := "py-2 px-3 fw-medium", "Parameter"),
                      th(cls := "py-2 fw-medium", "Target Rekomendasi"),
                      th(cls := "py-2 px-3 fw-medium text-end", "Batas Rentang Aman")
                    )
                  ),
                  tbody(
                    stage.parameters.map { p =>
                      tr(
                        td(cls := "py-2 px-3 fw-medium text-dark", p.name),
                        td(cls := "py-2", s"${p.targetValue} ${p.unit}"),
                        td(cls := "py-2 px-3 text-end text-muted", s"[${p.minValue} – ${p.maxValue}] ${p.unit}")
                      )
                    }
                  )
                )
              )
            )
          }
        )
      ),

      bukaMasaTanamModal(List((published, guide.name)), Some(guide.id))
    )

