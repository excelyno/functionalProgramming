package verdant.shell.views

import scalatags.Text.all.*
import scalatags.Text.tags2.title

/**
 * Tampilan Halaman Masuk (Login View) Verdant Pertanian.
 * Memisahkan akun dan hak akses antara Petani dan Mandor.
 */
trait LoginView:

  def loginPage(
    errorMsg: Option[String] = None,
    registeredUsername: String = ""
  ): String =
    "<!DOCTYPE html>" + html(lang := "id")(
      head(
        meta(charset := "utf-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1"),
        title("Masuk · Verdant Pertanian"),
        link(
          rel := "stylesheet",
          href := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
        ),
        link(
          rel := "stylesheet",
          href := "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"
        ),
        tag("style")(
          """
          :root {
            --verdant-primary: #24512f;
            --verdant-primary-hover: #2d6339;
            --verdant-canvas: #f5f6f3;
            --verdant-card-bg: #ffffff;
            --verdant-border: #d9ddd7;
            --verdant-text: #1d261f;
            --verdant-text-muted: #5f6b61;
          }
          body {
            background-color: var(--verdant-canvas);
            color: var(--verdant-text);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px 16px;
          }
          .login-card {
            background-color: var(--verdant-card-bg);
            border: 1px solid var(--verdant-border);
            border-radius: 12px;
            box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
            width: 100%;
            max-width: 480px;
            padding: 32px 28px;
          }
          .brand-logo {
            color: var(--verdant-primary);
            font-size: 26px;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            text-decoration: none;
          }
          .btn-verdant {
            background-color: var(--verdant-primary);
            color: #fff;
            border: 1px solid var(--verdant-primary);
            font-weight: 500;
            border-radius: 6px;
            padding: 8px 16px;
            width: 100%;
          }
          .btn-verdant:hover {
            background-color: var(--verdant-primary-hover);
            color: #fff;
          }
          .quick-btn {
            background: #fff;
            border: 1px solid var(--verdant-border);
            border-radius: 8px;
            padding: 10px 14px;
            text-decoration: none;
            color: var(--verdant-text);
            display: flex;
            align-items: center;
            justify-content: space-between;
            transition: all 0.15s ease;
            font-size: 13.5px;
          }
          .quick-btn:hover {
            border-color: var(--verdant-primary);
            background-color: #f8faf8;
            color: var(--verdant-primary);
            transform: translateY(-1px);
          }
          .role-badge-farmer {
            background-color: #e8f5e9;
            color: #2e7d32;
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 12px;
          }
          .role-badge-supervisor {
            background-color: #e3f2fd;
            color: #1565c0;
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 12px;
          }
          """
        )
      ),
      body(
        div(cls := "login-card")(
          // Header Logo & Deskripsi
          div(cls := "text-center mb-4")(
            div(cls := "brand-logo mb-2 justify-content-center")(
              i(cls := "bi bi-flower1"),
              span("Verdant")
            ),
            h1(cls := "h5 fw-bold text-dark mb-1", "Masuk ke Sistem Pertanian"),
            p(cls := "text-secondary small mb-0", "Pilih akun atau masukkan nama pengguna & kata sandi")
          ),

          // Pesan Error jika ada
          errorMsg match
            case Some(msg) =>
              div(cls := "alert alert-danger py-2 px-3 small rounded-2 mb-3 border-danger", role := "alert")(
                i(cls := "bi bi-exclamation-circle me-1"),
                msg
              )
            case None => modifier()
          ,

          // Form Login Biasa
          form(action := "/login", method := "POST", cls := "mb-4")(
            div(cls := "mb-3")(
              label(cls := "form-label small fw-semibold text-dark mb-1", `for` := "username", "Nama Pengguna (Username)"),
              input(
                `type` := "text",
                id := "username",
                name := "username",
                cls := "form-control form-control-sm py-2",
                value := registeredUsername,
                placeholder := "petani / mandor",
                required := true,
                autofocus := (registeredUsername.isEmpty)
              )
            ),
            div(cls := "mb-3")(
              label(cls := "form-label small fw-semibold text-dark mb-1", `for` := "password", "Kata Sandi"),
              input(
                `type` := "password",
                id := "password",
                name := "password",
                cls := "form-control form-control-sm py-2",
                placeholder := "••••••",
                required := true
              )
            ),
            button(`type` := "submit", cls := "btn btn-verdant py-2 mt-1")(
              i(cls := "bi bi-box-arrow-in-right me-1"),
              "Masuk Sekarang"
            )
          ),

          hr(cls := "my-3 text-muted"),

          // Quick Login Cepat untuk Demo & Evaluator
          div(cls := "mb-1")(
            div(cls := "d-flex align-items-center justify-content-between mb-2")(
              span(cls := "small fw-semibold text-secondary", "Masuk Cepat (Akun Demo):"),
              span(cls := "badge bg-light text-muted border", "1-Klik")
            ),
            div(cls := "d-flex flex-column gap-2")(
              // Petani 1
              a(href := "/quick-login/usr-petani-01", cls := "quick-btn")(
                div(cls := "d-flex align-items-center gap-2")(
                  i(cls := "bi bi-person-badge text-success fs-5"),
                  div(
                    strong(cls := "d-block", "Pak Joko"),
                    span(cls := "text-muted small", "Petani Lahan Blok 2A · @petani")
                  )
                ),
                span(cls := "role-badge-farmer", "Petani")
              ),
              // Petani 2
              a(href := "/quick-login/usr-petani-02", cls := "quick-btn")(
                div(cls := "d-flex align-items-center gap-2")(
                  i(cls := "bi bi-person-badge text-success fs-5"),
                  div(
                    strong(cls := "d-block", "Pak Slamet"),
                    span(cls := "text-muted small", "Petani Cabai Lahan 1 · @slamet")
                  )
                ),
                span(cls := "role-badge-farmer", "Petani")
              ),
              // Mandor 1
              a(href := "/quick-login/usr-mandor-01", cls := "quick-btn")(
                div(cls := "d-flex align-items-center gap-2")(
                  i(cls := "bi bi-shield-check text-primary fs-5"),
                  div(
                    strong(cls := "d-block", "Ir. Budi Santoso"),
                    span(cls := "text-muted small", "Mandor Kebun Utama · @mandor")
                  )
                ),
                span(cls := "role-badge-supervisor", "Mandor")
              ),
              // Mandor 2
              a(href := "/quick-login/usr-mandor-02", cls := "quick-btn")(
                div(cls := "d-flex align-items-center gap-2")(
                  i(cls := "bi bi-shield-check text-primary fs-5"),
                  div(
                    strong(cls := "d-block", "Hj. Nuraini, S.P."),
                    span(cls := "text-muted small", "Mandor Hortikultura · @nuraini")
                  )
                ),
                span(cls := "role-badge-supervisor", "Mandor")
              )
            )
          ),

          div(cls := "mt-3 text-center")(
            span(cls := "text-muted", style := "font-size: 11.5px;",
              "Sistem Budidaya Pertanian Verdant · Kata sandi bawaan semua akun: ",
              code("123")
            )
          )
        )
      )
    ).render

