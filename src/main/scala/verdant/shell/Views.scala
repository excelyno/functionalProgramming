package verdant.shell

import scalatags.Text.all.*
import scalatags.Text.tags2.{nav, title, style as cssStyle}
import verdant.core.*
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneId}

object Views:

  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")
    .withZone(ZoneId.systemDefault())

  private def formatTs(ts: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(ts))

  private val todayStr = LocalDate.now().toString

  // ==========================================================================
  // MASTER LAYOUT (MINIMAL, CALM, EDITORIAL, PRACTICAL)
  // ==========================================================================

  def layout(pageTitle: String, activeNav: String, alert: Option[(String, String)] = None)(pageContent: Modifier*): String =
    "<!DOCTYPE html>" + html(lang := "id")(
      head(
        meta(charset := "utf-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"),
        title(s"$pageTitle · Verdant Pertanian"),
        link(
          rel := "stylesheet",
          href := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
        ),
        link(
          rel := "stylesheet",
          href := "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"
        ),
        cssStyle(
          """
          :root {
            --verdant-primary: #1b4332;
            --verdant-primary-hover: #143326;
            --verdant-canvas: #ffffff;
            --verdant-canvas-subtle: #fcfbf9;
            --verdant-surface: #ffffff;
            --verdant-border: #e4e4e7;
            --verdant-border-subtle: #f4f4f5;
            --verdant-text: #18181b;
            --verdant-text-muted: #52525b;
            --verdant-text-subtle: #71717a;
          }
          body {
            background-color: var(--verdant-canvas);
            color: var(--verdant-text);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            font-size: 15px;
            line-height: 1.5;
            padding-bottom: 72px;
          }
          /* Navigation */
          .navbar-verdant {
            background-color: var(--verdant-primary);
            border-bottom: 1px solid rgba(0, 0, 0, 0.1);
          }
          .nav-link-verdant {
            color: rgba(255, 255, 255, 0.8) !important;
            font-size: 14px;
            font-weight: 500;
            padding: 6px 12px !important;
            border-radius: 4px;
          }
          .nav-link-verdant.active, .nav-link-verdant:hover {
            color: #ffffff !important;
            background-color: rgba(255, 255, 255, 0.12);
          }
          /* Buttons */
          .btn-verdant {
            background-color: var(--verdant-primary);
            color: #ffffff;
            font-size: 14px;
            font-weight: 500;
            border: 1px solid var(--verdant-primary);
            border-radius: 6px;
            padding: 7px 16px;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            justify-content: center;
          }
          .btn-verdant:hover {
            background-color: var(--verdant-primary-hover);
            color: #ffffff;
          }
          .btn-verdant-outline {
            background-color: transparent;
            color: var(--verdant-text);
            font-size: 14px;
            font-weight: 500;
            border: 1px solid var(--verdant-border);
            border-radius: 6px;
            padding: 7px 16px;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            justify-content: center;
          }
          .btn-verdant-outline:hover {
            background-color: var(--verdant-border-subtle);
            border-color: #d4d4d8;
            color: var(--verdant-text);
          }
          /* Flat cards & panels */
          .panel-verdant {
            background: #ffffff;
            border: 1px solid var(--verdant-border);
            border-radius: 8px;
            padding: 20px;
          }
          .section-label {
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--verdant-text-subtle);
          }
          /* Bottom navigation bar for mobile */
          .bottom-nav {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: #ffffff;
            border-top: 1px solid var(--verdant-border);
            z-index: 1030;
            display: flex;
            justify-content: space-around;
            padding: 8px 0;
          }
          .bottom-nav a {
            text-align: center;
            color: var(--verdant-text-subtle);
            text-decoration: none;
            font-size: 11px;
            flex-grow: 1;
          }
          .bottom-nav a.active {
            color: var(--verdant-primary);
            font-weight: 600;
          }
          .bottom-nav i {
            font-size: 18px;
            display: block;
            margin-bottom: 2px;
          }
          """
        )
      ),
      body(
        // Desktop / Tablet Navigation
        nav(cls := "navbar navbar-expand-lg navbar-dark navbar-verdant px-3 py-2")(
          div(cls := "container")(
            a(cls := "navbar-brand d-flex align-items-center gap-2 text-decoration-none", href := "/")(
              span(cls := "fw-bold fs-5 text-white tracking-tight", "Verdant"),
              span(cls := "badge bg-white text-dark small fw-normal ms-1", "Pertanian")
            ),
            button(
              cls := "navbar-toggler border-0",
              `type` := "button",
              data("bs-toggle") := "collapse",
              data("bs-target") := "#navbarVerdantNav"
            )(span(cls := "navbar-toggler-icon")),
            div(cls := "collapse navbar-collapse", id := "navbarVerdantNav")(
              ul(cls := "navbar-nav me-auto mb-2 mb-lg-0 ms-lg-3")(
                li(cls := "nav-item")(
                  a(cls := s"nav-link nav-link-verdant ${if activeNav == "beranda" then "active" else ""}", href := "/")("Beranda")
                ),
                li(cls := "nav-item")(
                  a(cls := s"nav-link nav-link-verdant ${if activeNav == "catatan" then "active" else ""}", href := "/catatan")("Catatan Budidaya")
                ),
                li(cls := "nav-item")(
                  a(cls := s"nav-link nav-link-verdant ${if activeNav == "panduan" then "active" else ""}", href := "/panduan")("Panduan Budidaya")
                ),
                li(cls := "nav-item")(
                  a(cls := s"nav-link nav-link-verdant ${if activeNav == "masa-tanam" then "active" else ""}", href := "/masa-tanam")("Masa Tanam")
                ),
                li(cls := "nav-item")(
                  a(cls := s"nav-link nav-link-verdant ${if activeNav == "riwayat" then "active" else ""}", href := "/riwayat")("Riwayat")
                )
              ),
              span(cls := "navbar-text small text-white-50 d-none d-lg-inline")(
                "Catat kegiatan budidaya, ikuti panduan, dan lacak riwayat pertanian dengan mudah."
              )
            )
          )
        ),

        // Main Container
        div(cls := "container py-3 py-md-4")(
          // Subtle Alert banner (Clean & Neutral)
          alert match
            case Some((kind, msg)) =>
              val borderClass = if kind == "success" then "border-success text-dark bg-light" else "border-danger text-dark bg-light"
              div(cls := s"alert $borderClass alert-dismissible fade show mb-3 border py-2 px-3 small", role := "alert")(
                div(cls := "d-flex align-items-center justify-content-between")(
                  div(raw(msg.replace("\n", "<br/>"))),
                  button(`type` := "button", cls := "btn-close small p-2", data("bs-dismiss") := "alert")
                )
              )
            case None => modifier()
          ,
          pageContent
        ),

        // Mobile Bottom Navigation Bar
        div(cls := "bottom-nav d-lg-none")(
          a(href := "/", cls := (if activeNav == "beranda" then "active" else ""))(
            i(cls := "bi bi-house"),
            span("Beranda")
          ),
          a(href := "/catatan", cls := (if activeNav == "catatan" then "active" else ""))(
            i(cls := "bi bi-journal-text"),
            span("Catatan")
          ),
          a(href := "/panduan", cls := (if activeNav == "panduan" then "active" else ""))(
            i(cls := "bi bi-book"),
            span("Panduan")
          ),
          a(href := "/masa-tanam", cls := (if activeNav == "masa-tanam" then "active" else ""))(
            i(cls := "bi bi-calendar3"),
            span("Masa Tanam")
          ),
          a(href := "/riwayat", cls := (if activeNav == "riwayat" then "active" else ""))(
            i(cls := "bi bi-clock-history"),
            span("Riwayat")
          )
        ),

        script(src := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js")
      )
    ).render

  // ==========================================================================
  // 1. BERANDA (CALM, EDITORIAL, PRACTICAL DASHBOARD)
  // ==========================================================================

  def berandaView(
    periods: List[(CultivationPeriod, String)],
    recentActivities: List[CultivationActivity],
    guides: List[Database#GuideRow]
  ): Modifier =
    div(
      // Editorial Header
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Beranda"),
          p(cls := "text-muted small mb-0", "Catat kegiatan budidaya, ikuti panduan, dan lacak riwayat pertanian dengan mudah.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#catatKegiatanModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Catat Kegiatan")
      ),

      // Masa Tanam Aktif Section
      div(cls := "mb-4")(
        div(cls := "d-flex justify-content-between align-items-center mb-3")(
          h2(cls := "section-label mb-0", "Masa Tanam Aktif di Lahan"),
          a(cls := "small text-muted text-decoration-none", href := "/masa-tanam", "Semua Masa Tanam →")
        ),
        if periods.isEmpty then
          div(cls := "p-4 text-center text-muted border rounded-3 bg-white")(
            p(cls := "mb-2 small", "Belum ada masa tanam yang aktif saat ini."),
            a(cls := "btn btn-sm btn-verdant", href := "/masa-tanam", "Buka Masa Tanam Baru")
          )
        else
          div(cls := "row g-3")(
            periods.map { case (period, guideName) =>
              div(cls := "col-md-6")(
                div(cls := "panel-verdant h-100 d-flex flex-column justify-content-between")(
                  div(
                    div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                      span(cls := "small text-muted fw-medium", period.code),
                      span(cls := "badge bg-light text-dark border", period.status)
                    ),
                    h3(cls := "h5 fw-bold text-dark mb-1", period.cropVariety),
                    p(cls := "small text-muted mb-3", s"${period.plotName} · Panduan: ${period.publishedEdition}")
                  ),
                  div(cls := "pt-2 border-top d-flex gap-2")(
                    a(cls := "btn btn-sm btn-verdant-outline flex-grow-1", href := s"/masa-tanam/${period.code}", "Buka Garis Waktu"),
                    a(cls := "btn btn-sm btn-verdant-outline flex-grow-1", href := s"/panduan/${period.guideId}", "Panduan Budidaya")
                  )
                )
              )
            }
          )
      ),

      // Two Column: Recent Activities & Farming Guidelines
      div(cls := "row g-4 pt-2")(
        // Recent Activities
        div(cls := "col-lg-7")(
          div(cls := "d-flex justify-content-between align-items-center mb-3")(
            h2(cls := "section-label mb-0", "Catatan Kegiatan Terbaru Petani"),
            a(cls := "small text-muted text-decoration-none", href := "/catatan", "Semua Catatan →")
          ),
          if recentActivities.isEmpty then
            div(cls := "p-4 text-center text-muted border rounded-3 bg-white small", "Belum ada catatan kegiatan yang dimasukkan.")
          else
            div(cls := "border-top")(
              recentActivities.take(5).map { act =>
                div(cls := "py-3 border-bottom")(
                  div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                    div(
                      span(cls := "fw-semibold text-dark me-2", act.activityType),
                      act.quantity.map(q => span(cls := "small text-muted", s"(${q})")).getOrElse(modifier())
                    ),
                    span(cls := "small text-muted", act.activityDate)
                  ),
                  p(cls := "mb-1 text-secondary small", act.notes),
                  div(cls := "small text-muted", s"Lahan: ${act.periodCode} · Pelaksana: ${act.farmerName}")
                )
              }
            )
        ),

        // Side Section: Guidelines & Practical Notes
        div(cls := "col-lg-5")(
          div(cls := "mb-4")(
            h2(cls := "section-label mb-3", "Panduan Budidaya Tersedia"),
            div(cls := "border-top")(
              guides.map { g =>
                div(cls := "py-2 border-bottom d-flex justify-content-between align-items-center")(
                  div(
                    a(cls := "fw-medium text-dark text-decoration-none small", href := s"/panduan/${g.id}", g.name),
                    div(cls := "small text-muted", g.cropVariety)
                  ),
                  a(cls := "btn btn-sm btn-verdant-outline py-1 px-2 text-nowrap", href := s"/panduan/${g.id}", "Buka")
                )
              }
            )
          ),

          div(cls := "p-3 rounded-3 bg-light border")(
            h3(cls := "small fw-bold text-dark text-uppercase tracking-wide mb-2", "Catatan Agronomi"),
            p(cls := "small text-muted mb-0", "Lakukan penyiraman pagi sebelum pukul 09.00 untuk mengoptimalkan penyerapan stomata dan mencegah genangan air berlebih di malam hari.")
          )
        )
      ),

      catatKegiatanModal(periods)
    )

  // ==========================================================================
  // MODAL: CATAT KEGIATAN (CLEAN, FOCUSED FORM)
  // ==========================================================================

  def catatKegiatanModal(periods: List[(CultivationPeriod, String)]): Modifier =
    div(cls := "modal fade", id := "catatKegiatanModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border rounded-3")(
          form(action := "/catatan/tambah", method := "POST")(
            div(cls := "modal-header border-bottom py-3")(
              h4(cls := "modal-title fs-6 fw-bold text-dark", "Catat Kegiatan Budidaya"),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Masa Tanam / Lahan"),
                select(name := "periodCode", cls := "form-select form-select-sm", required := true)(
                  if periods.isEmpty then
                    option(value := "", "Belum ada masa tanam aktif")
                  else
                    periods.map { case (p, _) =>
                      option(value := p.code, s"${p.code} — ${p.cropVariety} (${p.plotName})")
                    }
                )
              ),
              div(cls := "row g-2 mb-3")(
                div(cls := "col-7")(
                  label(cls := "form-label small fw-medium text-muted", "Jenis Kegiatan"),
                  select(name := "activityType", cls := "form-select form-select-sm", required := true)(
                    option(value := "Penyiraman", "Penyiraman"),
                    option(value := "Pemupukan", "Pemupukan"),
                    option(value := "Pengendalian Hama", "Pengendalian Hama"),
                    option(value := "Pemangkasan", "Pemangkasan"),
                    option(value := "Penyiangan", "Penyiangan Gulma"),
                    option(value := "Panen", "Panen"),
                    option(value := "Perawatan Lain", "Perawatan Lain")
                  )
                ),
                div(cls := "col-5")(
                  label(cls := "form-label small fw-medium text-muted", "Tanggal"),
                  input(`type` := "date", name := "activityDate", cls := "form-control form-control-sm", value := todayStr, required := true)
                )
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Jumlah / Takaran (Opsional)"),
                input(`type` := "text", name := "quantity", cls := "form-control form-control-sm", placeholder := "Misal: 30 kg NPK, 200 Liter, 150 kg Panen")
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Catatan Keterangan"),
                textarea(name := "notes", cls := "form-control form-control-sm", rows := 3, placeholder := "Misal: Penyiraman pagi lancar, tanaman tampak sehat dan segar.", required := true)
              ),
              div(cls := "mb-2")(
                label(cls := "form-label small fw-medium text-muted", "Nama Petani / Pelaksana"),
                input(`type` := "text", name := "farmerName", cls := "form-control form-control-sm", placeholder := "Misal: Pak Joko", required := true)
              )
            ),
            div(cls := "modal-footer border-top py-2")(
              button(`type` := "button", cls := "btn btn-sm btn-light border", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Simpan Kegiatan")
            )
          )
        )
      )
    )

  // ==========================================================================
  // 2. CATATAN BUDIDAYA (ACTIVITY JOURNAL - CLEAN TABLE/LIST)
  // ==========================================================================

  def catatanView(
    activities: List[CultivationActivity],
    periods: List[(CultivationPeriod, String)],
    selectedPeriodCode: Option[String] = None
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Catatan Budidaya"),
          p(cls := "text-muted small mb-0", "Jurnal harian seluruh kegiatan petani di petak lahan.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#catatKegiatanModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Catat Kegiatan")
      ),

      // Filter Bar
      div(cls := "p-3 mb-4 bg-light border rounded-3")(
        form(action := "/catatan", method := "GET", cls := "row g-2 align-items-center")(
          div(cls := "col-auto")(
            label(cls := "small fw-medium text-muted", "Saring Masa Tanam:")
          ),
          div(cls := "col-auto")(
            select(name := "periodCode", cls := "form-select form-select-sm")(
              option(value := "", "Semua Masa Tanam"),
              periods.map { case (p, _) =>
                option(
                  value := p.code,
                  if selectedPeriodCode.contains(p.code) then selected := true else modifier(),
                  s"${p.code} — ${p.cropVariety} (${p.plotName})"
                )
              }
            )
          ),
          div(cls := "col-auto")(
            button(`type` := "submit", cls := "btn btn-sm btn-verdant-outline", "Terapkan")
          ),
          div(cls := "col text-end small text-muted")(
            s"Total catatan: ${activities.length}"
          )
        )
      ),

      // Clean Table
      if activities.isEmpty then
        div(cls := "p-5 text-center text-muted border rounded-3 bg-white small")(
          p(cls := "mb-2", "Belum ada catatan kegiatan yang dimasukkan."),
          button(
            cls := "btn btn-sm btn-verdant",
            data("bs-toggle") := "modal",
            data("bs-target") := "#catatKegiatanModal"
          )("Catat Kegiatan Sekarang")
        )
      else
        div(cls := "border rounded-3 overflow-hidden bg-white")(
          div(cls := "table-responsive")(
            table(cls := "table table-hover align-middle mb-0 small")(
              thead(cls := "table-light text-muted")(
                tr(
                  th(cls := "fw-medium py-2 px-3", "Tanggal"),
                  th(cls := "fw-medium py-2", "Masa Tanam"),
                  th(cls := "fw-medium py-2", "Jenis Kegiatan"),
                  th(cls := "fw-medium py-2", "Catatan & Keterangan"),
                  th(cls := "fw-medium py-2", "Takaran / Volume"),
                  th(cls := "fw-medium py-2 px-3", "Pelaksana")
                )
              ),
              tbody(
                activities.map { act =>
                  tr(
                    td(cls := "py-2 px-3 text-nowrap", act.activityDate),
                    td(
                      a(cls := "text-decoration-none text-dark fw-medium", href := s"/masa-tanam/${act.periodCode}", act.periodCode)
                    ),
                    td(span(cls := "fw-semibold text-dark", act.activityType)),
                    td(span(cls := "text-secondary", act.notes)),
                    td(act.quantity.map(q => span(cls := "text-muted", q)).getOrElse(span(cls := "text-muted", "—"))),
                    td(cls := "px-3 text-muted", act.farmerName)
                  )
                }
              )
            )
          )
        )
      ,

      catatKegiatanModal(periods)
    )

  // ==========================================================================
  // 3. PANDUAN BUDIDAYA (LIST OF CROP GUIDES)
  // ==========================================================================

  def panduanListView(
    guides: List[Database#GuideRow],
    publishedEditions: Map[String, PublishedGuide]
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Panduan Budidaya"),
          p(cls := "text-muted small mb-0", "Panduan teknis dan takaran anjuran budidaya per komoditas tanaman.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#tambahPanduanModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Tambah Panduan Komoditas")
      ),

      div(cls := "row g-3")(
        guides.map { g =>
          val pubOpt = publishedEditions.get(g.id)
          div(cls := "col-md-6")(
            div(cls := "panel-verdant h-100 d-flex flex-column justify-content-between")(
              div(
                div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                  span(cls := "small text-muted", g.cropVariety),
                  pubOpt match
                    case Some(pub) => span(cls := "badge bg-light text-dark border", pub.editionTag)
                    case None      => span(cls := "badge bg-light text-muted border", "Draf Internal")
                ),
                h2(cls := "h5 fw-bold text-dark mb-1", g.name),
                p(cls := "small text-muted mb-3", s"Komoditas: ${g.cropVariety}")
              ),
              div(cls := "pt-3 border-top d-flex justify-content-between align-items-center")(
                span(cls := "small text-muted", formatTs(g.createdAt)),
                a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${g.id}", "Buka Panduan →")
              )
            )
          )
        }
      ),

      // Modal Tambah Panduan
      div(cls := "modal fade", id := "tambahPanduanModal", tabindex := "-1")(
        div(cls := "modal-dialog modal-dialog-centered")(
          div(cls := "modal-content border rounded-3")(
            form(action := "/panduan/tambah", method := "POST")(
              div(cls := "modal-header border-bottom py-3")(
                h5(cls := "modal-title fs-6 fw-bold text-dark", "Tambah Panduan Budidaya Baru"),
                button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
              ),
              div(cls := "modal-body py-3")(
                div(cls := "mb-3")(
                  label(cls := "form-label small fw-medium text-muted", "Kode Singkat / ID Panduan"),
                  input(`type` := "text", name := "id", cls := "form-control form-control-sm", placeholder := "Contoh: panduan-jagung-manis", required := true)
                ),
                div(cls := "mb-3")(
                  label(cls := "form-label small fw-medium text-muted", "Nama Lengkap Panduan"),
                  input(`type` := "text", name := "name", cls := "form-control form-control-sm", placeholder := "Contoh: SOP Budidaya Jagung Manis Hibrida", required := true)
                ),
                div(cls := "mb-3")(
                  label(cls := "form-label small fw-medium text-muted", "Komoditas & Varietas"),
                  input(`type` := "text", name := "cropVariety", cls := "form-control form-control-sm", placeholder := "Contoh: Jagung Manis Talenta", required := true)
                ),
                div(cls := "mb-3")(
                  label(cls := "form-label small fw-medium text-muted", "Penyusun / Mandor Pengusul"),
                  input(`type` := "text", name := "author", cls := "form-control form-control-sm", placeholder := "Contoh: Ir. Budi Santoso", required := true)
                )
              ),
              div(cls := "modal-footer border-top py-2")(
                button(`type` := "button", cls := "btn btn-sm btn-light border", data("bs-dismiss") := "modal", "Batal"),
                button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Buat Panduan Awal")
              )
            )
          )
        )
      )
    )

  // ==========================================================================
  // 4. PANDUAN DETAIL (INSTRUCTIONS, PARAMETERS & SUPERVISOR SECTION)
  // ==========================================================================

  def panduanDetailView(
    guide: Database#GuideRow,
    latestRevision: GuideRevision,
    publishedGuideOpt: Option[PublishedGuide],
    summary: GuideSummary
  ): Modifier =
    val snap = latestRevision.snapshot
    div(
      nav(aria.label := "breadcrumb", cls := "mb-3")(
        ol(cls := "breadcrumb small text-muted mb-0")(
          li(cls := "breadcrumb-item")(a(href := "/panduan", cls := "text-decoration-none text-muted", "Panduan Budidaya")),
          li(cls := "breadcrumb-item active text-dark", guide.name)
        )
      ),

      // Compact Header
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            h1(cls := "h4 fw-bold text-dark mb-0", guide.name),
            publishedGuideOpt match
              case Some(pub) => span(cls := "badge bg-light text-dark border", pub.editionTag)
              case None      => span(cls := "badge bg-light text-muted border", "Draf Internal")
          ),
          div(cls := "small text-muted", s"Komoditas: ${guide.cropVariety} · ${summary.totalStages} Tahapan · Total Durasi: ${summary.totalDurationDays} Hari")
        ),
        div(cls := "d-flex gap-2")(
          a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${guide.id}/tinjau", "Apa yang Berubah?"),
          button(
            cls := "btn btn-sm btn-verdant",
            data("bs-toggle") := "collapse",
            data("bs-target") := "#areaMandorCollapse"
          )("Ubah Panduan (Mandor)")
        )
      ),

      // Supervisor Edit Area (Collapsible, Understated)
      div(cls := "collapse mb-4", id := "areaMandorCollapse")(
        div(cls := "p-3 p-md-4 rounded-3 bg-light border")(
          h2(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-1", "Area Mandor & Penyuluh: Perbarui Draf Panduan"),
          p(cls := "small text-muted mb-3", "Perubahan akan disimpan sebagai draf revisi internal. Anda dapat meninjau ringkasan perubahan sebelum menerbitkannya secara resmi ke petani."),
          form(action := s"/panduan/${guide.id}/perbarui", method := "POST")(
            div(cls := "row g-3 mb-3")(
              div(cls := "col-md-6")(
                label(cls := "form-label small fw-medium text-muted", "Nama Mandor / Penyusun Perubahan"),
                input(`type` := "text", name := "author", cls := "form-control form-control-sm", placeholder := "Contoh: Ir. Budi Santoso", required := true)
              ),
              div(cls := "col-md-6")(
                label(cls := "form-label small fw-medium text-muted", "Alasan Perubahan"),
                input(`type` := "text", name := "changeNotes", cls := "form-control form-control-sm", placeholder := "Contoh: Naikkan nutrisi pembungaan musim hujan", required := true)
              )
            ),
            div(cls := "row g-3 mb-3")(
              div(cls := "col-md-4")(
                label(cls := "form-label small fw-medium text-muted", "Pilih Tahap"),
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
                label(cls := "form-label small fw-medium text-muted", "Target Anjuran Baru"),
                input(`type` := "number", step := "0.01", name := "newTargetValue", cls := "form-control form-control-sm", placeholder := "Contoh: 2.8", required := true)
              )
            ),
            div(cls := "text-end")(
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Simpan Draf & Tinjau Perubahan")
            )
          )
        )
      ),

      // Cultivation Stages List (Separated with Spacing & Dividers, No Over-containerization)
      div(cls := "mb-4")(
        h2(cls := "section-label mb-3", "Tahapan Budidaya & Petunjuk Lapangan"),
        div(cls := "border-top")(
          snap.stages.zipWithIndex.map { case (stage, idx) =>
            div(cls := "py-4 border-bottom")(
              div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                h3(cls := "h6 fw-bold text-dark mb-0", s"${idx + 1}. ${stage.name}"),
                span(cls := "small text-muted", s"Durasi: ${stage.durationDays} Hari")
              ),
              // Farmer instruction
              p(cls := "small text-secondary mb-3",
                strong(cls := "text-dark", "Petunjuk Petani: "),
                stage.instructions
              ),
              // Parameter table
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
  // 5. REVIEW PERUBAHAN ("Apa yang berubah?" & PUBLISH)
  // ==========================================================================

  def reviewPerubahanView(
    guide: Database#GuideRow,
    currentRevision: GuideRevision,
    parentRevisionOpt: Option[GuideRevision],
    diffSummary: ChangeSummary
  ): Modifier =
    div(
      nav(aria.label := "breadcrumb", cls := "mb-3")(
        ol(cls := "breadcrumb small text-muted mb-0")(
          li(cls := "breadcrumb-item")(a(href := "/panduan", cls := "text-decoration-none text-muted", "Panduan")),
          li(cls := "breadcrumb-item")(a(href := s"/panduan/${guide.id}", cls := "text-decoration-none text-muted", guide.name)),
          li(cls := "breadcrumb-item active text-dark", "Tinjauan Perubahan")
        )
      ),

      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Tinjauan Perubahan: Apa yang Berubah?"),
        p(cls := "text-muted small mb-0", s"Pemeriksaan perubahan rekomendasi sebelum menerbitkan edisi resmi untuk '${guide.name}'.")
      ),

      // Human-readable Changes List
      div(cls := "mb-4")(
        h2(cls := "section-label mb-3", "Perbedaan Rekomendasi Budidaya"),
        if diffSummary.changes.isEmpty then
          div(cls := "p-4 text-center text-muted border rounded-3 bg-white small", "Tidak ada perbedaan takaran antara revisi ini dengan versi sebelumnya.")
        else
          div(cls := "border-top")(
            diffSummary.changes.map { c =>
              div(cls := "py-3 border-bottom")(
                div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                  span(cls := "fw-semibold text-dark", s"${c.stageName} → ${c.paramName}"),
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

      // Supervisor Approval Box
      div(cls := "p-3 p-md-4 rounded-3 bg-light border")(
        h2(cls := "h6 fw-bold text-dark text-uppercase tracking-wide mb-1", "Persetujuan & Publikasi Resmi Mandor"),
        p(cls := "small text-muted mb-3", "Setelah diterbitkan, panduan ini akan resmi menjadi acuan operasional untuk seluruh masa tanam baru."),
        form(action := s"/panduan/${guide.id}/terbitkan", method := "POST")(
          input(`type` := "hidden", name := "revisionId", value := currentRevision.id),
          div(cls := "row g-3 mb-3")(
            div(cls := "col-md-6")(
              label(cls := "form-label small fw-medium text-muted", "Nama Edisi Resmi Baru"),
              input(`type` := "text", name := "editionTag", cls := "form-control form-control-sm", placeholder := "Contoh: Edisi 2026.2 - Musim Hujan", required := true)
            ),
            div(cls := "col-md-6")(
              label(cls := "form-label small fw-medium text-muted", "Nama Mandor / Kepala Kebun"),
              input(`type` := "text", name := "approvedBy", cls := "form-control form-control-sm", placeholder := "Contoh: H. Suwandi (Kepala Kebun)", required := true)
            )
          ),
          div(cls := "mb-3")(
            label(cls := "form-label small fw-medium text-muted", "Catatan Persetujuan"),
            textarea(name := "notes", cls := "form-control form-control-sm", rows := 2, placeholder := "Contoh: Takaran pupuk dan pH telah disesuaikan dengan kondisi cuaca terkini.")
          ),
          div(cls := "d-flex justify-content-between align-items-center")(
            a(cls := "btn btn-sm btn-verdant-outline", href := s"/panduan/${guide.id}", "Kembali ke Panduan"),
            button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Setujui & Publikasikan")
          )
        )
      )
    )

  // ==========================================================================
  // 6. MASA TANAM (CULTIVATION PERIODS LIST)
  // ==========================================================================

  def masaTanamListView(
    periods: List[(CultivationPeriod, String)],
    publishedGuides: List[(PublishedGuide, String)]
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-bold text-dark mb-1", "Masa Tanam"),
          p(cls := "text-muted small mb-0", "Pengelolaan siklus tanam di lahan yang terkunci pada panduan resmi.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#bukaMasaTanamModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Buka Masa Tanam Baru")
      ),

      div(cls := "row g-3")(
        periods.map { case (periodItem, gName) =>
          div(cls := "col-md-6")(
            div(cls := "panel-verdant h-100 d-flex flex-column justify-content-between")(
              div(
                div(cls := "d-flex justify-content-between align-items-baseline mb-2")(
                  span(cls := "small text-muted fw-medium", periodItem.code),
                  span(cls := "badge bg-light text-dark border", periodItem.status)
                ),
                h2(cls := "h5 fw-bold text-dark mb-1", a(cls := "text-decoration-none text-dark", href := s"/masa-tanam/${periodItem.code}", periodItem.code)),
                p(cls := "small text-muted mb-2", s"Komoditas: ${periodItem.cropVariety} · Lokasi: ${periodItem.plotName}"),
                div(cls := "small text-secondary mb-3", s"Panduan Terkunci: ${periodItem.publishedEdition}")
              ),
              div(cls := "pt-2 border-top text-end")(
                a(cls := "btn btn-sm btn-verdant-outline", href := s"/masa-tanam/${periodItem.code}", "Buka Garis Waktu →")
              )
            )
          )
        }
      ),

      // Modal: Buka Masa Tanam Baru
      div(cls := "modal fade", id := "bukaMasaTanamModal", tabindex := "-1")(
        div(cls := "modal-dialog modal-dialog-centered")(
          div(cls := "modal-content border rounded-3")(
            form(action := "/masa-tanam/buka", method := "POST")(
              div(cls := "modal-header border-bottom py-3")(
                h5(cls := "modal-title fs-6 fw-bold text-dark", "Buka Masa Tanam Baru"),
                button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
              ),
              div(cls := "modal-body py-3")(
                div(cls := "mb-3")(
                  label(cls := "form-label small fw-medium text-muted", "Kode Masa Tanam"),
                  input(`type` := "text", name := "code", cls := "form-control form-control-sm", placeholder := "Contoh: MT-2026-TOMAT-02", required := true)
                ),
                div(cls := "mb-3")(
                  label(cls := "form-label small fw-medium text-muted", "Pilih Panduan Resmi yang Berlaku"),
                  select(name := "guideSelection", cls := "form-select form-select-sm", required := true)(
                    if publishedGuides.isEmpty then
                      option(value := "", "Belum ada panduan resmi diterbitkan")
                    else
                      publishedGuides.map { case (pub, guideTitle) =>
                        option(value := s"${pub.guideId}|${pub.editionTag}", s"$guideTitle (${pub.editionTag})")
                      }
                  )
                ),
                div(cls := "mb-3")(
                  label(cls := "form-label small fw-medium text-muted", "Nama Lahan / Petak / Greenhouse"),
                  input(`type` := "text", name := "plotName", cls := "form-control form-control-sm", placeholder := "Contoh: Greenhouse Blok 3B atau Lahan Barat Petak 1", required := true)
                )
              ),
              div(cls := "modal-footer border-top py-2")(
                button(`type` := "button", cls := "btn btn-sm btn-light border", data("bs-dismiss") := "modal", "Batal"),
                button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Buka Masa Tanam")
              )
            )
          )
        )
      )
    )

  // ==========================================================================
  // 7. MASA TANAM DETAIL (REFACTORED: CALM, BALANCED, ZERO OVER-CONTAINERIZATION)
  // ==========================================================================

  def masaTanamDetailView(
    period: CultivationPeriod,
    guide: Database#GuideRow,
    revision: GuideRevision,
    publishedGuide: PublishedGuide,
    activities: List[CultivationActivity]
  ): Modifier =
    val snap = revision.snapshot
    div(
      // Breadcrumb
      nav(aria.label := "breadcrumb", cls := "mb-3")(
        ol(cls := "breadcrumb small text-muted mb-0")(
          li(cls := "breadcrumb-item")(a(href := "/masa-tanam", cls := "text-decoration-none text-muted", "Masa Tanam")),
          li(cls := "breadcrumb-item active text-dark", period.code)
        )
      ),

      // 1. Compact Page Header with Crop & Plot Name
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            h1(cls := "h4 fw-bold text-dark mb-0", s"${period.cropVariety} — ${period.plotName}"),
            span(cls := "badge bg-light text-dark border", period.status)
          ),
          // 2. Subtle supporting info line (NOT a large colored alert box)
          div(cls := "small text-muted")(
            span(s"Kode: ${period.code} · Dimulai: ${formatTs(period.startDate)} · "),
            span(s"Terkunci pada Panduan Resmi: ${period.publishedEdition} (disetujui oleh ${publishedGuide.approvedBy})")
          )
        ),
        // 4. One clear primary action
        button(
          cls := "btn btn-verdant text-nowrap",
          data("bs-toggle") := "modal",
          data("bs-target") := "#catatKegiatanModal"
        )(i(cls := "bi bi-plus-lg me-1"), "Catat Kegiatan")
      ),

      // Balanced Layout: Primary Activity Journal & Compact Stages Guide
      div(cls := "row g-4")(
        // 3. Primary Content: Catatan Kegiatan Lapangan (Chronological List with Thin Separators)
        div(cls := "col-lg-7")(
          div(cls := "mb-4")(
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
              div(cls := "border-top")(
                activities.map { act =>
                  div(cls := "py-3 border-bottom")(
                    div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                      div(
                        span(cls := "fw-semibold text-dark me-2", act.activityType),
                        act.quantity.map(q => span(cls := "small text-muted", s"(${q})")).getOrElse(modifier())
                      ),
                      span(cls := "small text-muted", act.activityDate)
                    ),
                    p(cls := "mb-1 text-secondary small", act.notes),
                    div(cls := "small text-muted", s"Pelaksana: ${act.farmerName}")
                  )
                }
              )
          )
        ),

        // 5. Compact List of Stages (Panduan Budidaya - Divided with Spacing, No Nested Cards)
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

      catatKegiatanModal(List((period, guide.name)))
    )

  // ==========================================================================
  // 8. RIWAYAT (HISTORY & REVISIONS)
  // ==========================================================================

  def riwayatView(
    revisions: List[GuideRevision],
    publishedEditions: List[(PublishedGuide, String)]
  ): Modifier =
    div(
      div(cls := "pb-3 mb-4 border-bottom")(
        h1(cls := "h4 fw-bold text-dark mb-1", "Riwayat Perubahan & Edisi Panduan"),
        p(cls := "text-muted small mb-0", "Lacak seluruh jejak perubahan panduan budidaya serta riwayat pemulihan versi.")
      ),

      div(cls := "row g-4")(
        // Published Editions History
        div(cls := "col-md-5")(
          div(cls := "mb-4")(
            h2(cls := "section-label mb-3", "Riwayat Edisi Panduan Resmi"),
            if publishedEditions.isEmpty then
              div(cls := "p-4 text-center text-muted border rounded-3 bg-white small", "Belum ada edisi resmi yang diterbitkan.")
            else
              div(cls := "border-top")(
                publishedEditions.map { case (pub, gName) =>
                  div(cls := "py-3 border-bottom")(
                    div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                      span(cls := "badge bg-light text-dark border", pub.editionTag),
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

        // Revision Log & Restore
        div(cls := "col-md-7")(
          div(cls := "mb-4")(
            h2(cls := "section-label mb-3", "Catatan Revisi & Pemulihan Versi"),
            if revisions.isEmpty then
              div(cls := "p-4 text-center text-muted border rounded-3 bg-white small", "Belum ada riwayat revisi.")
            else
              div(cls := "border-top")(
                revisions.zipWithIndex.map { case (rev, idx) =>
                  div(cls := "py-3 border-bottom")(
                    div(cls := "d-flex justify-content-between align-items-baseline mb-1")(
                      span(cls := "fw-semibold text-dark", rev.changeNotes),
                      span(cls := "small text-muted", formatTs(rev.timestamp))
                    ),
                    div(cls := "small text-muted mb-2", s"Penyusun: ${rev.author}"),
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

