package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.{nav, title, style as cssStyle}

/**
 * ============================================================================
 * LAYOUT VIEW: Kerangka Master HTML, Navigasi, CSS Nyaman di Mata, & Role Switcher
 * ============================================================================
 * 
 * Modul ini mengatur kerangka visual luar yang nyaman di mata (eyestrain-free):
 * - Menggunakan palet hijau alami (#1b4332 / #2d6a4f), latar soft slate (#f8fafc),
 *   dan kartu berbayangan lembut (#ffffff border #e2e8f0).
 * - Menegaskan konsep sederhana:
 *   1. MANDOR: Membuat & menguji "Cetakan Budidaya (Master SOP)".
 *   2. PETANI: Menerapkan cetakan ke lahan & mencatat log harian.
 *   3. CATATAN KEKAL: Log kegiatan petani bersifat permanen (anti-hapus ala Git).
 */
trait LayoutView:

  // ==========================================================================
  // MASTER LAYOUT WRAPPER
  // ==========================================================================
  def layout(
    pageTitle: String,
    activeNav: String,
    currentRole: String = "farmer",
    alert: Option[(String, String)] = None
  )(pageContent: Modifier*): String =
    val isSupervisor = currentRole.equalsIgnoreCase("supervisor") || currentRole.equalsIgnoreCase("mandor")
    val roleLabel = if isSupervisor then "Mandor" else "Petani"
    val switchTargetRole = if isSupervisor then "farmer" else "supervisor"
    val switchTargetLabel = if isSupervisor then "Ganti ke Mode Petani" else "Ganti ke Mode Mandor"
    val switchTargetIcon = if isSupervisor then "bi-person-check" else "bi-pencil-square"

    "<!DOCTYPE html>" + html(lang := "id")(
      head(
        meta(charset := "utf-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"),
        title(s"$pageTitle · Verdant Pertanian ($roleLabel)"),
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
            --verdant-primary-hover: #2d6a4f;
            --verdant-accent: #52b788;
            --verdant-canvas: #f8fafc;
            --verdant-card-bg: #ffffff;
            --verdant-border: #e2e8f0;
            --verdant-border-subtle: #f1f5f9;
            --verdant-text: #0f172a;
            --verdant-text-muted: #64748b;
            --verdant-text-subtle: #94a3b8;
            --verdant-git-badge-bg: #e0e7ff;
            --verdant-git-badge-text: #3730a3;
          }
          body {
            background-color: var(--verdant-canvas);
            color: var(--verdant-text);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            font-size: 14.5px;
            line-height: 1.6;
            padding-bottom: 80px;
          }
          .navbar-verdant {
            background-color: var(--verdant-primary);
            border-bottom: 1px solid rgba(0, 0, 0, 0.08);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
          }
          .nav-link-verdant {
            color: rgba(255, 255, 255, 0.88) !important;
            font-size: 13.5px;
            font-weight: 500;
            padding: 7px 13px !important;
            border-radius: 6px;
            transition: all 0.15s ease-in-out;
          }
          .nav-link-verdant.active, .nav-link-verdant:hover {
            color: #ffffff !important;
            background-color: rgba(255, 255, 255, 0.15);
          }
          .btn-verdant {
            background-color: var(--verdant-primary);
            color: #ffffff;
            font-size: 13.5px;
            font-weight: 500;
            border: 1px solid var(--verdant-primary);
            border-radius: 7px;
            padding: 7px 16px;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 1px 2px rgba(0,0,0,0.05);
            transition: all 0.15s ease-in-out;
          }
          .btn-verdant:hover {
            background-color: var(--verdant-primary-hover);
            color: #ffffff;
            border-color: var(--verdant-primary-hover);
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
          }
          .btn-verdant-outline {
            background-color: #ffffff;
            color: var(--verdant-primary);
            font-size: 13.5px;
            font-weight: 500;
            border: 1px solid var(--verdant-border);
            border-radius: 7px;
            padding: 7px 16px;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 1px 2px rgba(0,0,0,0.02);
            transition: all 0.15s ease-in-out;
          }
          .btn-verdant-outline:hover {
            background-color: var(--verdant-border-subtle);
            color: var(--verdant-primary-hover);
            border-color: #cbd5e1;
          }
          .panel-verdant {
            border: 1px solid var(--verdant-border);
            border-radius: 10px;
            padding: 20px;
            background-color: var(--verdant-card-bg);
            box-shadow: 0 1px 3px rgba(0,0,0,0.03);
          }
          .section-label {
            font-size: 12.5px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.04em;
            color: var(--verdant-text-muted);
          }
          .badge-git-immutable {
            background-color: var(--verdant-git-badge-bg);
            color: var(--verdant-git-badge-text);
            border: 1px solid #c7d2fe;
            font-size: 11px;
            font-weight: 600;
            padding: 3px 8px;
            border-radius: 6px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
          }
          .badge-cetakan {
            background-color: #ecfdf5;
            color: #065f46;
            border: 1px solid #a7f3d0;
            font-size: 11px;
            font-weight: 600;
            padding: 3px 8px;
            border-radius: 6px;
          }
          .bottom-nav {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            height: 62px;
            background-color: #ffffff;
            border-top: 1px solid var(--verdant-border);
            display: flex;
            align-items: center;
            justify-content: space-around;
            z-index: 1030;
            box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
          }
          .bottom-nav a {
            color: var(--verdant-text-muted);
            text-decoration: none;
            display: flex;
            flex-direction: column;
            align-items: center;
            font-size: 11px;
            font-weight: 500;
          }
          .bottom-nav a.active {
            color: var(--verdant-primary);
            font-weight: 600;
          }
          .bottom-nav i {
            font-size: 20px;
            margin-bottom: 2px;
          }
          """
        )
      ),
      body(
        // Top Navbar
        tag("header")(
          nav(cls := "navbar navbar-expand-lg navbar-dark navbar-verdant py-2")(
            div(cls := "container")(
              a(cls := "navbar-brand fw-bold d-flex align-items-center gap-2", href := "/")(
                i(cls := "bi bi-sprout text-warning fs-5"),
                span("Verdant Pertanian")
              ),
              button(
                cls := "navbar-toggler border-0",
                `type` := "button",
                data("bs-toggle") := "collapse",
                data("bs-target") := "#navbarVerdantContent"
              )(span(cls := "navbar-toggler-icon")),
              div(cls := "collapse navbar-collapse", id := "navbarVerdantContent")(
                ul(cls := "navbar-nav me-auto mb-2 mb-lg-0")(
                  li(cls := "nav-item")(
                    a(cls := s"nav-link nav-link-verdant ${if activeNav == "beranda" then "active" else ""}", href := "/")("Beranda")
                  ),
                  if !isSupervisor then
                    Seq(
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "masa-tanam" then "active" else ""}", href := "/masa-tanam")("Lahan Saya (Cetakan Terkunci)")
                      ),
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "catatan" then "active" else ""}", href := "/catatan")("Buku Catatan Lahan (Log Abadi)")
                      ),
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "panduan" then "active" else ""}", href := "/panduan")("Katalog Cetakan Mandor")
                      )
                    )
                  else
                    Seq(
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "kelola-panduan" then "active" else ""}", href := "/kelola-panduan")("Kelola Cetakan Budidaya")
                      ),
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "pemeriksaan-perubahan" then "active" else ""}", href := "/pemeriksaan-perubahan")("Periksa Perubahan Draf")
                      ),
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "riwayat" then "active" else ""}", href := "/riwayat")("Riwayat & Silsilah Cetakan")
                      )
                    )
                ),
                // Role Switcher in Navbar
                div(cls := "d-flex align-items-center gap-2 mt-2 mt-lg-0")(
                  span(cls := "small text-white-50 d-none d-xl-inline me-1", s"Sedang Aktif: $roleLabel"),
                  a(
                    href := s"/role?set=$switchTargetRole&redirect=/",
                    cls := "btn btn-sm btn-outline-light py-1 px-3 d-flex align-items-center gap-1",
                    style := "font-size: 13px; border-radius: 6px;"
                  )(
                    i(cls := s"bi $switchTargetIcon"),
                    span(switchTargetLabel)
                  )
                )
              )
            )
          )
        ),

        // Main Container
        div(cls := "container py-3 py-md-4")(
          alert match
            case Some((kind, msg)) =>
              val borderClass = if kind == "success" then "border-success bg-success-subtle text-success-emphasis" else "border-danger bg-danger-subtle text-danger-emphasis"
              div(cls := s"alert $borderClass alert-dismissible fade show mb-3 border py-2 px-3 small rounded-3", role := "alert")(
                div(cls := "d-flex align-items-center justify-content-between")(
                  div(raw(msg.replace("\n", "<br/>"))),
                  button(`type` := "button", cls := "btn-close small p-2", data("bs-dismiss") := "alert")
                )
              )
            case None => modifier()
          ,
          pageContent
        ),

        // Mobile Bottom Navigation Bar (Role-Specific)
        div(cls := "bottom-nav d-lg-none")(
          if !isSupervisor then
            Seq(
              a(href := "/", cls := (if activeNav == "beranda" then "active" else ""))(
                i(cls := "bi bi-house"),
                span("Beranda")
              ),
              a(href := "/masa-tanam", cls := (if activeNav == "masa-tanam" then "active" else ""))(
                i(cls := "bi bi-calendar3"),
                span("Lahan")
              ),
              a(href := "/catatan", cls := (if activeNav == "catatan" then "active" else ""))(
                i(cls := "bi bi-journal-text"),
                span("Buku Log")
              ),
              a(href := "/role?set=supervisor&redirect=/", cls := "")(
                i(cls := "bi bi-shield-check"),
                span("Ke Mandor")
              )
            )
          else
            Seq(
              a(href := "/", cls := (if activeNav == "beranda" then "active" else ""))(
                i(cls := "bi bi-house"),
                span("Beranda")
              ),
              a(href := "/kelola-panduan", cls := (if activeNav == "kelola-panduan" then "active" else ""))(
                i(cls := "bi bi-journal-check"),
                span("Cetakan")
              ),
              a(href := "/pemeriksaan-perubahan", cls := (if activeNav == "pemeriksaan-perubahan" then "active" else ""))(
                i(cls := "bi bi-arrow-left-right"),
                span("Perubahan")
              ),
              a(href := "/riwayat", cls := (if activeNav == "riwayat" then "active" else ""))(
                i(cls := "bi bi-clock-history"),
                span("Riwayat")
              ),
              a(href := "/role?set=farmer&redirect=/", cls := "")(
                i(cls := "bi bi-person"),
                span("Ke Petani")
              )
            )
        ),

        script(src := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js")
      )
    ).render

  // ==========================================================================
  // 403 FORBIDDEN VIEW
  // ==========================================================================
  def forbiddenView(requiredRole: String, currentRole: String): Modifier =
    div(cls := "py-5 text-center")(
      div(cls := "p-4 p-md-5 bg-white border rounded-4 shadow-sm mx-auto", style := "max-width: 580px;")(
        div(cls := "text-danger mb-3")(
          i(cls := "bi bi-shield-lock-fill", style := "font-size: 3rem;")
        ),
        h1(cls := "h4 fw-bold text-dark mb-2", "Akses Ditolak (403 Forbidden)"),
        p(cls := "text-secondary mb-3",
          s"Halaman atau tindakan ini memerlukan hak akses peran ",
          strong(cls := "text-dark", requiredRole),
          s". Anda saat ini menggunakan mode ",
          strong(cls := "text-dark", currentRole),
          "."
        ),
        div(cls := "p-3 bg-light rounded-3 text-start small text-muted mb-4 border")(
          strong(cls := "text-dark d-block mb-1", "💡 Pergantian Peran Praktis:"),
          "Untuk mempermudah simulasi, Anda dapat langsung beralih peran kapan saja dengan tombol di bawah ini."
        ),
        div(cls := "d-flex justify-content-center gap-2 flex-wrap")(
          if requiredRole.toLowerCase.contains("mandor") || requiredRole.toLowerCase.contains("supervisor") then
            a(cls := "btn btn-verdant", href := "/role?set=supervisor&redirect=/", "Beralih ke Mode Mandor Sekarang")
          else
            a(cls := "btn btn-verdant", href := "/role?set=farmer&redirect=/", "Beralih ke Mode Petani Sekarang")
          ,
          a(cls := "btn btn-verdant-outline", href := "/", "Kembali ke Beranda")
        )
      )
    )
