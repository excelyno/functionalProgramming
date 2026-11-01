package verdant.shell.views

import scalatags.Text.all.*
import verdant.core.*

/**
 * Buku catatan lapangan petani.
 *
 * Catatan bersifat append-only: setelah disimpan tidak bisa diubah atau dihapus,
 * dan tiap catatan punya ID unik sebagai rujukan.
 */
trait JournalViews extends ViewHelpers with SharedModals:

  // ==========================================================================
  // JURNAL CATATAN (BISA DISARING PER PETAK)
  // ==========================================================================
  def catatanView(
    activities: List[CultivationActivity],
    periods: List[(CultivationPeriod, String)],
    selectedPeriodCode: Option[String] = None,
    currentUser: Option[User] = None
  ): Modifier =
    val addButton: Modifier =
      button(
        `type` := "button",
        cls := "btn btn-verdant",
        data("bs-toggle") := "modal",
        data("bs-target") := "#catatKegiatanModal"
      )("Catat kegiatan")

    div(
      div(cls := "d-flex flex-column flex-md-row justify-content-between align-items-md-end gap-3 pb-3 mb-4 border-bottom")(
        div(
          h1(cls := "h4 fw-semibold text-dark mb-1", "Catatan lapangan"),
          p(cls := "text-muted small mb-0",
            "Riwayat penyiraman, pemupukan, panen, dan kegiatan lain. Catatan tidak bisa diubah atau dihapus."
          )
        ),
        addButton
      ),

      // Saring per petak
      form(action := "/catatan", method := "GET", cls := "d-flex flex-wrap align-items-center gap-2 mb-3")(
        label(cls := "small fw-medium text-secondary", `for` := "filter-period", "Petak"),
        select(name := "periodCode", id := "filter-period", cls := "form-select form-select-sm w-auto")(
          option(value := "", "Semua petak"),
          periods.map { case (p, _) =>
            option(
              value := p.code,
              if selectedPeriodCode.contains(p.code) then selected := true else modifier(),
              s"${p.plotName} · ${p.cropVariety} (${p.code})"
            )
          }
        ),
        button(`type` := "submit", cls := "btn btn-sm btn-verdant-outline", "Saring"),
        span(cls := "small text-muted ms-auto", s"${activities.length} catatan")
      ),

      if activities.isEmpty then
        div(cls := "border rounded-2 bg-white text-center text-muted small p-4")(
          p(cls := "mb-3", "Belum ada catatan kegiatan."),
          button(
            `type` := "button",
            cls := "btn btn-sm btn-verdant",
            data("bs-toggle") := "modal",
            data("bs-target") := "#catatKegiatanModal"
          )("Catat kegiatan pertama")
        )
      else
        div(cls := "border rounded-2 bg-white overflow-hidden")(
          div(cls := "table-responsive")(
            table(cls := "table table-hover align-middle mb-0 small")(
              thead(cls := "table-light")(
                tr(
                  th(cls := "py-2 px-3 fw-medium", "Tanggal"),
                  th(cls := "py-2 fw-medium", "Petak"),
                  th(cls := "py-2 fw-medium", "Kegiatan"),
                  th(cls := "py-2 fw-medium", "Catatan"),
                  th(cls := "py-2 fw-medium", "Takaran / hasil"),
                  th(cls := "py-2 fw-medium", "Petani"),
                  th(cls := "py-2 px-3 fw-medium text-end", "ID")
                )
              ),
              tbody(
                activities.map { act =>
                  tr(
                    td(cls := "py-2 px-3 text-nowrap font-monospace text-muted", act.activityDate),
                    td(cls := "py-2")(
                      a(cls := "text-decoration-none text-dark", href := s"/masa-tanam/${act.periodCode}", act.periodCode)
                    ),
                    td(cls := "py-2 fw-medium text-dark", act.activityType),
                    td(cls := "py-2 text-secondary", act.notes),
                    td(cls := "py-2", act.quantity.map(q => span(q)).getOrElse(span(cls := "text-muted", "—"))),
                    td(cls := "py-2 text-muted", act.farmerName),
                    td(cls := "py-2 px-3 text-end font-monospace text-muted", s"#${act.id.take(8)}")
                  )
                }
              )
            )
          )
        )
      ,

      catatKegiatanModal(periods, selectedPeriodCode, currentUser)
    )