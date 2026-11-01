package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.nav
import verdant.core.*
import verdant.shell.Database

/**
 * ============================================================================
 * SUPERVISOR VIEWS: Ruang Kerja Mandor (Pusat Cetakan Budidaya / SOP Master)
 * ============================================================================
 * 
 * Tanggung Jawab Mandor:
 * 1. Merancang "Cetakan Budidaya" (SOP) dengan target takaran pupuk, air, dan durasi per fase.
 * 2. Mengubah nilai anjuran draf secara aman (tidak merusak lahan petani yang sedang berjalan).
 * 3. Meninjau ringkasan perbedaan (Human-readable Diff) lalu menyahkan "Edisi Resmi Baru".
 * 4. Melacak riwayat silsilah revisi dan melakukan pemulihan (*rollback*) jika ada kekeliruan.
 */
trait SupervisorViews extends ViewHelpers with SharedModals:

  // ==========================================================================
  // 1. BERANDA MANDOR (OVERVIEW CETAKAN & REVIEW DRAF)
  // ==========================================================================
  def supervisorBerandaView(
    draftsAwaiting: List[(Database#GuideRow, GuideRevision, Option[PublishedGuide])],
    publishedGuides: List[(PublishedGuide, String)],
    recentRevisions: List[(GuideRevision, String)]
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            span(cls := "badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1 small rounded-pill", "📐 Mode Mandor"),
            span(cls := "small text-muted", "Perancang Cetakan Budidaya & Kebijakan")
          ),
          h1(cls := "h4 fw-bold text-dark mb-1", "Pusat Kendali Cetakan Budidaya"),
          p(cls := "text-muted small mb-0", "Kelola standar cetakan budidaya (SOP Master) untuk acuan petani di kebun.")
        ),
        div(cls := "d-flex gap-2")(
          a(cls := "btn btn-verdant-outline", href := "/pemeriksaan-perubahan")(
            i(cls := "bi bi-arrow-left-right me-1"), "Periksa Perubahan Draf"
          ),
          button(
            cls := "btn btn-verdant",
            data("bs-toggle") := "modal",
            data("bs-target") := "#tambahPanduanModal"
          )(i(cls := "bi bi-plus-lg me-1"), "Buat Cetakan Baru")
        )
      ),

      // SECTION 1: DRAFTS AWAITING REVIEW
      div(cls := "mb-4")(
        div(cls := "d-flex justify-content-between align-items-center mb-3")(
          h2(cls := "section-label mb-0 text-dark", "Draf Cetakan Menunggu Pengesahan Mandor"),
          span(cls := s"badge ${if draftsAwaiting.nonEmpty then "bg-warning text-dark" else "bg-light text-muted border"}", s"${draftsAwaiting.length} Draf")
        ),
        if draftsAwaiting.isEmpty then
          div(cls := "p-4 text-center border rounded-4 bg-white text-muted small shadow-sm")(
            i(cls := "bi bi-check-circle text-success fs-3 d-block mb-1"),
            "Seluruh cetakan budidaya telah diterbitkan secara resmi. Tidak ada draf yang menunggu persetujuan."
          )
        else
          div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
            draftsAwaiting.map { case (g, rev, pubOpt) =>
              div(cls := "p-3 border-bottom d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3")(
                div(
                  div(cls := "d-flex align-items-center gap-2 mb-1")(
                    a(cls := "fw-bold text-dark text-decoration-none", href := s"/panduan/${g.id}", g.name),
                    span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle small", "Draf Belum Sah")
                  ),
                  p(cls := "small text-secondary mb-1", s"Catatan Revisi: ${rev.changeNotes}"),
                  div(cls := "small text-muted", s"Penyusun: ${rev.author} · Waktu: ${formatTs(rev.timestamp)}")
                ),
                div(cls := "d-flex gap-2")(
                  a(cls := "btn btn-sm btn-verdant", href := s"/panduan/${g.id}/tinjau")("Tinjau & Sahkan Edisi Baru →")
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
              h2(cls := "section-label mb-0 text-dark", "Cetakan Resmi yang Sedang Dipakai Petani"),
              a(cls := "small text-decoration-none text-success fw-medium", href := "/kelola-panduan", "Kelola Semua →")
            ),
            if publishedGuides.isEmpty then
              div(cls := "p-4 text-center border rounded-4 bg-white text-muted small shadow-sm", "Belum ada cetakan resmi yang disahkan.")
            else
              div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
                publishedGuides.map { case (pub, gName) =>
                  div(cls := "p-3 border-bottom d-flex justify-content-between align-items-center")(
                    div(
                      a(cls := "fw-bold text-dark text-decoration-none small d-block mb-1", href := s"/panduan/${pub.guideId}", gName),
                      div(cls := "small text-muted", s"${pub.editionTag} · Disahkan oleh: ${pub.approvedBy}")
                    ),
                    a(cls := "btn btn-sm btn-verdant-outline py-1 px-3 text-nowrap", href := s"/panduan/${pub.guideId}", "Edit Draf Cetakan")
                  )
                }
              )
          )
        ),

        div(cls := "col-lg-5")(
          div(cls := "p-3 p-md-4 rounded-4 bg-white border shadow-sm mb-4")(
            h3(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-2", "Prinsip Cetakan Mandor"),
            p(cls := "small text-muted mb-3", "Perubahan takaran oleh mandor dilakukan secara bertahap dan aman:"),
            ol(cls := "small text-secondary ps-3 mb-0")(
              li(cls := "mb-2", strong(cls := "text-dark", "Susun Draf Baru: "), "Ubah rekomendasi durasi atau nutrisi tanpa mengganggu petani."),
              li(cls := "mb-2", strong(cls := "text-dark", "Periksa Diff: "), "Bandingkan angka lama vs angka baru dalam kalimat ramah."),
              li(strong(cls := "text-dark", "Rilis Edisi Resmi: "), "Setelah disahkan, cetakan baru dapat langsung dipilih oleh petani.")
            )
          )
        )
      ),

      tambahPanduanModal()
    )

  // ==========================================================================
  // 2. KELOLA SEMUA CETAKAN BUDIDAYA
  // ==========================================================================
  def supervisorKelolaPanduanView(
    guides: List[(Database#GuideRow, GuideRevision, Option[PublishedGuide])]
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Kelola Cetakan Budidaya (SOP Master)"),
          p(cls := "text-muted small mb-0", "Daftar seluruh cetakan tanaman, versi resmi yang berlaku, dan draf pembaruan mandor.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#tambahPanduanModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Buat Cetakan Baru")
      ),

      div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
        table(cls := "table table-hover align-middle mb-0 small")(
          thead(cls := "table-light text-muted")(
            tr(
              th(cls := "py-3 px-3 fw-semibold", "Nama Cetakan Budidaya"),
              th(cls := "py-3 fw-semibold", "Komoditas"),
              th(cls := "py-3 fw-semibold", "Edisi Resmi Berlaku"),
              th(cls := "py-3 fw-semibold", "Status Draf"),
              th(cls := "py-3 px-3 fw-semibold text-end", "Aksi Mandor")
            )
          ),
          tbody(
            guides.map { case (g, rev, pubOpt) =>
              val hasUnpublishedDraft = pubOpt.isEmpty || pubOpt.exists(_.revisionId != rev.id)
              tr(
                td(cls := "py-3 px-3",
                  a(cls := "fw-bold text-dark text-decoration-none", href := s"/panduan/${g.id}", g.name),
                  div(cls := "text-muted font-monospace", style := "font-size: 11px;", s"Revisi: #${rev.id.take(8)}")
                ),
                td(cls := "py-3 text-muted", g.cropVariety),
                td(cls := "py-3",
                  pubOpt match
                    case Some(pub) => span(cls := "badge-cetakan", pub.editionTag)
                    case None      => span(cls := "badge bg-light text-muted border", "Belum Diterbitkan")
                ),
                td(cls := "py-3",
                  if hasUnpublishedDraft then
                    span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle", "Ada Draf Baru")
                  else
                    span(cls := "badge bg-light text-muted border", "Sesuai Edisi Resmi")
                ),
                td(cls := "py-3 px-3 text-end")(
                  div(cls := "d-inline-flex gap-1")(
                    a(cls := "btn btn-sm btn-verdant-outline py-1 px-3", href := s"/panduan/${g.id}", "Ubah Draf"),
                    if hasUnpublishedDraft then
                      a(cls := "btn btn-sm btn-verdant py-1 px-3", href := s"/panduan/${g.id}/tinjau", "Tinjau")
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
  // 3. PEMERIKSAAN PERUBAHAN DRAF (DIFF TAKARAN RAMAH)
  // ==========================================================================
  def supervisorPemeriksaanPerubahanView(
    draftGuides: List[(Database#GuideRow, GuideRevision, Option[GuideSnapshot], ChangeSummary, Option[PublishedGuide])]
  ): Modifier =
    div(
      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Pemeriksaan Perubahan Cetakan (Diff)"),
        p(cls := "text-muted small mb-0", "Tinjau perbedaan rekomendasi takaran sebelum mengesahkan dan merilis edisi resmi baru untuk petani.")
      ),

      if draftGuides.isEmpty then
        div(cls := "p-5 text-center text-muted border rounded-4 bg-white shadow-sm")(
          i(cls := "bi bi-check2-all text-success fs-2 d-block mb-2"),
          h2(cls := "h6 fw-bold text-dark mb-1", "Seluruh Draf Telah Diterbitkan"),
          p(cls := "small mb-0", "Tidak ada draf cetakan yang sedang menunggu peninjauan atau persetujuan mandor.")
        )
      else
        div(cls := "row g-4")(
          draftGuides.map { case (g, currRev, baseSnapOpt, diffSummary, pubOpt) =>
            div(cls := "col-12")(
              div(cls := "panel-verdant border-warning-subtle shadow-sm")(
                div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 mb-3 pb-3 border-bottom")(
                  div(
                    div(cls := "d-flex align-items-center gap-2 mb-1")(
                      h2(cls := "h5 fw-bold text-dark mb-0", g.name),
                      span(cls := "badge bg-warning-subtle text-warning-emphasis border border-warning-subtle", "Draf Menunggu Pengesahan")
                    ),
                    div(cls := "small text-muted", s"Komoditas: ${g.cropVariety} · Penyusun: ${currRev.author} · Catatan: ${currRev.changeNotes}")
                  ),
                  div(cls := "d-flex gap-2")(
                    a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${g.id}", "Ubah Draf"),
                    a(cls := "btn btn-sm btn-verdant", href := s"/panduan/${g.id}/tinjau", "Tinjau & Putuskan →")
                  )
                ),

                h3(cls := "section-label mb-2 text-dark", "Perubahan Takaran & Parameter (Human-Readable Diff)"),
                if diffSummary.changes.isEmpty then
                  div(cls := "p-3 bg-light rounded-3 text-muted small mb-3", "Tidak ada perubahan parameter teknis dibandingkan versi sebelumnya.")
                else
                  div(cls := "border rounded-3 overflow-hidden bg-white mb-3")(
                    diffSummary.changes.map { c =>
                      div(cls := "p-3 border-bottom")(
                        div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                          span(cls := "fw-bold text-dark small", s"${c.stageName} ➔ ${c.paramName}"),
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
          li(cls := "breadcrumb-item")(a(href := "/kelola-panduan", cls := "text-decoration-none text-muted", "Kelola Cetakan")),
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
              span(cls := "badge-cetakan", "Edisi Resmi Sah")
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
          )("Edit Takaran Draf")
        )
      ),

      // Area Form Edit Draf Mandor
      div(cls := s"collapse ${if hasUnpublishedDraft then "show" else ""} mb-4", id := "areaMandorCollapse")(
        div(cls := "p-3 p-md-4 rounded-4 bg-white border shadow-sm")(
          h2(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-1", "Ubah Nilai Rekomendasi Takaran (Draf Mandor)"),
          p(cls := "small text-muted mb-3", "Perubahan akan disimpan sebagai revisi internal baru. Petani tidak akan terganggu sebelum Anda mengesahkan dan menerbitkannya secara resmi."),
          form(action := s"/panduan/${guide.id}/perbarui", method := "POST")(
            div(cls := "row g-3 mb-3")(
              div(cls := "col-md-6")(
                label(cls := "form-label small fw-semibold text-secondary", "Nama Mandor Penyusun"),
                input(`type` := "text", name := "author", cls := "form-control form-control-sm", placeholder := "Misal: Ir. Budi Santoso", required := true)
              ),
              div(cls := "col-md-6")(
                label(cls := "form-label small fw-semibold text-secondary", "Alasan / Catatan Perubahan"),
                input(`type` := "text", name := "changeNotes", cls := "form-control form-control-sm", placeholder := "Misal: Penyesuaian nutrisi untuk antisipasi musim hujan", required := true)
              )
            ),
            div(cls := "row g-3 mb-3")(
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-semibold text-secondary", "Pilih Tahap Budidaya"),
                select(name := "stageName", cls := "form-select form-select-sm")(
                  snap.stages.map { s => option(value := s.name, s.name) }
                )
              ),
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-semibold text-secondary", "Pilih Parameter"),
                select(name := "paramName", cls := "form-select form-select-sm")(
                  snap.stages.flatMap(_.parameters).map(_.name).distinct.map { pName =>
                    option(value := pName, pName)
                  }
                )
              ),
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-semibold text-secondary", "Nilai Target Rekomendasi Baru"),
                input(`type` := "number", step := "0.01", name := "newTargetValue", cls := "form-control form-control-sm", placeholder := "Contoh: 2.8", required := true)
              )
            ),
            div(cls := "text-end")(
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Simpan Draf & Periksa Diff")
            )
          )
        )
      ),

      // Tahapan Cetakan Saat Ini
      div(cls := "mb-4")(
        h2(cls := "section-label mb-3 text-dark", "Tahapan Budidaya & Parameter Cetakan Saat Ini"),
        div(cls := "border-top")(
          snap.stages.zipWithIndex.map { case (stage, idx) =>
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
      )
    )

  // ==========================================================================
  // 5. TINJAU PERUBAHAN & PUBLIKASIKAN ATAU TOLAK DRAF
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
          li(cls := "breadcrumb-item")(a(href := "/kelola-panduan", cls := "text-decoration-none text-muted", "Kelola Cetakan")),
          li(cls := "breadcrumb-item")(a(href := s"/panduan/${guide.id}", cls := "text-decoration-none text-muted", guide.name)),
          li(cls := "breadcrumb-item active text-dark", "Tinjauan Perubahan")
        )
      ),

      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Tinjauan Perubahan: Apa yang Berubah?"),
        p(cls := "text-muted small mb-0", s"Pemeriksaan perubahan draf untuk '${guide.name}'. Anda dapat mengesahkan edisi resmi baru, atau menolak draf ini.")
      ),

      // Human-readable Diff Box
      div(cls := "mb-4")(
        h2(cls := "section-label mb-3 text-dark", "Rangkuman Perubahan Takaran"),
        if diffSummary.changes.isEmpty then
          div(cls := "p-4 text-center text-muted border rounded-4 bg-white small mb-4 shadow-sm", "Tidak ada perbedaan parameter teknis.")
        else
          div(cls := "border rounded-4 overflow-hidden bg-white mb-4 shadow-sm")(
            diffSummary.changes.map { c =>
              div(cls := "p-3 border-bottom")(
                div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                  span(cls := "fw-bold text-dark", s"${c.stageName} ➔ ${c.paramName}"),
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
        // Kotak 1: Sahkan & Terbitkan
        div(cls := "col-md-7")(
          div(cls := "p-3 p-md-4 rounded-4 bg-white border border-success-subtle shadow-sm")(
            h2(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-1", "Sahkan & Terbitkan Edisi Resmi"),
            p(cls := "small text-muted mb-3", "Setelah disahkan, draf ini akan menjadi edisi resmi baru yang dapat dipilih petani saat memulai masa tanam."),
            form(action := s"/panduan/${guide.id}/terbitkan", method := "POST")(
              input(`type` := "hidden", name := "revisionId", value := currentRevision.id),
              div(cls := "row g-3 mb-3")(
                div(cls := "col-md-6")(
                  label(cls := "form-label small fw-semibold text-secondary", "Nama Edisi Resmi Baru"),
                  input(`type` := "text", name := "editionTag", cls := "form-control form-control-sm", placeholder := "Contoh: Edisi 2026.2 - Musim Hujan", required := true)
                ),
                div(cls := "col-md-6")(
                  label(cls := "form-label small fw-semibold text-secondary", "Nama Mandor Pengesah"),
                  input(`type` := "text", name := "approvedBy", cls := "form-control form-control-sm", placeholder := "Contoh: Ir. Budi Santoso", required := true)
                )
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Catatan Pengesahan"),
                textarea(name := "notes", cls := "form-control form-control-sm", rows := 2, placeholder := "Contoh: Telah diuji lab dan siap dipakai di lahan.")
              ),
              div(cls := "text-end")(
                button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Sahkan & Terbitkan Edisi Baru")
              )
            )
          )
        ),

        // Kotak 2: Tolak Draf
        div(cls := "col-md-5")(
          div(cls := "p-3 p-md-4 rounded-4 bg-white border border-danger-subtle shadow-sm")(
            h2(cls := "h6 fw-bold text-danger text-uppercase tracking-wide mb-1", "Tolak / Batalkan Draf"),
            p(cls := "small text-muted mb-3", "Batalkan draf perubahan ini dan kembalikan cetakan ke edisi resmi terakhir yang telah sah."),
            form(action := s"/panduan/${guide.id}/tolak", method := "POST")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Nama Mandor Penolak"),
                input(`type` := "text", name := "rejectedBy", cls := "form-control form-control-sm", placeholder := "Misal: Mandor Pemeriksa", required := true)
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Alasan Penolakan"),
                input(`type` := "text", name := "reason", cls := "form-control form-control-sm", placeholder := "Misal: Takaran nutrisi belum sesuai rekomendasi lab", required := true)
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
  // 6. RIWAYAT CETAKAN & SILSILAH REVISI (AUDIT TRAIL & ROLLBACK)
  // ==========================================================================
  def supervisorRiwayatView(
    revisions: List[GuideRevision],
    publishedEditions: List[(PublishedGuide, String)]
  ): Modifier =
    div(
      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Riwayat & Silsilah Cetakan Budidaya"),
        p(cls := "text-muted small mb-0", "Lacak seluruh jejak perubahan cetakan budidaya serta riwayat pemulihan (*rollback*) ke versi terdahulu.")
      ),

      div(cls := "row g-4")(
        // Kolom Kiri: Riwayat Edisi Cetakan Resmi
        div(cls := "col-md-5")(
          div(cls := "mb-4")(
            h2(cls := "section-label mb-3 text-dark", "Daftar Edisi Cetakan yang Pernah Dirilis"),
            if publishedEditions.isEmpty then
              div(cls := "p-4 text-center text-muted border rounded-4 bg-white small shadow-sm", "Belum ada edisi resmi yang dirilis.")
            else
              div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
                publishedEditions.map { case (pub, gName) =>
                  div(cls := "p-3 border-bottom")(
                    div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                      span(cls := "badge-cetakan", pub.editionTag),
                      span(cls := "small text-muted font-monospace", formatTs(pub.publishedAt))
                    ),
                    h3(cls := "h6 fw-bold text-dark mb-1", gName),
                    p(cls := "small text-secondary mb-1", pub.notes),
                    div(cls := "small text-muted", s"Disahkan oleh: ${pub.approvedBy}")
                  )
                }
              )
          )
        ),

        // Kolom Kanan: Log Revisi & Rollback
        div(cls := "col-md-7")(
          div(cls := "mb-4")(
            h2(cls := "section-label mb-3 text-dark", "Catatan Revisi & Pemulihan Versi (Rollback)"),
            if revisions.isEmpty then
              div(cls := "p-4 text-center text-muted border rounded-4 bg-white small shadow-sm", "Belum ada riwayat revisi.")
            else
              div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
                revisions.zipWithIndex.map { case (rev, idx) =>
                  div(cls := "p-3 border-bottom")(
                    div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                      span(cls := "fw-bold text-dark", rev.changeNotes),
                      span(cls := "small text-muted font-monospace", formatTs(rev.timestamp))
                    ),
                    div(cls := "small text-muted mb-2 font-monospace", s"Penyusun: ${rev.author} · Hash: #${rev.id.take(8)}"),
                    if idx == 0 then
                      span(cls := "badge bg-success-subtle text-success border border-success-subtle", "● Versi Draf Terkini")
                    else
                      form(action := s"/riwayat/pulihkan", method := "POST", cls := "d-inline")(
                        input(`type` := "hidden", name := "targetRevisionId", value := rev.id),
                        input(`type` := "hidden", name := "author", value := "Mandor Pemulih"),
                        button(
                          `type` := "submit",
                          cls := "btn btn-sm btn-outline-danger py-1 px-2 border-danger-subtle",
                          onclick := "return confirm('Apakah Anda yakin ingin memulihkan cetakan ke versi revisi ini?');"
                        )("Kembalikan ke Versi Ini")
                      )
                  )
                }
              )
          )
        )
      )
    )
