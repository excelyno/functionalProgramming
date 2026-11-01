package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.{nav, title, style as cssStyle}

/**
 * ============================================================================
 * LAYOUT VIEW: Kerangka Master HTML, Navigasi, CSS, & Role Switcher
 * ============================================================================
 * 
 * Modul ini mengatur "bingkai luar" seluruh halaman web di Verdant:
 * - <head> berisi meta tags, Bootstrap 5.3.3, Bootstrap Icons, dan tema warna Verdant
 * - Top Navbar: Menu navigasi adaptif sesuai peran (Petani vs Mandor) + Tombol Beralih Peran
 * - Wadah Notifikasi / Alert (hijau untuk sukses, merah untuk error/peringatan)
 * - Bottom Navigation Bar untuk layar HP / Mobile
 * - Tampilan 403 Forbidden ketika seorang Petani mencoba mengakses fitur Mandor
 * 
 * KONSEP SCALA UNTUK PEMULA:
 * - `pageContent: Modifier*` -> Tanda asterisk (*) artinya variadic parameter / varargs.
 *   Fungsi ini bisa menerima 1, 2, atau banyak elemen HTML sekaligus untuk diletakkan di dalam container.
 * - `.render` -> Scalatags mengubah struktur pohon objek Scala menjadi string HTML siap kirim ke browser.
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
    val switchTargetLabel = if isSupervisor then "Beralih ke Petani" else "Beralih ke Mandor"
    val switchTargetIcon = if isSupervisor then "bi-person" else "bi-shield-check"

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
            --verdant-primary-hover: #143326;
            --verdant-canvas: #ffffff;
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
            padding-bottom: 76px;
          }
          .navbar-verdant {
            background-color: var(--verdant-primary);
            border-bottom: 1px solid rgba(0, 0, 0, 0.1);
          }
          .nav-link-verdant {
            color: rgba(255, 255, 255, 0.82) !important;
            font-size: 14px;
            font-weight: 500;
            padding: 6px 12px !important;
            border-radius: 4px;
          }
          .nav-link-verdant.active, .nav-link-verdant:hover {
            color: #ffffff !important;
            background-color: rgba(255, 255, 255, 0.14);
          }
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
            color: var(--verdant-primary);
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
            color: var(--verdant-primary-hover);
            border-color: #d4d4d8;
          }
          .panel-verdant {
            border: 1px solid var(--verdant-border);
            border-radius: 8px;
            padding: 20px;
            background-color: #ffffff;
          }
          .section-label {
            font-size: 13px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--verdant-text-muted);
          }
          .bottom-nav {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            height: 60px;
            background-color: #ffffff;
            border-top: 1px solid var(--verdant-border);
            display: flex;
            align-items: center;
            justify-content: space-around;
            z-index: 1030;
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
                i(cls := "bi bi-sprout text-warning"),
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
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "masa-tanam" then "active" else ""}", href := "/masa-tanam")("Masa Tanam")
                      ),
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "panduan" then "active" else ""}", href := "/panduan")("Panduan Budidaya")
                      ),
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "catatan" then "active" else ""}", href := "/catatan")("Catatan Harian")
                      )
                    )
                  else
                    Seq(
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "kelola-panduan" then "active" else ""}", href := "/kelola-panduan")("Kelola Panduan")
                      ),
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "pemeriksaan-perubahan" then "active" else ""}", href := "/pemeriksaan-perubahan")("Pemeriksaan Perubahan")
                      ),
                      li(cls := "nav-item")(
                        a(cls := s"nav-link nav-link-verdant ${if activeNav == "riwayat" then "active" else ""}", href := "/riwayat")("Riwayat Panduan")
                      )
                    )
                ),
                // Role Switcher in Navbar
                div(cls := "d-flex align-items-center gap-2 mt-2 mt-lg-0")(
                  span(cls := "small text-white-50 d-none d-xl-inline me-1", s"Peran: $roleLabel"),
                  a(
                    href := s"/role?set=$switchTargetRole&redirect=/",
                    cls := "btn btn-sm btn-outline-light py-1 px-3 d-flex align-items-center gap-1",
                    style := "font-size: 13px;"
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
                span("Masa Tanam")
              ),
              a(href := "/panduan", cls := (if activeNav == "panduan" then "active" else ""))(
                i(cls := "bi bi-book"),
                span("Panduan")
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
                span("Panduan")
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
  // Ditampilkan ketika seorang pengguna mencoba mengakses halaman di luar wewenang perannya.
  // ==========================================================================
  def forbiddenView(requiredRole: String, currentRole: String): Modifier =
    div(cls := "py-5 text-center")(
      div(cls := "p-4 p-md-5 bg-white border rounded-3 shadow-sm mx-auto", style := "max-width: 580px;")(
        div(cls := "text-danger mb-3")(
          i(cls := "bi bi-shield-lock-fill", style := "font-size: 3rem;")
        ),
        h1(cls := "h4 fw-bold text-dark mb-2", "Akses Ditolak (403 Forbidden)"),
        p(cls := "text-secondary mb-3",
          s"Halaman atau tindakan ini memerlukan hak akses peran ",
          strong(cls := "text-dark", requiredRole),
          s". Anda saat ini menggunakan sesi demo sebagai ",
          strong(cls := "text-dark", currentRole),
          "."
        ),
        div(cls := "p-3 bg-light rounded-3 text-start small text-muted mb-4 border")(
          strong(cls := "text-dark d-block mb-1", "ℹ️ Informasi Demo Role Separation:"),
          "Untuk mempermudah pengujian alur bisnis, Anda dapat langsung beralih peran kapan saja tanpa proses login rumit. Gunakan tombol di bawah ini untuk berpindah peran."
        ),
        div(cls := "d-flex justify-content-center gap-2 flex-wrap")(
          if requiredRole.toLowerCase.contains("mandor") || requiredRole.toLowerCase.contains("supervisor") then
            a(cls := "btn btn-verdant", href := "/role?set=supervisor&redirect=/", "Beralih ke Peran Mandor Sekarang")
          else
            a(cls := "btn btn-verdant", href := "/role?set=farmer&redirect=/", "Beralih ke Peran Petani Sekarang")
          ,
          a(cls := "btn btn-verdant-outline", href := "/", "Kembali ke Beranda")
        )
      )
    )

