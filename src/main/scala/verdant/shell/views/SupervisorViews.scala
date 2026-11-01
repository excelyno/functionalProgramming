package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.nav
import verdant.core.*
import verdant.shell.Database

/**
 * Tampilan untuk mandor.
 *
 * Alur kerja: susun draf cetakan, periksa perubahan (diff), lalu sahkan jadi edisi resmi
 * atau tolak. Riwayat revisi bisa dipulihkan ke versi sebelumnya.
 */
trait SupervisorViews extends ViewHelpers with SharedModals:

  // --------------------------------------------------------------------------
  // Komponen kecil
  // --------------------------------------------------------------------------
  private def supHeader(title: String, subtitle: String, actions: Modifier*): Modifier =
    div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-md-end gap-3 pb-3 mb-4 border-bottom")(
      div(
        h1(cls := "h4 fw-semibold text-dark mb-1", title),
        p(cls := "text-muted small mb-0", subtitle)
      ),
      if actions.nonEmpty then div(cls := "d-flex gap-2 flex-wrap")(actions*) else modifier()
    )

  private def supEmpty(message: String): Modifier =
    div(cls := "border rounded-2 bg-white text-center text-muted small p-4")(message)

  private def supSectionTitle(text: String, aside: Modifier = modifier()): Modifier =
    div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
      h2(cls := "h6 fw-semibold text-dark mb-0", text),
      aside
    )

  private def newGuideButton(buttonCls: String): Modifier =
    button(
      `type` := "button",
      cls := buttonCls,
      data("bs-toggle") := "modal",
      data("bs-target") := "#tambahPanduanModal"
    )("Buat cetakan baru")

  // ==========================================================================
  // 1. BERANDA MANDOR
  // ==========================================================================
  def supervisorBerandaView(
    draftsAwaiting: List[(Database#GuideRow, GuideRevision, Option[PublishedGuide])],
    publishedGuides: List[(PublishedGuide, String)],
    recentRevisions: List[(GuideRevision, String)],
    currentUser: Option[User] = None
  ): Modifier =
    div(
      supHeader(
        "Beranda mandor",
        "Draf yang menunggu pengesahan dan cetakan yang sedang dipakai petani.",
        a(cls := "btn btn-verdant-outline", href := "/pemeriksaan-perubahan", "Periksa perubahan"),
        newGuideButton("btn btn-verdant")
      ),

      // Draf menunggu pengesahan
      div(cls := "mb-4")(
        supSectionTitle(
          "Draf menunggu pengesahan",
          span(cls := "small text-muted", s"${draftsAwaiting.length} draf")
        ),
        if draftsAwaiting.isEmpty then
          supEmpty("Semua cetakan sudah diterbitkan. Tidak ada draf yang menunggu.")
        else
          div(cls := "border rounded-2 bg-white")(
            draftsAwaiting.map { case (g, rev, _) =>
              div(cls := "px-3 py-3 border-bottom d-flex flex-column flex-md-row justify-content-between gap-3")(
                div(
                  a(cls := "fw-medium text-dark text-decoration-none", href := s"/panduan/${g.id}", g.name),
                  p(cls := "small text-secondary mb-1", rev.changeNotes),
                  div(cls := "small text-muted", s"${rev.author} · ${formatTs(rev.timestamp)}")
                ),
                div(cls := "d-flex align-items-start")(
                  a(cls := "btn btn-sm btn-verdant text-nowrap", href := s"/panduan/${g.id}/tinjau", "Tinjau dan sahkan")
                )
              )
            }
          )
      ),

      // Cetakan resmi
      div(cls := "mb-4")(
        supSectionTitle(
          "Cetakan yang dipakai petani",
          a(cls := "small text-success text-decoration-none", href := "/kelola-panduan", "Kelola semua")
        ),
        if publishedGuides.isEmpty then
          supEmpty("Belum ada cetakan yang disahkan.")
        else
          div(cls := "border rounded-2 bg-white")(
            publishedGuides.map { case (pub, gName) =>
              div(cls := "px-3 py-3 border-bottom d-flex justify-content-between align-items-center gap-3")(
                div(
                  a(cls := "fw-medium text-dark text-decoration-none d-block", href := s"/panduan/${pub.guideId}", gName),
                  span(cls := "small text-muted", s"${pub.editionTag} · disahkan ${pub.approvedBy}")
                ),
                a(cls := "btn btn-sm btn-verdant-outline text-nowrap", href := s"/panduan/${pub.guideId}", "Buka draf")
              )
            }
          ),
        p(cls := "small text-muted mt-3 mb-0",
          "Perubahan takaran tidak langsung berlaku di lahan petani. Petani tetap memakai edisi yang mereka kunci sampai edisi baru disahkan."
        )
      ),

      tambahPanduanModal(currentUser)
    )

  // ==========================================================================
  // 2. KELOLA SEMUA CETAKAN
  // ==========================================================================
  def supervisorKelolaPanduanView(
    guides: List[(Database#GuideRow, GuideRevision, Option[PublishedGuide])],
    currentUser: Option[User] = None
  ): Modifier =
    div(
      supHeader(
        "Kelola cetakan",
        "Semua cetakan, edisi resmi yang berlaku, dan status drafnya.",
        newGuideButton("btn btn-verdant")
      ),

      if guides.isEmpty then
        supEmpty("Belum ada cetakan.")
      else
        div(cls := "border rounded-2 bg-white overflow-hidden")(
          div(cls := "table-responsive")(
            table(cls := "table table-hover align-middle mb-0 small")(
              thead(cls := "table-light")(
                tr(
                  th(cls := "py-2 px-3 fw-medium", "Cetakan"),
                  th(cls := "py-2 fw-medium", "Komoditas"),
                  th(cls := "py-2 fw-medium", "Edisi resmi"),
                  th(cls := "py-2 fw-medium", "Draf"),
                  th(cls := "py-2 px-3 fw-medium text-end", "")
                )
              ),
              tbody(
                guides.map { case (g, rev, pubOpt) =>
                  val hasUnpublishedDraft = pubOpt.isEmpty || pubOpt.exists(_.revisionId != rev.id)
                  tr(
                    td(cls := "py-2 px-3",
                      a(cls := "fw-medium text-dark text-decoration-none", href := s"/panduan/${g.id}", g.name),
                      div(cls := "text-muted font-monospace", style := "font-size: 11px;", s"#${rev.id.take(8)}")
                    ),
                    td(cls := "py-2 text-muted", g.cropVariety),
                    td(cls := "py-2",
                      pubOpt match
                        case Some(pub) => span(pub.editionTag)
                        case None      => span(cls := "text-muted", "Belum terbit")
                    ),
                    td(cls := "py-2",
                      if hasUnpublishedDraft then span(cls := "text-warning-emphasis", "Ada draf baru")
                      else span(cls := "text-muted", "Sesuai edisi resmi")
                    ),
                    td(cls := "py-2 px-3 text-end")(
                      div(cls := "d-inline-flex gap-1")(
                        a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${g.id}", "Ubah draf"),
                        if hasUnpublishedDraft then
                          a(cls := "btn btn-sm btn-verdant", href := s"/panduan/${g.id}/tinjau", "Tinjau")
                        else modifier()
                      )
                    )
                  )
                }
              )
            )
          )
        ),

      tambahPanduanModal(currentUser)
    )

  // ==========================================================================
  // 3. PEMERIKSAAN PERUBAHAN (DIFF)
  // ==========================================================================
  def supervisorPemeriksaanPerubahanView(
    draftGuides: List[(Database#GuideRow, GuideRevision, Option[GuideSnapshot], ChangeSummary, Option[PublishedGuide])]
  ): Modifier =
    div(
      supHeader(
        "Periksa perubahan",
        "Bandingkan takaran draf dengan versi sebelumnya sebelum mengesahkan edisi baru."
      ),

      if draftGuides.isEmpty then
        supEmpty("Semua draf sudah diterbitkan. Tidak ada yang perlu diperiksa.")
      else
        div(cls := "d-flex flex-column gap-4")(
          draftGuides.map { case (g, currRev, _, diffSummary, _) =>
            div(cls := "border rounded-2 bg-white")(
              div(cls := "p-3 border-bottom d-flex flex-column flex-md-row justify-content-between gap-3")(
                div(
                  h2(cls := "h6 fw-semibold text-dark mb-1", g.name),
                  div(cls := "small text-muted", s"${g.cropVariety} · ${currRev.author} · ${currRev.changeNotes}")
                ),
                div(cls := "d-flex gap-2 align-items-start")(
                  a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${g.id}", "Ubah draf"),
                  a(cls := "btn btn-sm btn-verdant text-nowrap", href := s"/panduan/${g.id}/tinjau", "Tinjau dan putuskan")
                )
              ),
              if diffSummary.changes.isEmpty then
                div(cls := "p-3 small text-muted", "Tidak ada perubahan parameter dibanding versi sebelumnya.")
              else
                div(
                  diffSummary.changes.map { c =>
                    div(cls := "px-3 py-2 border-bottom")(
                      div(cls := "d-flex justify-content-between align-items-baseline")(
                        span(cls := "small fw-medium text-dark", s"${c.stageName} · ${c.paramName}"),
                        span(cls := "small text-muted", c.changeType)
                      ),
                      p(cls := "mb-0 small text-secondary", c.humanDescription)
                    )
                  }
                )
            )
          }
        )
    )

  // ==========================================================================
  // 4. DETAIL CETAKAN & EDITOR DRAF
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
          li(cls := "breadcrumb-item")(a(href := "/kelola-panduan", cls := "text-decoration-none text-muted", "Kelola cetakan")),
          li(cls := "breadcrumb-item active text-dark", guide.name)
        )
      ),

      supHeader(
        guide.name,
        s"${guide.cropVariety} · ${summary.totalStages} tahapan · ${summary.totalDurationDays} hari · " +
          (if hasUnpublishedDraft then "draf belum terbit" else "sesuai edisi resmi"),
        if hasUnpublishedDraft then
          a(cls := "btn btn-sm btn-verdant", href := s"/panduan/${guide.id}/tinjau", "Tinjau dan terbitkan")
        else modifier(),
        button(
          `type` := "button",
          cls := "btn btn-sm btn-verdant-outline",
          data("bs-toggle") := "collapse",
          data("bs-target") := "#areaMandorCollapse"
        )("Ubah takaran")
      ),

      // Form ubah draf
      div(cls := s"collapse ${if hasUnpublishedDraft then "show" else ""} mb-4", id := "areaMandorCollapse")(
        div(cls := "border rounded-2 bg-white p-3 p-md-4")(
          h2(cls := "h6 fw-semibold text-dark mb-1", "Ubah nilai takaran"),
          p(cls := "small text-muted mb-3",
            "Disimpan sebagai revisi baru. Petani tidak terpengaruh sebelum edisi disahkan dan diterbitkan."
          ),
          form(action := s"/panduan/${guide.id}/perbarui", method := "POST")(
            div(cls := "row g-3 mb-3")(
              div(cls := "col-md-6")(
                label(cls := "form-label small fw-medium mb-1", `for` := "upd-author", "Penyusun"),
                input(`type` := "text", id := "upd-author", name := "author", cls := "form-control form-control-sm", required := true)
              ),
              div(cls := "col-md-6")(
                label(cls := "form-label small fw-medium mb-1", `for` := "upd-notes", "Alasan perubahan"),
                input(`type` := "text", id := "upd-notes", name := "changeNotes", cls := "form-control form-control-sm",
                  placeholder := "mis. penyesuaian nutrisi musim hujan", required := true)
              )
            ),
            div(cls := "row g-3 mb-3")(
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-medium mb-1", `for` := "upd-stage", "Tahap"),
                select(name := "stageName", id := "upd-stage", cls := "form-select form-select-sm")(
                  snap.stages.map { s => option(value := s.name, s.name) }
                )
              ),
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-medium mb-1", `for` := "upd-param", "Parameter"),
                select(name := "paramName", id := "upd-param", cls := "form-select form-select-sm")(
                  snap.stages.flatMap(_.parameters).map(_.name).distinct.map { pName =>
                    option(value := pName, pName)
                  }
                )
              ),
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-medium mb-1", `for` := "upd-value", "Target baru"),
                input(`type` := "number", step := "0.01", id := "upd-value", name := "newTargetValue",
                  cls := "form-control form-control-sm", placeholder := "mis. 2.8", required := true)
              )
            ),
            div(cls := "text-end")(
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Simpan draf dan periksa perubahan")
            )
          )
        )
      ),

      // Tahapan saat ini
      div(cls := "mb-4")(
        h2(cls := "h6 fw-semibold text-dark mb-2", "Tahapan dan parameter saat ini"),
        div(cls := "border-top")(
          snap.stages.zipWithIndex.map { case (stage, idx) =>
            div(cls := "py-4 border-bottom")(
              div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                h3(cls := "h6 fw-semibold text-dark mb-0", s"${idx + 1}. ${stage.name}"),
                span(cls := "small text-muted", s"${stage.durationDays} hari")
              ),
              p(cls := "small text-secondary mb-3", stage.instructions),
              div(cls := "border rounded-2 overflow-hidden bg-white")(
                table(cls := "table table-sm mb-0 small")(
                  thead(cls := "table-light")(
                    tr(
                      th(cls := "py-2 px-3 fw-medium", "Parameter"),
                      th(cls := "py-2 fw-medium", "Target"),
                      th(cls := "py-2 px-3 fw-medium text-end", "Rentang aman")
                    )
                  ),
                  tbody(
                    stage.parameters.map { p =>
                      tr(
                        td(cls := "py-2 px-3 text-dark", p.name),
                        td(cls := "py-2", s"${p.targetValue} ${p.unit}"),
                        td(cls := "py-2 px-3 text-end text-muted", s"${p.minValue} – ${p.maxValue} ${p.unit}")
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
  // 5. TINJAU PERUBAHAN: SAHKAN ATAU TOLAK
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
          li(cls := "breadcrumb-item")(a(href := "/kelola-panduan", cls := "text-decoration-none text-muted", "Kelola cetakan")),
          li(cls := "breadcrumb-item")(a(href := s"/panduan/${guide.id}", cls := "text-decoration-none text-muted", guide.name)),
          li(cls := "breadcrumb-item active text-dark", "Tinjau perubahan")
        )
      ),

      supHeader(
        "Tinjau perubahan",
        s"Draf '${guide.name}'. Sahkan sebagai edisi resmi baru, atau tolak."
      ),

      // Ringkasan diff
      div(cls := "mb-4")(
        h2(cls := "h6 fw-semibold text-dark mb-2", "Yang berubah"),
        if diffSummary.changes.isEmpty then
          supEmpty("Tidak ada perbedaan parameter.")
        else
          div(cls := "border rounded-2 bg-white")(
            diffSummary.changes.map { c =>
              div(cls := "px-3 py-3 border-bottom")(
                div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                  span(cls := "fw-medium text-dark", s"${c.stageName} · ${c.paramName}"),
                  span(cls := "small text-muted", c.changeType)
                ),
                p(cls := "mb-1 small text-secondary", c.humanDescription),
                div(cls := "small text-muted font-monospace",
                  s"${c.oldValue.map(_.toString).getOrElse("-")} → ${c.newValue.map(_.toString).getOrElse("-")} ${c.unit}"
                )
              )
            }
          )
      ),

      div(cls := "row g-4")(
        // Sahkan
        div(cls := "col-md-7")(
          div(cls := "border rounded-2 bg-white p-3 p-md-4 h-100")(
            h2(cls := "h6 fw-semibold text-dark mb-1", "Sahkan dan terbitkan"),
            p(cls := "small text-muted mb-3",
              "Draf ini menjadi edisi resmi baru dan bisa dipilih petani saat memulai masa tanam."
            ),
            form(action := s"/panduan/${guide.id}/terbitkan", method := "POST")(
              input(`type` := "hidden", name := "revisionId", value := currentRevision.id),
              div(cls := "row g-3 mb-3")(
                div(cls := "col-md-6")(
                  label(cls := "form-label small fw-medium mb-1", `for` := "pub-edition", "Nama edisi"),
                  input(`type` := "text", id := "pub-edition", name := "editionTag", cls := "form-control form-control-sm",
                    placeholder := "mis. Edisi 2026.2 (musim hujan)", required := true)
                ),
                div(cls := "col-md-6")(
                  label(cls := "form-label small fw-medium mb-1", `for` := "pub-by", "Pengesah"),
                  input(`type` := "text", id := "pub-by", name := "approvedBy", cls := "form-control form-control-sm", required := true)
                )
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium mb-1", `for` := "pub-notes", "Catatan pengesahan"),
                textarea(name := "notes", id := "pub-notes", cls := "form-control form-control-sm", rows := 2,
                  placeholder := "mis. sudah diuji dan siap dipakai di lahan")
              ),
              div(cls := "text-end")(
                button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Sahkan dan terbitkan")
              )
            )
          )
        ),

        // Tolak
        div(cls := "col-md-5")(
          div(cls := "border rounded-2 bg-white p-3 p-md-4 h-100")(
            h2(cls := "h6 fw-semibold text-dark mb-1", "Tolak draf"),
            p(cls := "small text-muted mb-3",
              "Batalkan perubahan ini dan kembali ke edisi resmi terakhir."
            ),
            form(action := s"/panduan/${guide.id}/tolak", method := "POST")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium mb-1", `for` := "rej-by", "Penolak"),
                input(`type` := "text", id := "rej-by", name := "rejectedBy", cls := "form-control form-control-sm", required := true)
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium mb-1", `for` := "rej-reason", "Alasan"),
                input(`type` := "text", id := "rej-reason", name := "reason", cls := "form-control form-control-sm",
                  placeholder := "mis. takaran belum sesuai rekomendasi lab", required := true)
              ),
              div(cls := "text-end")(
                button(`type` := "submit", cls := "btn btn-sm btn-outline-danger", "Tolak draf")
              )
            )
          )
        )
      )
    )

  // ==========================================================================
  // 6. RIWAYAT & PEMULIHAN REVISI
  // ==========================================================================
  def supervisorRiwayatView(
    revisions: List[GuideRevision],
    publishedEditions: List[(PublishedGuide, String)]
  ): Modifier =
    div(
      supHeader(
        "Riwayat",
        "Edisi yang pernah dirilis dan seluruh revisi, termasuk pemulihan ke versi lama."
      ),

      div(cls := "row g-4")(
        // Edisi resmi
        div(cls := "col-md-5")(
          h2(cls := "h6 fw-semibold text-dark mb-2", "Edisi yang pernah dirilis"),
          if publishedEditions.isEmpty then
            supEmpty("Belum ada edisi resmi.")
          else
            div(cls := "border rounded-2 bg-white")(
              publishedEditions.map { case (pub, gName) =>
                div(cls := "px-3 py-3 border-bottom")(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                    span(cls := "fw-medium text-dark", gName),
                    span(cls := "small text-muted font-monospace", formatTs(pub.publishedAt))
                  ),
                  div(cls := "small text-secondary", pub.editionTag),
                  p(cls := "small text-secondary mb-1", pub.notes),
                  div(cls := "small text-muted", s"Disahkan ${pub.approvedBy}")
                )
              }
            )
        ),

        // Revisi
        div(cls := "col-md-7")(
          h2(cls := "h6 fw-semibold text-dark mb-2", "Revisi dan pemulihan"),
          if revisions.isEmpty then
            supEmpty("Belum ada riwayat revisi.")
          else
            div(cls := "border rounded-2 bg-white")(
              revisions.zipWithIndex.map { case (rev, idx) =>
                div(cls := "px-3 py-3 border-bottom")(
                  div(cls := "d-flex justify-content-between align-items-baseline gap-3 mb-1")(
                    span(cls := "fw-medium text-dark", rev.changeNotes),
                    span(cls := "small text-muted font-monospace text-nowrap", formatTs(rev.timestamp))
                  ),
                  div(cls := "small text-muted font-monospace mb-2", s"${rev.author} · #${rev.id.take(8)}"),
                  if idx == 0 then
                    span(cls := "small text-success", "Versi draf terkini")
                  else
                    form(action := "/riwayat/pulihkan", method := "POST", cls := "d-inline")(
                      input(`type` := "hidden", name := "targetRevisionId", value := rev.id),
                      input(`type` := "hidden", name := "author", value := "Mandor Pemulih"),
                      button(
                        `type` := "submit",
                        cls := "btn btn-sm btn-outline-danger py-0 px-2",
                        onclick := "return confirm('Pulihkan cetakan ke versi revisi ini?');"
                      )("Pulihkan versi ini")
                    )
                )
              }
            )
        )
      )
    )