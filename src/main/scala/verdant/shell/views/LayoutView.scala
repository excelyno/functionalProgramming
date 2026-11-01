package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.{nav, title, style as cssStyle}
import verdant.core.User

/**
 * Kerangka halaman: head, CSS global, navbar, navigasi bawah (HP), dan peran aktif.
 *
 * Catatan CSS: `badge-git-immutable`, `badge-cetakan`, `panel-verdant`, dan
 * `section-label` masih didefinisikan karena view mandor/jurnal belum semuanya
 * dirapikan. Hapus setelah tidak ada yang memakai.
 */
trait LayoutView:

  // ==========================================================================
  // MASTER LAYOUT WRAPPER
  // ==========================================================================
  def layout(
    pageTitle: String,
    activeNav: String,
    currentRole: String = "farmer",
    alert: Option[(String, String)] = None,
    currentUser: Option[User] = None
  )(pageContent: Modifier*): String =
    val effectiveRole = currentUser.map(u => if u.isSupervisor then "supervisor" else "farmer").getOrElse(currentRole)
    val isSupervisor = effectiveRole.equalsIgnoreCase("supervisor") || effectiveRole.equalsIgnoreCase("mandor")
    val roleLabel = if isSupervisor then "Mandor" else "Petani"

    def navLink(key: String, target: String, text: String): Modifier =
      li(cls := "nav-item")(
        a(cls := s"nav-link nav-link-verdant ${if activeNav == key then "active" else ""}", href := target)(text)
      )

    "<!DOCTYPE html>" + html(lang := "id")(
      head(
        meta(charset := "utf-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1"),
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
            --verdant-primary: #24512f;
            --verdant-primary-hover: #2d6339;
            --verdant-canvas: #f5f6f3;
            --verdant-card-bg: #ffffff;
            --verdant-border: #d9ddd7;
            --verdant-border-subtle: #eceee9;
            --verdant-text: #1d261f;
            --verdant-text-muted: #5f6b61;
          }
          body {
            background-color: var(--verdant-canvas);
            color: var(--verdant-text);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            font-size: 14.5px;
            line-height: 1.55;
          }
          @media (max-width: 991.98px) { body { padding-bottom: 72px; } }

          a { color: var(--verdant-primary); }
          :focus-visible { outline: 2px solid var(--verdant-primary); outline-offset: 2px; }

          /* Navbar */
          .navbar-verdant {
            background-color: var(--verdant-primary);
            border-bottom: 1px solid rgba(0, 0, 0, 0.12);
          }
          .nav-link-verdant {
            color: rgba(255, 255, 255, 0.85) !important;
            font-size: 13.5px;
            padding: 6px 12px !important;
            border-radius: 4px;
          }
          .nav-link-verdant:hover { color: #fff !important; background-color: rgba(255, 255, 255, 0.10); }
          .nav-link-verdant.active { color: #fff !important; background-color: rgba(255, 255, 255, 0.16); }

          /* Tombol */
          .btn-verdant, .btn-verdant-outline {
            font-size: 13.5px;
            font-weight: 500;
            border-radius: 6px;
            padding: 6px 14px;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            transition: background-color .12s, border-color .12s, color .12s;
          }
          .btn-verdant { background-color: var(--verdant-primary); color: #fff; border: 1px solid var(--verdant-primary); }
          .btn-verdant:hover { background-color: var(--verdant-primary-hover); border-color: var(--verdant-primary-hover); color: #fff; }
          .btn-verdant-outline { background-color: #fff; color: var(--verdant-primary); border: 1px solid var(--verdant-border); }
          .btn-verdant-outline:hover { background-color: var(--verdant-border-subtle); border-color: #bfc5bc; color: var(--verdant-primary); }
          .btn-sm.btn-verdant, .btn-sm.btn-verdant-outline { padding: 4px 12px; font-size: 13px; }

          /* Bootstrap "success" dibuat mengikuti warna utama (pill pilihan, link) */
          .btn-outline-success {
            --bs-btn-color: var(--verdant-primary);
            --bs-btn-border-color: var(--verdant-border);
            --bs-btn-hover-color: var(--verdant-primary);
            --bs-btn-hover-bg: var(--verdant-border-subtle);
            --bs-btn-hover-border-color: #bfc5bc;
            --bs-btn-active-color: #fff;
            --bs-btn-active-bg: var(--verdant-primary);
            --bs-btn-active-border-color: var(--verdant-primary);
          }
          .text-success { color: var(--verdant-primary) !important; }
          .form-control:focus, .form-select:focus {
            border-color: var(--verdant-primary);
            box-shadow: 0 0 0 3px rgba(36, 81, 47, 0.15);
          }
          .modal-header, .modal-footer { border-color: var(--verdant-border-subtle); }

          /* Dipertahankan untuk view yang belum dirapikan */
          .panel-verdant {
            border: 1px solid var(--verdant-border);
            border-radius: 6px;
            padding: 16px;
            background-color: var(--verdant-card-bg);
          }
          .section-label {
            font-size: 13px;
            font-weight: 600;
            color: var(--verdant-text);
          }
          .badge-git-immutable, .badge-cetakan {
            font-size: 11px;
            font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
            color: var(--verdant-text-muted);
            display: inline-flex;
            align-items: center;
            gap: 4px;
          }

          /* Navigasi bawah (HP) */
          .bottom-nav {
            position: fixed;
            bottom: 0; left: 0; right: 0;
            height: 58px;
            background-color: #fff;
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
          }
          .bottom-nav a.active { color: var(--verdant-primary); font-weight: 600; }
          .bottom-nav i { font-size: 19px; margin-bottom: 1px; }
          """
        )
      ),
      body(
        tag("header")(
          nav(cls := "navbar navbar-expand-lg navbar-dark navbar-verdant py-2")(
            div(cls := "container")(
              a(cls := "navbar-brand fw-semibold", href := "/", "Verdant Pertanian"),
              button(
                cls := "navbar-toggler border-0",
                `type` := "button",
                data("bs-toggle") := "collapse",
                data("bs-target") := "#navbarVerdantContent",
                attr("aria-label") := "Buka menu"
              )(span(cls := "navbar-toggler-icon")),
              div(cls := "collapse navbar-collapse", id := "navbarVerdantContent")(
                ul(cls := "navbar-nav me-auto mb-2 mb-lg-0")(
                  navLink("beranda", "/", "Beranda"),
                  if !isSupervisor then
                    Seq(
                      navLink("masa-tanam", "/masa-tanam", "Lahan"),
                      navLink("catatan", "/catatan", "Catatan"),
                      navLink("panduan", "/panduan", "Cetakan")
                    )
                  else
                    Seq(
                      navLink("kelola-panduan", "/kelola-panduan", "Kelola cetakan"),
                      navLink("pemeriksaan-perubahan", "/pemeriksaan-perubahan", "Periksa perubahan"),
                      navLink("riwayat", "/riwayat", "Riwayat")
                    )
                ),
                div(cls := "d-flex align-items-center gap-2 mt-2 mt-lg-0")(
                  currentUser match
                    case Some(u) =>
                      div(cls := "d-flex align-items-center gap-2")(
                        div(cls := "text-end d-none d-sm-block text-white", style := "line-height: 1.2;")(
                          div(cls := "fw-semibold", style := "font-size: 13px;", u.fullName),
                          span(cls := "text-white-50", style := "font-size: 11px;", if u.isSupervisor then "Mandor" else "Petani")
                        ),
                        a(
                          href := "/logout",
                          cls := "btn btn-sm btn-outline-light py-1 px-2",
                          attr("title") := "Keluar dari akun",
                          style := "font-size: 12.5px;"
                        )(
                          i(cls := "bi bi-box-arrow-right me-1"),
                          "Keluar"
                        )
                      )
                    case None =>
                      div(cls := "d-flex align-items-center gap-2")(
                        a(
                          href := "/login",
                          cls := "btn btn-sm btn-light py-1 px-3 fw-semibold text-dark",
                          style := "font-size: 13px;"
                        )(
                          i(cls := "bi bi-box-arrow-in-right me-1"),
                          "Masuk"
                        )
                      )
                )
              )
            )
          )
        ),

        div(cls := "container py-3 py-md-4")(
          alert match
            case Some((kind, msg)) =>
              val tone = if kind == "success" then "border-success bg-success-subtle text-success-emphasis" else "border-danger bg-danger-subtle text-danger-emphasis"
              div(cls := s"alert $tone alert-dismissible fade show mb-3 border py-2 px-3 small rounded-2", role := "alert")(
                div(cls := "d-flex align-items-center justify-content-between")(
                  div(raw(msg.replace("\n", "<br/>"))),
                  button(`type` := "button", cls := "btn-close small p-2", data("bs-dismiss") := "alert", attr("aria-label") := "Tutup")
                )
              )
            case None => modifier()
          ,
          pageContent
        ),

        div(cls := "bottom-nav d-lg-none")(
          if !isSupervisor then
            Seq(
              a(href := "/", cls := (if activeNav == "beranda" then "active" else ""))(i(cls := "bi bi-house"), span("Beranda")),
              a(href := "/masa-tanam", cls := (if activeNav == "masa-tanam" then "active" else ""))(i(cls := "bi bi-calendar3"), span("Lahan")),
              a(href := "/catatan", cls := (if activeNav == "catatan" then "active" else ""))(i(cls := "bi bi-journal-text"), span("Catatan")),
              a(href := "/panduan", cls := (if activeNav == "panduan" then "active" else ""))(i(cls := "bi bi-book"), span("Cetakan")),
              a(href := "/logout")(i(cls := "bi bi-box-arrow-right"), span("Keluar"))
            )
          else
            Seq(
              a(href := "/", cls := (if activeNav == "beranda" then "active" else ""))(i(cls := "bi bi-house"), span("Beranda")),
              a(href := "/kelola-panduan", cls := (if activeNav == "kelola-panduan" then "active" else ""))(i(cls := "bi bi-journal-check"), span("Cetakan")),
              a(href := "/pemeriksaan-perubahan", cls := (if activeNav == "pemeriksaan-perubahan" then "active" else ""))(i(cls := "bi bi-arrow-left-right"), span("Perubahan")),
              a(href := "/riwayat", cls := (if activeNav == "riwayat" then "active" else ""))(i(cls := "bi bi-clock-history"), span("Riwayat")),
              a(href := "/logout")(i(cls := "bi bi-box-arrow-right"), span("Keluar"))
            )
        ),

        script(src := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js")
      )
    ).render

  // ==========================================================================
  // 403 FORBIDDEN VIEW
  // ==========================================================================
  def forbiddenView(requiredRole: String, currentRole: String): Modifier =
    div(cls := "py-5")(
      div(cls := "p-4 p-md-5 bg-white border rounded-2 mx-auto", style := "max-width: 520px;")(
        h1(cls := "h5 fw-semibold text-dark mb-2", "Akses ditolak (403)"),
        p(cls := "text-secondary mb-4",
          "Halaman ini khusus untuk akun dengan hak akses ",
          strong(cls := "text-dark", requiredRole),
          ". Akun Anda saat ini memiliki peran ",
          strong(cls := "text-dark", currentRole),
          "."
        ),
        div(cls := "d-flex gap-2 flex-wrap")(
          a(cls := "btn btn-verdant", href := "/login", "Masuk dengan Akun Lain"),
          a(cls := "btn btn-verdant-outline", href := "/", "Kembali ke Beranda")
        )
      )
    )