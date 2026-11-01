package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.nav
import verdant.core.*
import verdant.shell.Database

/**
 * ============================================================================
 * FARMER VIEWS: Ruang Kerja Petani (Penerapan Cetakan Mandor & Log Abadi)
 * ============================================================================
 * 
 * Filosofi Utama:
 * 1. Mandor merilis "Cetakan Budidaya" (Master SOP takaran pupuk, air, dan durasi).
 * 2. Petani menerapkan cetakan tersebut ke lahan/greenhouse-nya (terkunci permanen).
 * 3. Seluruh catatan kegiatan petani bersifat KEKAL & ANTI-HAPUS (seperti komit Git).
 *    Hal ini menjamin riwayat panen tidak dapat dimanipulasi diam-diam.
 */
trait FarmerViews extends ViewHelpers with SharedModals:

  // ==========================================================================
  // 1. BERANDA PETANI (FOCUSED ON NEXT ACTIONS & LOG KEKAL)
  // ==========================================================================
  def farmerBerandaView(
    periods: List[(CultivationPeriod, String)],
    recentActivities: List[CultivationActivity],
    publishedGuides: List[(Database#GuideRow, PublishedGuide, GuideSnapshot)]
  ): Modifier =
    div(
      // Header & Quick Action
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            span(cls := "badge bg-success-subtle text-success border border-success-subtle px-2 py-1 small rounded-pill", "🌿 Mode Petani"),
            span(cls := "small text-muted", "Pendamping Lahan Lapangan")
          ),
          h1(cls := "h4 fw-bold text-dark mb-1", "Beranda Lahan & Buku Catatan"),
          p(cls := "text-muted small mb-0", "Terapkan cetakan budidaya mandor dan catat aktivitas lapangan secara permanen.")
        ),
        div(cls := "d-flex gap-2")(
          if periods.nonEmpty then
            button(
              cls := "btn btn-verdant",
              data("bs-toggle") := "modal",
              data("bs-target") := "#catatKegiatanModal"
            )(i(cls := "bi bi-journal-plus me-1"), "Catat Kegiatan Lapangan")
          else modifier(),
          button(
            cls := "btn btn-verdant-outline",
            data("bs-toggle") := "modal",
            data("bs-target") := "#bukaMasaTanamModal"
          )(i(cls := "bi bi-plus-circle me-1"), "Terapkan Cetakan Baru")
        )
      ),

      // BANNER EDUKASI VISUAL: Konsep Sederhana Mandor-Petani-Git
      div(cls := "p-3 p-md-4 mb-4 rounded-4 bg-white border border-success-subtle shadow-sm")(
        div(cls := "row g-3 align-items-center")(
          div(cls := "col-md-4 border-end-md")(
            div(cls := "d-flex gap-3 align-items-center")(
              div(cls := "p-3 rounded-circle bg-success-subtle text-success fs-4")(i(cls := "bi bi-clipboard2-check")),
              div(
                div(cls := "fw-bold text-dark small", "1. Mandor Buat Cetakan"),
                div(cls := "text-muted", style := "font-size: 12px;", "Master SOP takaran air, nutrisi, dan durasi per fase.")
              )
            )
          ),
          div(cls := "col-md-4 border-end-md")(
            div(cls := "d-flex gap-3 align-items-center")(
              div(cls := "p-3 rounded-circle bg-primary-subtle text-primary fs-4")(i(cls := "bi bi-pin-angle")),
              div(
                div(cls := "fw-bold text-dark small", "2. Petani Kunci Cetakan"),
                div(cls := "text-muted", style := "font-size: 12px;", "Cetakan mandor terkunci permanen di petak lahan Anda.")
              )
            )
          ),
          div(cls := "col-md-4")(
            div(cls := "d-flex gap-3 align-items-center")(
              div(cls := "p-3 rounded-circle text-indigo fs-4", style := "background-color: #e0e7ff; color: #3730a3;")(i(cls := "bi bi-lock-fill")),
              div(
                div(cls := "fw-bold text-dark small", "3. Catatan Abadi ala Git"),
                div(cls := "text-muted", style := "font-size: 12px;", "Data tersimpan permanen tanpa tombol hapus untuk bukti panen.")
              )
            )
          )
        )
      ),

      // KONDISI 1: Belum Ada Lahan yang Menerapkan Cetakan
      if periods.isEmpty then
        div(cls := "p-5 border rounded-4 bg-white text-center mb-4 shadow-sm")(
          div(cls := "text-muted mb-3")(i(cls := "bi bi-flower1", style := "font-size: 3.2rem; color: #2d6a4f;")),
          h2(cls := "h5 fw-bold text-dark mb-2", "Belum Ada Petak Lahan yang Menerapkan Cetakan"),
          p(cls := "text-secondary small mb-4 mx-auto", style := "max-width: 480px;",
            "Untuk memulai, pilih salah satu Cetakan Budidaya resmi dari mandor. Cetakan tersebut akan menjadi acuan takaran pupuk dan air di lahan Anda."
          ),
          button(
            cls := "btn btn-verdant px-4 py-2",
            data("bs-toggle") := "modal",
            data("bs-target") := "#bukaMasaTanamModal"
          )(i(cls := "bi bi-play-circle me-2"), "Pilih Cetakan & Mulai Tanam Sekarang")
        )
      else
        // KONDISI 2: Sudah Ada Lahan Aktif
        val (activePeriod, guideName) = periods.head
        div(cls := "mb-4")(
          div(cls := "panel-verdant mb-4 bg-white border border-success-subtle shadow-sm")(
            div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 mb-3")(
              div(
                div(cls := "d-flex align-items-center gap-2 mb-1 flex-wrap")(
                  span(cls := "badge bg-success text-white small px-2 py-1", "Lahan Aktif"),
                  span(cls := "badge-cetakan", s"🔒 Cetakan Mandor: ${activePeriod.publishedEdition}"),
                  span(cls := "small text-muted font-monospace", s"#${activePeriod.code}")
                ),
                h2(cls := "h4 fw-bold text-dark mb-1", s"${activePeriod.cropVariety} — ${activePeriod.plotName}"),
                div(cls := "small text-muted")(
                  span(s"Dimulai sejak: ${formatTs(activePeriod.startDate)} · Acuan takaran terkunci paten.")
                )
              ),
              div(cls := "d-flex gap-2 flex-wrap")(
                button(
                  cls := "btn btn-verdant text-nowrap",
                  data("bs-toggle") := "modal",
                  data("bs-target") := "#catatKegiatanModal"
                )(i(cls := "bi bi-journal-plus me-1"), "Catat Kegiatan"),
                a(
                  cls := "btn btn-verdant-outline text-nowrap",
                  href := s"/masa-tanam/${activePeriod.code}"
                )("Buka Buku Log Lahan →")
              )
            ),
            div(cls := "pt-3 border-top d-flex gap-3 small text-muted flex-wrap align-items-center")(
              a(cls := "text-decoration-none text-success fw-medium d-inline-flex align-items-center gap-1", href := s"/panduan/${activePeriod.guideId}")(
                i(cls := "bi bi-book"), "Pelajari Rincian Cetakan Budidaya Ini →"
              ),
              if periods.length > 1 then
                a(cls := "text-decoration-none text-muted", href := "/masa-tanam")(
                  s"Lihat ${periods.length - 1} lahan aktif lainnya →"
                )
              else modifier()
            )
          ),

          // Layout 2 Kolom: Log Lapangan Abadi vs Cetakan Tersedia
          div(cls := "row g-4")(
            // Kolom Kiri: Catatan Kegiatan Terbaru (Kekal / Anti-Hapus)
            div(cls := "col-lg-7")(
              div(cls := "d-flex justify-content-between align-items-center mb-3")(
                div(
                  h3(cls := "section-label mb-0 text-dark", "Log Lapangan Terkini (Anti-Hapus)"),
                  span(cls := "text-muted", style := "font-size: 12px;", "Tersimpan permanen seperti commit Git")
                ),
                a(cls := "small text-decoration-none text-success fw-medium", href := "/catatan", "Semua Catatan →")
              ),
              if recentActivities.isEmpty then
                div(cls := "p-4 text-center text-muted border rounded-4 bg-white small")(
                  p(cls := "mb-2", "Belum ada catatan kegiatan di petak lahan ini."),
                  button(cls := "btn btn-sm btn-verdant", data("bs-toggle") := "modal", data("bs-target") := "#catatKegiatanModal")("Catat Kegiatan Pertama")
                )
              else
                div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
                  div(cls := "divide-y")(
                    recentActivities.take(5).map { act =>
                      div(cls := "p-3 border-bottom")(
                        div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                          div(cls := "d-flex align-items-center gap-2")(
                            span(cls := "fw-bold text-dark", act.activityType),
                            act.quantity.map(q => span(cls := "badge bg-light text-dark border", q)).getOrElse(modifier())
                          ),
                          span(cls := "small text-muted font-monospace", act.activityDate)
                        ),
                        p(cls := "mb-1 text-secondary small", act.notes),
                        div(cls := "d-flex justify-content-between align-items-center pt-1 small")(
                          span(cls := "text-muted", s"Lahan: ${act.periodCode} · Pelaksana: ${act.farmerName}"),
                          span(cls := "badge-git-immutable",
                            i(cls := "bi bi-lock-fill"), s"Log #${act.id.take(8)}"
                          )
                        )
                      )
                    }
                  )
                )
            ),

            // Kolom Kanan: Cetakan Budidaya dari Mandor
            div(cls := "col-lg-5")(
              div(cls := "mb-4")(
                div(cls := "d-flex justify-content-between align-items-center mb-3")(
                  h3(cls := "section-label mb-0 text-dark", "Cetakan Resmi dari Mandor"),
                  a(cls := "small text-decoration-none text-success fw-medium", href := "/panduan", "Lihat Semua →")
                ),
                if publishedGuides.isEmpty then
                  div(cls := "p-3 border rounded-3 bg-white text-muted small", "Belum ada cetakan resmi yang disahkan mandor.")
                else
                  div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
                    publishedGuides.map { case (g, pub, snap) =>
                      div(cls := "p-3 border-bottom d-flex justify-content-between align-items-center")(
                        div(
                          a(cls := "fw-semibold text-dark text-decoration-none small d-block mb-1", href := s"/panduan/${g.id}", g.name),
                          div(cls := "small text-muted", s"${g.cropVariety} · ${pub.editionTag}")
                        ),
                        a(cls := "btn btn-sm btn-verdant-outline py-1 px-3 text-nowrap", href := s"/panduan/${g.id}", "Buka Cetakan")
                      )
                    }
                  )
              ),
              div(cls := "p-3 rounded-4 bg-white border border-light-subtle shadow-sm")(
                h4(cls := "small fw-bold text-dark text-uppercase tracking-wide mb-1", "🛡️ Mengapa Catatan Anti-Hapus?"),
                p(cls := "small text-muted mb-0",
                  "Seperti commit pada Git, catatan petani tidak bisa diedit atau dihapus diam-diam. Hal ini memberi bukti valid kepada pembeli dan auditor bahwa tanaman dirawat sesuai takaran aman."
                )
              )
            )
          )
        )
      ,

      catatKegiatanModal(periods),
      bukaMasaTanamModal(publishedGuides.map { case (g, pub, _) => (pub, g.name) })
    )

  // ==========================================================================
  // 2. DAFTAR LAHAN SAYA (BERBASIS CETAKAN)
  // ==========================================================================
  def farmerMasaTanamListView(
    periods: List[(CultivationPeriod, String)],
    publishedGuides: List[(PublishedGuide, String)]
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Daftar Lahan Tani Saya"),
          p(cls := "text-muted small mb-0", "Setiap petak lahan terikat pada satu cetakan resmi mandor agar standar budidaya konsisten.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#bukaMasaTanamModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Terapkan Cetakan ke Lahan Baru")
      ),

      if periods.isEmpty then
        div(cls := "p-5 text-center text-muted border rounded-4 bg-white shadow-sm")(
          p(cls := "mb-3", "Belum ada lahan yang terdaftar dengan cetakan budidaya."),
          button(
            cls := "btn btn-verdant",
            data("bs-toggle") := "modal",
            data("bs-target") := "#bukaMasaTanamModal"
          )("Terapkan Cetakan Mandor Sekarang")
        )
      else
        div(cls := "row g-3")(
          periods.map { case (periodItem, gName) =>
            div(cls := "col-md-6")(
              div(cls := "panel-verdant h-100 d-flex flex-column justify-content-between shadow-sm")(
                div(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                    span(cls := "small text-muted fw-bold font-monospace", periodItem.code),
                    span(cls := "badge bg-success-subtle text-success border border-success-subtle", periodItem.status)
                  ),
                  h2(cls := "h5 fw-bold text-dark mb-1", periodItem.cropVariety),
                  p(cls := "small text-muted mb-2", s"Lokasi: ${periodItem.plotName}"),
                  div(cls := "small text-secondary mb-3",
                    span("Cetakan Mandor: "),
                    strong(cls := "text-dark", periodItem.publishedEdition)
                  )
                ),
                div(cls := "pt-3 border-top d-flex gap-2")(
                  a(cls := "btn btn-sm btn-verdant flex-grow-1", href := s"/masa-tanam/${periodItem.code}",
                    i(cls := "bi bi-journal-text me-1"), "Buka Buku Log Lahan"
                  ),
                  a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${periodItem.guideId}", "Lihat Cetakan")
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
  // 3. DETAIL LAHAN & BUKU LOG KEGIATAN KEKAL
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
          li(cls := "breadcrumb-item")(a(href := "/masa-tanam", cls := "text-decoration-none text-muted", "Lahan Saya")),
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
            span(s"Kode Lahan: ${period.code} · Dimulai: ${formatTs(period.startDate)}")
          )
        ),
        button(
          cls := "btn btn-verdant text-nowrap",
          data("bs-toggle") := "modal",
          data("bs-target") := "#catatKegiatanModal"
        )(i(cls := "bi bi-journal-plus me-1"), "Catat Kegiatan di Lahan Ini")
      ),

      // Banner Cetakan Terkunci
      div(cls := "p-3 rounded-4 bg-white border border-success-subtle shadow-sm mb-4")(
        div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-2")(
          div(
            div(cls := "small fw-bold text-dark d-flex align-items-center gap-2",
              i(cls := "bi bi-lock-fill text-success"),
              s"Terkunci Paten pada Cetakan Mandor: ${period.publishedEdition}"
            ),
            div(cls := "small text-muted",
              s"Disahkan oleh ${publishedGuide.approvedBy} (Revisi #${revision.id.take(8)}). Sekalipun mandor mengubah cetakan di masa depan, lahan ini tetap aman memakai takaran ini."
            )
          ),
          a(cls := "btn btn-sm btn-verdant-outline text-nowrap", href := s"/panduan/${guide.id}")(
            "Lihat Detail Cetakan Budidaya →"
          )
        )
      ),

      div(cls := "row g-4")(
        // Kolom Kiri: Garis Waktu Log Lapangan (Anti-Hapus)
        div(cls := "col-lg-7")(
          div(cls := "d-flex justify-content-between align-items-center mb-3")(
            div(
              h2(cls := "section-label mb-0 text-dark", "Buku Log Kegiatan Lapangan"),
              span(cls := "text-muted", style := "font-size: 12px;", "Riwayat kekal tidak dapat dimanipulasi")
            ),
            span(cls := "small text-muted", s"${activities.length} kegiatan dicatat")
          ),
          if activities.isEmpty then
            div(cls := "p-4 text-center text-muted border rounded-4 bg-white small")(
              p(cls := "mb-2", "Belum ada kegiatan yang dicatat pada petak lahan ini."),
              button(
                cls := "btn btn-sm btn-verdant",
                data("bs-toggle") := "modal",
                data("bs-target") := "#catatKegiatanModal"
              )("Catat Kegiatan Pertama")
            )
          else
            div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
              activities.map { act =>
                val complianceWarning = VerdantCore.checkActivityCompliance(act, allParams)
                div(cls := "p-3 border-bottom")(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                    div(cls := "d-flex align-items-center gap-2")(
                      span(cls := "fw-bold text-dark", act.activityType),
                      act.quantity.map(q => span(cls := "badge bg-light text-dark border", q)).getOrElse(modifier())
                    ),
                    span(cls := "small text-muted font-monospace", act.activityDate)
                  ),
                  p(cls := "mb-1 text-secondary small", act.notes),
                  div(cls := "d-flex justify-content-between align-items-center small pt-1")(
                    span(cls := "text-muted", s"Petani: ${act.farmerName}"),
                    div(cls := "d-flex align-items-center gap-2")(
                      complianceWarning match
                        case Some(warning) =>
                          span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle", attr("title") := warning)(
                            i(cls := "bi bi-exclamation-triangle me-1"), "Periksa Takaran"
                          )
                        case None => modifier(),
                      span(cls := "badge-git-immutable",
                        i(cls := "bi bi-lock-fill"), s"Log #${act.id.take(8)}"
                      )
                    )
                  )
                )
              }
            )
        ),

        // Kolom Kanan: Rincian Cetakan Budidaya yang Menjadi Acuan
        div(cls := "col-lg-5")(
          div(cls := "p-3 p-md-4 rounded-4 bg-white border shadow-sm")(
            h2(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-1", "Tahapan Cetakan yang Berlaku"),
            p(cls := "small text-muted mb-3", s"Panduan takaran resmi ${period.publishedEdition}"),
            div(
              snap.stages.zipWithIndex.map { case (s, idx) =>
                div(cls := s"py-3 ${if idx > 0 then "border-top" else ""}")(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                    span(cls := "fw-bold text-dark small", s"${idx + 1}. ${s.name}"),
                    span(cls := "badge bg-light text-dark border", s"${s.durationDays} Hari")
                  ),
                  p(cls := "small text-secondary mb-2", s.instructions),
                  ul(cls := "list-unstyled mb-0 small text-muted")(
                    s.parameters.map { p =>
                      li(
                        span(cls := "fw-medium text-dark", p.name),
                        s": ${p.targetValue} ${p.unit} ",
                        span(cls := "text-muted", s"[Batas: ${p.minValue}–${p.maxValue}]")
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
  // 4. KATALOG CETAKAN RESMI MANDOR (PETANI)
  // ==========================================================================
  def farmerPanduanListView(
    guides: List[(Database#GuideRow, PublishedGuide, GuideSnapshot)]
  ): Modifier =
    div(
      div(cls := "pb-3 mb-4 border-bottom")(
        div(cls := "d-flex align-items-center gap-2 mb-1")(
          span(cls := "badge bg-success-subtle text-success border border-success-subtle px-2 py-1 small rounded-pill", "📐 Resep Mandor"),
          span(cls := "small text-muted", "Standar Operasional Baku")
        ),
        h1(cls := "h4 fw-bold text-dark mb-1", "Katalog Cetakan Budidaya Mandor"),
        p(cls := "text-muted small mb-0", "Pilih cetakan tanaman di bawah ini untuk diterapkan langsung ke lahan pertanian Anda.")
      ),

      if guides.isEmpty then
        div(cls := "p-5 text-center text-muted border rounded-4 bg-white shadow-sm")(
          "Belum ada cetakan resmi yang disahkan oleh mandor."
        )
      else
        div(cls := "row g-3")(
          guides.map { case (g, pub, snap) =>
            val summary = VerdantCore.summarizeGuide(snap)
            div(cls := "col-md-6")(
              div(cls := "panel-verdant h-100 d-flex flex-column justify-content-between shadow-sm")(
                div(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                    span(cls := "small text-muted fw-bold", g.cropVariety),
                    span(cls := "badge-cetakan", pub.editionTag)
                  ),
                  h2(cls := "h5 fw-bold text-dark mb-1", g.name),
                  p(cls := "small text-muted mb-3", s"Total Durasi: ${summary.totalDurationDays} Hari · ${summary.totalStages} Tahapan Budidaya")
                ),
                div(cls := "pt-3 border-top d-flex gap-2")(
                  a(cls := "btn btn-sm btn-verdant flex-grow-1", href := s"/panduan/${g.id}", "Pelajari Cetakan"),
                  button(
                    cls := "btn btn-sm btn-verdant-outline",
                    data("bs-toggle") := "modal",
                    data("bs-target") := "#bukaMasaTanamModal"
                  )("Terapkan ke Lahan")
                )
              )
            )
          }
        )
      ,

      bukaMasaTanamModal(guides.map { case (g, pub, _) => (pub, g.name) })
    )

  // ==========================================================================
  // 5. DETAIL CETAKAN RESMI MANDOR (PETANI - READ ONLY)
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
          li(cls := "breadcrumb-item")(a(href := "/panduan", cls := "text-decoration-none text-muted", "Katalog Cetakan")),
          li(cls := "breadcrumb-item active text-dark", guide.name)
        )
      ),

      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            h1(cls := "h4 fw-bold text-dark mb-0", guide.name),
            span(cls := "badge-cetakan", published.editionTag)
          ),
          div(cls := "small text-muted", s"Komoditas: ${guide.cropVariety} · ${summary.totalStages} Tahapan · Total Durasi: ${summary.totalDurationDays} Hari")
        ),
        button(
          cls := "btn btn-verdant text-nowrap",
          data("bs-toggle") := "modal",
          data("bs-target") := "#bukaMasaTanamModal"
        )(i(cls := "bi bi-pin-angle me-1"), "Terapkan Cetakan Ini ke Lahan")
      ),

      // Info Pengesahan Edisi Resmi
      div(cls := "p-3 rounded-4 bg-white border border-success-subtle shadow-sm mb-4")(
        div(cls := "small text-muted")(
          strong(cls := "text-dark", s"Edisi Resmi: ${published.editionTag}"),
          span(s" · Disahkan oleh: ${published.approvedBy} · Catatan Mandor: ${published.notes}")
        )
      ),

      // Daftar Tahapan Budidaya
      div(cls := "mb-4")(
        h2(cls := "section-label mb-3 text-dark", "Rincian Instruksi Lapangan & Takaran Rekomendasi"),
        div(cls := "border-top")(
          snapshot.stages.zipWithIndex.map { case (stage, idx) =>
            div(cls := "py-4 border-bottom")(
              div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                h3(cls := "h6 fw-bold text-dark mb-0", s"${idx + 1}. ${stage.name}"),
                span(cls := "badge bg-light text-dark border", s"Durasi: ${stage.durationDays} Hari")
              ),
              p(cls := "small text-secondary mb-3",
                strong(cls := "text-dark", "Instruksi: "),
                stage.instructions
              ),
              div(cls := "border rounded-3 overflow-hidden bg-white shadow-sm")(
                table(cls := "table table-sm table-borderless mb-0 small")(
                  thead(cls := "table-light text-muted border-bottom")(
                    tr(
                      th(cls := "py-2 px-3 fw-medium", "Parameter Takaran"),
                      th(cls := "py-2 fw-medium", "Target Rekomendasi Mandor"),
                      th(cls := "py-2 px-3 fw-medium text-end", "Batas Rentang Aman")
                    )
                  ),
                  tbody(
                    stage.parameters.map { p =>
                      tr(
                        td(cls := "py-2 px-3 fw-semibold text-dark", p.name),
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
