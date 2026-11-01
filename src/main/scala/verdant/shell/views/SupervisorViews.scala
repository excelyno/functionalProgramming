package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.nav
import verdant.core.*
import verdant.shell.Database

/**
 * ============================================================================
 * SUPERVISOR VIEWS: Ruang Kerja Mandor / Supervisor Agronomi
 * ============================================================================
 * 
 * Modul ini menangani seluruh antarmuka kendali mutu bagi Mandor:
 * 1. supervisorBerandaView               -> Dashboard Mandor: draf menunggu review, panduan aktif, dan aksi cepat.
 * 2. supervisorKelolaPanduanView         -> Tabel master daftar panduan, status revisi, dan aksi kelola.
 * 3. supervisorPemeriksaanPerubahanView  -> Daftar perbedaan draf (Human-Readable Diff) sebelum persetujuan.
 * 4. supervisorPanduanDetailView         -> Editor parameter agronomi (membuat revisi draf baru secara non-destruktif).
 * 5. supervisorTinjauPerubahanView       -> Halaman pengambilan keputusan: Setujui & Terbitkan vs Tolak Draf.
 * 6. supervisorRiwayatView               -> Silsilah silsilah pohon revisi (audit trail) dan fitur pemulihan versi (Rollback).
 * 
 * KONSEP AGRO-INFORMATIKA (SISTEM REVISI):
 * Di Verdant, perubahan rekomendasi pupuk/air oleh mandor TIDAK PERNAH menimpa data lama (Immutable).
 * Setiap modifikasi menciptakan `GuideRevision` baru dengan hash SHA-256 unik.
 * Petani yang sedang menanam tetap aman dengan versi lama yang terkunci,
 * sampai mandor resmi menerbitkan Edisi Baru.
 */
