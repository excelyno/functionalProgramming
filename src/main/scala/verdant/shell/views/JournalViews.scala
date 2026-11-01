package verdant.shell.views

import scalatags.Text.all.*
import verdant.core.*

/**
 * ============================================================================
 * JOURNAL VIEWS: Jurnal Catatan Harian Kegiatan Budidaya
 * ============================================================================
 * 
 * Modul ini menangani tampilan jurnal riwayat aktivitas operasional pertanian:
 * - catatanView -> Tabel rekapitulasi harian seluruh penyiraman, pemupukan, dan panen
 *                  yang dapat disaring (filter) berdasarkan petak masa tanam tertentu.
 * 
 * ALUR DATA:
 * - Dipanggil dari Main.scala baris 453: @cask.get("/catatan")
 * - Mengambil parameter filter opsional `periodCode` dari URL query string (misal: /catatan?periodCode=MT-2026-TOMAT-01).
 * - Menampilkan modal pencatatan baru jika petani ingin langsung menambah catatan.
 */
trait JournalViews extends ViewHelpers with SharedModals:

  // ==========================================================================
  // JURNAL CATATAN UMUM (FILTERABLE BY PERIOD)
  // Dipanggil dari: Main.scala baris 453 (GET /catatan)
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

      // Bar Filter Masa Tanam
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

      // Tabel Data Catatan Harian
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
                  th(cls := "py-2 px-3 fw-medium", "Tanggal"),
                  th(cls := "py-2 fw-medium", "Masa Tanam"),
                  th(cls := "py-2 fw-medium", "Jenis Kegiatan"),
                  th(cls := "py-2 fw-medium", "Catatan Lapangan"),
                  th(cls := "py-2 fw-medium", "Takaran / Volume"),
                  th(cls := "py-2 px-3 fw-medium", "Pelaksana")
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

      catatKegiatanModal(periods, selectedPeriodCode)
    )

