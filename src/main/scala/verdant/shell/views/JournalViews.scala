package verdant.shell.views

import scalatags.Text.all.*
import verdant.core.*

/**
 * ============================================================================
 * JOURNAL VIEWS: Buku Catatan Lapangan Petani (Log Kekal / Anti-Hapus ala Git)
 * ============================================================================
 * 
 * Filosofi Inti:
 * Catatan kegiatan petani di Verdant bersifat KEKAL (Append-Only Ledger).
 * - Sekali disimpan, catatan langsung memiliki kode verifikasi unik (seperti commit hash Git).
 * - Tidak ada tombol 'Hapus' atau manipulasi diam-diam.
 * - Ini memberi jaminan keaslian data kepada pembeli/auditor bahwa tanaman dirawat
 *   sesuai standar cetakan mandor.
 */
trait JournalViews extends ViewHelpers with SharedModals:

  // ==========================================================================
  // JURNAL CATATAN UMUM (FILTERABLE BY PERIOD)
  // ==========================================================================
  def catatanView(
    activities: List[CultivationActivity],
    periods: List[(CultivationPeriod, String)],
    selectedPeriodCode: Option[String] = None
  ): Modifier =
    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-3 pb-3 mb-4 border-bottom")(
        div(
          div(cls := "d-flex align-items-center gap-2 mb-1")(
            span(cls := "badge-git-immutable", i(cls := "bi bi-lock-fill"), "Log Kekal (Anti-Hapus)"),
            span(cls := "small text-muted", "Integritas Riwayat Budidaya")
          ),
          h1(cls := "h4 fw-bold text-dark mb-1", "Buku Catatan Lapangan Petani"),
          p(cls := "text-muted small mb-0", "Jurnal harian seluruh penyiraman, pemupukan, dan panen. Tersimpan permanen seperti commit Git.")
        ),
        button(
          cls := "btn btn-verdant",
          data("bs-toggle") := "modal",
          data("bs-target") := "#catatKegiatanModal"
        )(i(cls := "bi bi-journal-plus me-1"), "Tambah Catatan Lapangan")
      ),

      // Bar Filter Petak Lahan
      div(cls := "p-3 mb-4 bg-white border rounded-4 shadow-sm")(
        form(action := "/catatan", method := "GET", cls := "row g-2 align-items-center")(
          div(cls := "col-auto")(
            label(cls := "small fw-semibold text-secondary", "Pilih Petak Lahan:")
          ),
          div(cls := "col-auto")(
            select(name := "periodCode", cls := "form-select form-select-sm")(
              option(value := "", "Semua Petak Lahan"),
              periods.map { case (p, _) =>
                option(
                  value := p.code,
                  if selectedPeriodCode.contains(p.code) then selected := true else modifier(),
                  s"${p.plotName} (${p.cropVariety}) — [${p.code}]"
                )
              }
            )
          ),
          div(cls := "col-auto")(
            button(`type` := "submit", cls := "btn btn-sm btn-verdant-outline", "Saring Data")
          ),
          div(cls := "col text-end small text-muted")(
            s"Total riwayat tercatat: ${activities.length} aktivitas"
          )
        )
      ),

      // Tabel Data Catatan Harian (Kekal)
      if activities.isEmpty then
        div(cls := "p-5 text-center text-muted border rounded-4 bg-white small shadow-sm")(
          i(cls := "bi bi-journal-x fs-2 d-block mb-2 text-muted"),
          p(cls := "mb-2", "Belum ada catatan kegiatan yang dimasukkan."),
          button(
            cls := "btn btn-sm btn-verdant",
            data("bs-toggle") := "modal",
            data("bs-target") := "#catatKegiatanModal"
          )("Catat Kegiatan Pertama")
        )
      else
        div(cls := "border rounded-4 overflow-hidden bg-white shadow-sm")(
          div(cls := "table-responsive")(
            table(cls := "table table-hover align-middle mb-0 small")(
              thead(cls := "table-light text-muted")(
                tr(
                  th(cls := "py-3 px-3 fw-semibold", "Tanggal"),
                  th(cls := "py-3 fw-semibold", "Petak Lahan"),
                  th(cls := "py-3 fw-semibold", "Jenis Kegiatan"),
                  th(cls := "py-3 fw-semibold", "Catatan Lapangan"),
                  th(cls := "py-3 fw-semibold", "Takaran / Hasil"),
                  th(cls := "py-3 fw-semibold", "Petani Pelaksana"),
                  th(cls := "py-3 px-3 fw-semibold text-end", "Status Integritas")
                )
              ),
              tbody(
                activities.map { act =>
                  tr(
                    td(cls := "py-3 px-3 text-nowrap font-monospace text-muted", act.activityDate),
                    td(
                      a(cls := "text-decoration-none text-dark fw-bold", href := s"/masa-tanam/${act.periodCode}", act.periodCode)
                    ),
                    td(span(cls := "fw-bold text-dark", act.activityType)),
                    td(span(cls := "text-secondary", act.notes)),
                    td(act.quantity.map(q => span(cls := "badge bg-light text-dark border", q)).getOrElse(span(cls := "text-muted", "—"))),
                    td(cls := "text-muted", act.farmerName),
                    td(cls := "px-3 text-end")(
                      span(cls := "badge-git-immutable",
                        i(cls := "bi bi-lock-fill"), s"#${act.id.take(8)}"
                      )
                    )
                  )
                }
              )
            )
          ),
          div(cls := "p-3 bg-light border-top text-muted small d-flex justify-content-between align-items-center flex-wrap gap-2")(
            span(
              i(cls := "bi bi-shield-check text-success me-1"),
              "Prinsip Kekal: Catatan tidak dapat dihapus untuk menjaga keaslian jejak panen."
            ),
            span(cls := "text-muted font-monospace", style := "font-size: 11px;", "Immutable Ledger Mode: ON")
          )
        )
      ,

      catatKegiatanModal(periods, selectedPeriodCode)
    )