trait SupervisorViews extends ViewHelpers with SharedModals:

  // ==========================================================================
  // 1. BERANDA MANDOR (OVERVIEW & ACTION CENTER)
  // Dipanggil dari: Main.scala baris 112 (GET /)
  // ==========================================================================
  def supervisorBerandaView(
    draftsAwaiting: List[(Database#GuideRow, GuideRevision, Option[PublishedGuide])],
    publishedGuides: List[(PublishedGuide, String)],
    recentRevisions: List[(GuideRevision, String)]
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Beranda Mandor"),
          p(cls := "text-muted small mb-0", "Kelola draf panduan agronomi, periksa perbandingan perubahan, dan terbitkan edisi resmi.")
        ),
        div(cls := "d-flex gap-2")(
          a(cls := "btn btn-verdant-outline", href := "/pemeriksaan-perubahan")(
            i(cls := "bi bi-arrow-left-right me-1"), "Pemeriksaan Perubahan"
          ),
          button(
            cls := "btn btn-verdant",
            data("bs-toggle") := "modal",
            data("bs-target") := "#tambahPanduanModal"
          )(i(cls := "bi bi-plus-lg me-1"), "Buat Panduan Baru")
        )
      ),

      // SECTION 1: DRAFTS AWAITING REVIEW (TOP PRIORITY)
      div(cls := "mb-4")(
        div(cls := "d-flex justify-content-between align-items-center mb-3")(
          h2(cls := "section-label mb-0", "Draf Menunggu Pemeriksaan & Persetujuan"),
          span(cls := s"badge ${if draftsAwaiting.nonEmpty then "bg-warning text-dark" else "bg-light text-muted border"}", s"${draftsAwaiting.length} Draf")
        ),
        if draftsAwaiting.isEmpty then
          div(cls := "p-4 text-center border rounded-3 bg-white text-muted small")(
            i(cls := "bi bi-check-circle text-success fs-4 d-block mb-1"),
            "Seluruh draf panduan sudah diterbitkan sebagai edisi resmi. Tidak ada draf yang menunggu persetujuan saat ini."
          )
        else
          div(cls := "border rounded-3 overflow-hidden bg-white")(
            draftsAwaiting.map { case (g, rev, pubOpt) =>
              div(cls := "p-3 border-bottom d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3")(
                div(
                  div(cls := "d-flex align-items-center gap-2 mb-1")(
                    a(cls := "fw-bold text-dark text-decoration-none", href := s"/panduan/${g.id}", g.name),
                    span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle small", "Draf Menunggu Tinjauan")
                  ),
                  p(cls := "small text-secondary mb-1", s"Catatan Perubahan: ${rev.changeNotes}"),
                  div(cls := "small text-muted", s"Penyusun: ${rev.author} · Waktu: ${formatTs(rev.timestamp)}")
                ),
                div(cls := "d-flex gap-2")(
                  a(cls := "btn btn-sm btn-verdant", href := s"/panduan/${g.id}/tinjau")("Periksa Perubahan (Diff) →")
                )
              )
            }
          )
      ),

      // SECTION 2: PUBLISHED GUIDES SUMMARY
      div(cls := "row g-4")(
        div(cls := "col-lg-7")(
          div(cls := "mb-4")(
            div(cls := "d-flex justify-content-between align-items-center mb-3")(
              h2(cls := "section-label mb-0", "Panduan Resmi Aktif di Kebun"),
              a(cls := "small text-decoration-none text-success fw-medium", href := "/kelola-panduan", "Kelola Semua Panduan →")
            ),
            if publishedGuides.isEmpty then
              div(cls := "p-4 text-center border rounded-3 bg-white text-muted small", "Belum ada panduan resmi yang diterbitkan.")
            else
              div(cls := "border rounded-3 overflow-hidden bg-white")(
                publishedGuides.map { case (pub, gName) =>
                  div(cls := "p-3 border-bottom d-flex justify-content-between align-items-center")(
                    div(
                      a(cls := "fw-semibold text-dark text-decoration-none small d-block mb-1", href := s"/panduan/${pub.guideId}", gName),
                      div(cls := "small text-muted", s"${pub.editionTag} · Disetujui: ${pub.approvedBy}")
                    ),
                    a(cls := "btn btn-sm btn-verdant-outline py-1 px-2 text-nowrap", href := s"/panduan/${pub.guideId}", "Kelola Draf")
                  )
                }
              )
          )
        ),

        div(cls := "col-lg-5")(
          div(cls := "p-3 p-md-4 rounded-3 bg-light border mb-4")(
            h3(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-2", "Tata Kelola Panduan Resmi"),
            p(cls := "small text-muted mb-3", "Alur kerja mandor menjamin ketepatan takaran sebelum diterapkan di lapangan:"),
            ol(cls := "small text-secondary ps-3 mb-0")(
              li(cls := "mb-2", strong(cls := "text-dark", "Susun Draf: "), "Ubah rekomendasi durasi atau takaran anjuran."),
              li(cls := "mb-2", strong(cls := "text-dark", "Periksa Perubahan: "), "Baca ringkasan diff dalam bahasa ramah dan uji kelayakan batas aman."),
              li(strong(cls := "text-dark", "Publikasikan: "), "Kunci revisi menjadi Edisi Resmi baru untuk masa tanam mendatang.")
            )
          )
        )
      ),

      tambahPanduanModal()
    )

  // ==========================================================================
  // 2. KELOLA SEMUA PANDUAN BUDIDAYA
  // Dipanggil dari: Main.scala baris 231 (GET /kelola-panduan)
  // ==========================================================================
  def supervisorKelolaPanduanView(
    guides: List[(Database#GuideRow, GuideRevision, Option[PublishedGuide])]
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Kelola Panduan Budidaya"),
          p(cls := "text-muted small mb-0", "Daftar panduan agronomi, draf revisi internal, dan edisi resmi yang berlaku.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#tambahPanduanModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Buat Panduan Baru")
      ),

      div(cls := "border rounded-3 overflow-hidden bg-white")(
        table(cls := "table table-hover align-middle mb-0 small")(
          thead(cls := "table-light text-muted")(
            tr(
              th(cls := "py-2 px-3 fw-medium", "Nama Panduan"),
              th(cls := "py-2 fw-medium", "Komoditas"),
              th(cls := "py-2 fw-medium", "Edisi Resmi Berlaku"),
              th(cls := "py-2 fw-medium", "Status Draf"),
              th(cls := "py-2 px-3 fw-medium text-end", "Aksi Mandor")
            )
          ),
          tbody(
            guides.map { case (g, rev, pubOpt) =>
              val hasUnpublishedDraft = pubOpt.isEmpty || pubOpt.exists(_.revisionId != rev.id)
              tr(
                td(cls := "py-2 px-3",
                  a(cls := "fw-semibold text-dark text-decoration-none", href := s"/panduan/${g.id}", g.name),
                  div(cls := "text-muted", style := "font-size: 11px;", s"Revisi: #${rev.id.take(8)}")
                ),
                td(cls := "py-2 text-muted", g.cropVariety),
                td(cls := "py-2",
                  pubOpt match
                    case Some(pub) => span(cls := "badge bg-success-subtle text-success border border-success-subtle", pub.editionTag)
                    case None      => span(cls := "badge bg-light text-muted border", "Belum Diterbitkan")
                ),
                td(cls := "py-2",
                  if hasUnpublishedDraft then
                    span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle", "Ada Draf Baru")
                  else
                    span(cls := "badge bg-light text-muted border", "Telah Diterbitkan")
                ),
                td(cls := "py-2 px-3 text-end")(
                  div(cls := "d-inline-flex gap-1")(
                    a(cls := "btn btn-sm btn-verdant-outline py-1 px-2", href := s"/panduan/${g.id}", "Ubah Draf"),
                    if hasUnpublishedDraft then
                      a(cls := "btn btn-sm btn-verdant py-1 px-2", href := s"/panduan/${g.id}/tinjau", "Tinjau")
                    else modifier()
                  )
                )
              )
            }
          )
        )
      ),

      tambahPanduanModal()
    )

  // ==========================================================================
  // 3. PEMERIKSAAN PERUBAHAN DRAF (HUMAN-READABLE DIFF LIST)
  // Dipanggil dari: Main.scala baris 296 (GET /pemeriksaan-perubahan)
  // ==========================================================================
  def supervisorPemeriksaanPerubahanView(
    draftGuides: List[(Database#GuideRow, GuideRevision, Option[GuideSnapshot], ChangeSummary, Option[PublishedGuide])]
  ): Modifier =
    div(
      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Pemeriksaan Perubahan Draf"),
        p(cls := "text-muted small mb-0", "Tinjau perbedaan rekomendasi takaran sebelum menyetujui dan menerbitkan edisi resmi baru.")
      ),

      if draftGuides.isEmpty then
        div(cls := "p-5 text-center text-muted border rounded-3 bg-white")(
          i(cls := "bi bi-check2-all text-success fs-2 d-block mb-2"),
          h2(cls := "h6 fw-bold text-dark mb-1", "Seluruh Draf Telah Diterbitkan"),
          p(cls := "small mb-0", "Tidak ada draf panduan yang sedang menunggu peninjauan atau persetujuan saat ini.")
        )
      else
        div(cls := "row g-4")(
          draftGuides.map { case (g, currRev, baseSnapOpt, diffSummary, pubOpt) =>
            div(cls := "col-12")(
              div(cls := "panel-verdant border-warning-subtle")(
                div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 mb-3 pb-3 border-bottom")(
                  div(
                    div(cls := "d-flex align-items-center gap-2 mb-1")(
                      h2(cls := "h5 fw-bold text-dark mb-0", g.name),
                      span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle", "Draf Menunggu Persetujuan")
                    ),
                    div(cls := "small text-muted", s"Komoditas: ${g.cropVariety} · Penyusun: ${currRev.author} · Catatan: ${currRev.changeNotes}")
                  ),
                  div(cls := "d-flex gap-2")(
                    a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${g.id}", "Ubah Draf"),
                    a(cls := "btn btn-sm btn-verdant", href := s"/panduan/${g.id}/tinjau", "Tinjau & Putuskan →")
                  )
                ),

                h3(cls := "section-label mb-2", "Perubahan Takaran & Parameter (Human-Readable Diff)"),
                if diffSummary.changes.isEmpty then
                  div(cls := "p-3 bg-light rounded-3 text-muted small mb-3", "Tidak ada perubahan parameter teknis dibandingkan versi sebelumnya.")
                else
                  div(cls := "border rounded-3 overflow-hidden bg-white mb-3")(
                    diffSummary.changes.map { c =>
                      div(cls := "p-3 border-bottom")(
                        div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                          span(cls := "fw-semibold text-dark small", s"${c.stageName} ➔ ${c.paramName}"),
                          span(cls := "badge bg-light text-dark border", c.changeType)
                        ),
                        p(cls := "mb-0 text-secondary small", c.humanDescription)
                      )
                    }
                  )
              )
            )
          }
        )
    )

  // ==========================================================================
  // 4. DETAIL PANDUAN & EDITOR DRAF PARAMETER MANDOR
  // Dipanggil dari: Main.scala baris 214 (GET /panduan/:id oleh Mandor)
  // Form mengarah ke: POST /panduan/:id/perbarui (Main.scala baris 301)
  // ==========================================================================
  def supervisorPanduanDetailView(
    guide: Database#GuideRow,
    latestRevision: GuideRevision,
    publishedGuideOpt: Option[PublishedGuide],
    summary: GuideSummary
  ): Modifier =
    val snap = latestRevision.snapshot
    val hasUnpublishedDraft = publishedGuideOpt.isEmpty || publishedGuideOpt.exists(_.revisionId != latestRevision.id)
    div(
      nav(aria.label := "breadcrumb", cls := "mb-3")(
        ol(cls := "breadcrumb small text-muted mb-0")(
          li(cls := "breadcrumb-item")(a(href := "/kelola-panduan", cls := "text-decoration-none text-muted", "Kelola Panduan")),
          li(cls := "breadcrumb-item active text-dark", guide.name)
        )
      ),

      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            h1(cls := "h4 fw-bold text-dark mb-0", guide.name),
            if hasUnpublishedDraft then
              span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle", "Draf Belum Terbit")
            else
              span(cls := "badge bg-success-subtle text-success border border-success-subtle", "Edisi Resmi")
          ),
          div(cls := "small text-muted", s"Komoditas: ${guide.cropVariety} · ${summary.totalStages} Tahapan · Total Durasi: ${summary.totalDurationDays} Hari")
        ),
        div(cls := "d-flex gap-2")(
          if hasUnpublishedDraft then
            a(cls := "btn btn-sm btn-verdant", href := s"/panduan/${guide.id}/tinjau")("Tinjau & Publikasikan Draf →")
          else modifier(),
          button(
            cls := "btn btn-sm btn-verdant-outline",
            data("bs-toggle") := "collapse",
            data("bs-target") := "#areaMandorCollapse"
          )("Edit Parameter Draf")
        )
      ),

      // Area Form Edit Draf Mandor (Bisa dibuka/tutup)
      div(cls := s"collapse ${if hasUnpublishedDraft then "show" else ""} mb-4", id := "areaMandorCollapse")(
        div(cls := "p-3 p-md-4 rounded-3 bg-light border")(
          h2(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-1", "Ubah Nilai Anjuran Parameter (Draf Mandor)"),
          p(cls := "small text-muted mb-3", "Perubahan akan disimpan sebagai revisi internal baru. Petani tidak akan melihat perubahan ini sebelum Anda menyetujui dan menerbitkannya secara resmi."),
          form(action := s"/panduan/${guide.id}/perbarui", method := "POST")(
            div(cls := "row g-3 mb-3")(
              div(cls := "col-md-6")(
                label(cls := "form-label small fw-medium text-muted", "Nama Mandor / Penyusun"),
                input(`type` := "text", name := "author", cls := "form-control form-control-sm", placeholder := "Misal: Ir. Budi Santoso", required := true)
              ),
              div(cls := "col-md-6")(
                label(cls := "form-label small fw-medium text-muted", "Alasan / Catatan Perubahan"),
                input(`type` := "text", name := "changeNotes", cls := "form-control form-control-sm", placeholder := "Misal: Menaikkan nutrisi EC untuk fase pembungaan musim hujan", required := true)
              )
            ),
            div(cls := "row g-3 mb-3")(
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-medium text-muted", "Pilih Tahap Budidaya"),
                select(name := "stageName", cls := "form-select form-select-sm")(
                  snap.stages.map { s => option(value := s.name, s.name) }
                )
              ),
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-medium text-muted", "Pilih Parameter"),
                select(name := "paramName", cls := "form-select form-select-sm")(
                  snap.stages.flatMap(_.parameters).map(_.name).distinct.map { pName =>
                    option(value := pName, pName)
                  }
                )
              ),
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-medium text-muted", "Nilai Target Rekomendasi Baru"),
                input(`type` := "number", step := "0.01", name := "newTargetValue", cls := "form-control form-control-sm", placeholder := "Contoh: 2.8", required := true)
              )
            ),
            div(cls := "text-end")(
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Simpan Draf & Periksa Perubahan")
            )
          )
        )
      ),

      // Daftar Tahapan Budidaya Saat Ini
      div(cls := "mb-4")(
        h2(cls := "section-label mb-3", "Tahapan Budidaya & Parameter Saat Ini"),
        div(cls := "border-top")(
          snap.stages.zipWithIndex.map { case (stage, idx) =>
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
      )
    )

  // ==========================================================================
  // 5. TINJAU PERUBAHAN & PUBLIKASIKAN ATAU TOLAK DRAF
  // Dipanggil dari: Main.scala baris 361 (GET /panduan/:id/tinjau)
  // Form Terbitkan -> POST /panduan/:id/terbitkan (Main.scala baris 366)
  // Form Tolak     -> POST /panduan/:id/tolak (Main.scala baris 386)
  // ==========================================================================
  def supervisorTinjauPerubahanView(
    guide: Database#GuideRow,
    currentRevision: GuideRevision,
    parentRevisionOpt: Option[GuideRevision],
    diffSummary: ChangeSummary,
    publishedOpt: Option[PublishedGuide]
  ): Modifier =
    div(
      nav(aria.label := "breadcrumb", cls := "mb-3")(
        ol(cls := "breadcrumb small text-muted mb-0")(
          li(cls := "breadcrumb-item")(a(href := "/kelola-panduan", cls := "text-decoration-none text-muted", "Kelola Panduan")),
          li(cls := "breadcrumb-item")(a(href := s"/panduan/${guide.id}", cls := "text-decoration-none text-muted", guide.name)),
          li(cls := "breadcrumb-item active text-dark", "Tinjauan Perubahan")
        )
      ),

      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Tinjauan Perubahan: Apa yang Berubah?"),
        p(cls := "text-muted small mb-0", s"Pemeriksaan perubahan draf untuk '${guide.name}'. Anda dapat menyetujui dan menerbitkan edisi resmi baru, atau menolak draf ini.")
      ),

      // Human-readable Diff Box
      div(cls := "mb-4")(
        h2(cls := "section-label mb-3", "Rangkuman Perubahan Takaran"),
        if diffSummary.changes.isEmpty then
          div(cls := "p-4 text-center text-muted border rounded-3 bg-white small mb-4", "Tidak ada perbedaan parameter teknis.")
        else
          div(cls := "border rounded-3 overflow-hidden bg-white mb-4")(
            diffSummary.changes.map { c =>
              div(cls := "p-3 border-bottom")(
                div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                  span(cls := "fw-semibold text-dark", s"${c.stageName} ➔ ${c.paramName}"),
                  span(cls := "badge bg-light text-dark border", c.changeType)
                ),
                p(cls := "mb-1 text-secondary small", c.humanDescription),
                div(cls := "small text-muted",
                  s"Sebelumnya: ${c.oldValue.map(_.toString).getOrElse("-")} ${c.unit} ➔ Nilai baru: ${c.newValue.map(_.toString).getOrElse("-")} ${c.unit}"
                )
              )
            }
          )
      ),

      // Action Area: Approve & Publish vs Reject Draft
      div(cls := "row g-4")(
        // Kotak 1: Setujui & Terbitkan
        div(cls := "col-md-7")(
          div(cls := "p-3 p-md-4 rounded-3 bg-light border")(
            h2(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-1", "Setujui & Publikasikan Edisi Resmi"),
            p(cls := "small text-muted mb-3", "Setelah diterbitkan, draf ini akan menjadi edisi resmi baru yang dapat dipilih petani saat memulai masa tanam."),
            form(action := s"/panduan/${guide.id}/terbitkan", method := "POST")(
              input(`type` := "hidden", name := "revisionId", value := currentRevision.id),
              div(cls := "row g-3 mb-3")(
                div(cls := "col-md-6")(
                  label(cls := "form-label small fw-medium text-muted", "Nama Edisi Resmi Baru"),
                  input(`type` := "text", name := "editionTag", cls := "form-control form-control-sm", placeholder := "Contoh: Edisi 2026.2 - Musim Hujan", required := true)
                ),
                div(cls := "col-md-6")(
                  label(cls := "form-label small fw-medium text-muted", "Nama Mandor / Pengesah"),
                  input(`type` := "text", name := "approvedBy", cls := "form-control form-control-sm", placeholder := "Contoh: H. Suwandi (Kepala Kebun)", required := true)
                )
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Catatan Persetujuan"),
                textarea(name := "notes", cls := "form-control form-control-sm", rows := 2, placeholder := "Contoh: Telah diverifikasi sesuai kondisi iklim basah dan siap diterapkan.")
              ),
              div(cls := "text-end")(
                button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Setujui & Terbitkan Sekarang")
              )
            )
          )
        ),

        // Kotak 2: Tolak Draf
        div(cls := "col-md-5")(
          div(cls := "p-3 p-md-4 rounded-3 bg-white border border-danger-subtle")(
            h2(cls := "h6 fw-bold text-danger text-uppercase tracking-wide mb-1", "Tolak / Batalkan Draf"),
            p(cls := "small text-muted mb-3", "Batalkan draf perubahan ini dan kembalikan panduan ke edisi resmi terakhir yang telah terbit."),
            form(action := s"/panduan/${guide.id}/tolak", method := "POST")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Nama Mandor Penolak"),
                input(`type` := "text", name := "rejectedBy", cls := "form-control form-control-sm", placeholder := "Misal: Mandor Pemeriksa", required := true)
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Alasan Penolakan"),
                input(`type` := "text", name := "reason", cls := "form-control form-control-sm", placeholder := "Misal: Takaran EC terlalu tinggi untuk fase awal", required := true)
              ),
              div(cls := "text-end")(
                button(`type` := "submit", cls := "btn btn-sm btn-outline-danger", "Tolak & Batalkan Draf")
              )
            )
          )
        )
      )
    )

  // ==========================================================================
  // 6. RIWAYAT PANDUAN & SILSILAH REVISI (AUDIT TRAIL & RESTORE)
  // Dipanggil dari: Main.scala baris 412 (GET /riwayat)
  // Form Pulihkan -> POST /riwayat/pulihkan (Main.scala baris 417)
  // ==========================================================================
  def supervisorRiwayatView(
    revisions: List[GuideRevision],
    publishedEditions: List[(PublishedGuide, String)]
  ): Modifier =
    div(
      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Riwayat Panduan & Silsilah Revisi"),
        p(cls := "text-muted small mb-0", "Lacak seluruh jejak perubahan panduan budidaya serta riwayat pemulihan versi terdahulu.")
      ),

      div(cls := "row g-4")(
        // Kolom Kiri: Riwayat Edisi Panduan Resmi
        div(cls := "col-md-5")(
          div(cls := "mb-4")(
            h2(cls := "section-label mb-3", "Riwayat Edisi Panduan Resmi"),
            if publishedEditions.isEmpty then
              div(cls := "p-4 text-center text-muted border rounded-3 bg-white small", "Belum ada edisi resmi yang diterbitkan.")
            else
              div(cls := "border rounded-3 overflow-hidden bg-white")(
                publishedEditions.map { case (pub, gName) =>
                  div(cls := "p-3 border-bottom")(
                    div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                      span(cls := "badge bg-success-subtle text-success border border-success-subtle", pub.editionTag),
                      span(cls := "small text-muted", formatTs(pub.publishedAt))
                    ),
                    h3(cls := "h6 fw-semibold text-dark mb-1", gName),
                    p(cls := "small text-secondary mb-1", pub.notes),
                    div(cls := "small text-muted", s"Disetujui oleh: ${pub.approvedBy}")
                  )
                }
              )
          )
        ),

        // Kolom Kanan: Log Revisi & Pemulihan Versi
        div(cls := "col-md-7")(
          div(cls := "mb-4")(
            h2(cls := "section-label mb-3", "Catatan Revisi & Pemulihan Versi"),
            if revisions.isEmpty then
              div(cls := "p-4 text-center text-muted border rounded-3 bg-white small", "Belum ada riwayat revisi.")
            else
              div(cls := "border rounded-3 overflow-hidden bg-white")(
                revisions.zipWithIndex.map { case (rev, idx) =>
                  div(cls := "p-3 border-bottom")(
                    div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                      span(cls := "fw-semibold text-dark", rev.changeNotes),
                      span(cls := "small text-muted", formatTs(rev.timestamp))
                    ),
                    div(cls := "small text-muted mb-2", s"Penyusun: ${rev.author} · Hash: #${rev.id.take(8)}"),
                    if idx == 0 then
                      span(cls := "small text-success fw-medium", "● Versi Draf Terkini")
                    else
                      form(action := s"/riwayat/pulihkan", method := "POST", cls := "d-inline")(
                        input(`type` := "hidden", name := "targetRevisionId", value := rev.id),
                        input(`type` := "hidden", name := "author", value := "Mandor Pemulih"),
                        button(
                          `type` := "submit",
                          cls := "btn btn-sm btn-verdant-outline py-1 px-2 text-danger border-danger-subtle",
                          onclick := "return confirm('Apakah Anda yakin ingin memulihkan panduan ke versi revisi ini?');"
                        )("Kembalikan ke Versi Ini")
                      )
                  )
                }
              )
          )
        )
      )
    )

