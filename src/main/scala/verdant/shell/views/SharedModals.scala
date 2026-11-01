package verdant.shell.views

import scalatags.Text.all.*
import verdant.core.*

/**
 * ============================================================================
 * SHARED MODALS: Dialog Pop-up Form untuk Petani dan Mandor
 * ============================================================================
 * 
 * Modul ini berisi modal pop-up (Bootstrap Modal) yang digunakan di berbagai halaman:
 * 1. catatKegiatanModal -> Pop-up form bagi petani untuk mencatat kegiatan di lahan (penyiraman, pemupukan, dll).
 * 2. bukaMasaTanamModal -> Pop-up form bagi petani untuk mengikat lahan ke panduan resmi (buka siklus tanam baru).
 * 3. tambahPanduanModal  -> Pop-up form bagi mandor untuk mendaftarkan draft SOP komoditas baru.
 * 
 * KONSEP SCALA & SCALATAGS:
 * - `Modifier`: Tipe data Scalatags yang mewakili elemen HTML, atribut, teks, atau pecahan DOM.
 * - `form(action := "...", method := "POST")`: Sintaks Scalatags untuk <form action="..." method="POST">.
 * - `option(value := ..., if isSelected then selected := true else modifier(), ...)`:
 *   Cara kondisional menambahkan atribut HTML di Scalatags. `modifier()` berarti "tidak menambahkan apa-apa".
 */
trait SharedModals extends ViewHelpers:

  // ==========================================================================
  // 1. MODAL: CATAT KEGIATAN BUDIDAYA (PETANI)
  // Dipanggil dari: Beranda Petani, Detail Masa Tanam, dan Jurnal Catatan
  // Form mengarah ke: POST /catatan/tambah (Main.scala baris 458)
  // ==========================================================================
  def catatKegiatanModal(
    periods: List[(CultivationPeriod, String)],
    defaultPeriodCode: Option[String] = None
  ): Modifier =
    div(cls := "modal fade", id := "catatKegiatanModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border rounded-3")(
          form(action := "/catatan/tambah", method := "POST")(
            div(cls := "modal-header border-bottom py-3")(
              h4(cls := "modal-title fs-6 fw-bold text-dark", "Catat Kegiatan Budidaya"),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3")(
              // Pilihan Masa Tanam
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Pilih Masa Tanam / Lahan"),
                select(name := "periodCode", cls := "form-select form-select-sm", required := true)(
                  if periods.isEmpty then
                    option(value := "", "Belum ada masa tanam aktif")
                  else
                    periods.map { case (p, _) =>
                      val isSelected = defaultPeriodCode.contains(p.code)
                      option(
                        value := p.code,
                        if isSelected then selected := true else modifier(),
                        s"${p.code} — ${p.cropVariety} (${p.plotName})"
                      )
                    }
                )
              ),
              // Jenis Kegiatan & Tanggal
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
              // Dosis / Takaran
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Jumlah / Takaran"),
                input(`type` := "text", name := "quantity", cls := "form-control form-control-sm", placeholder := "Contoh: 30 kg NPK, 200 Liter, 150 kg Panen")
              ),
              // Catatan
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Catatan Lapangan"),
                textarea(name := "notes", cls := "form-control form-control-sm", rows := 3, placeholder := "Contoh: Penyiraman pagi berjalan normal, kondisi daun segar.", required := true)
              ),
              // Nama Petani
              div(cls := "mb-2")(
                label(cls := "form-label small fw-medium text-muted", "Nama Petani / Pelaksana"),
                input(`type` := "text", name := "farmerName", cls := "form-control form-control-sm", placeholder := "Contoh: Pak Joko", required := true)
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
  // 2. MODAL: BUKA MASA TANAM BARU (PETANI)
  // Dipanggil dari: Beranda Petani, Daftar Masa Tanam, Katalog Panduan
  // Form mengarah ke: POST /masa-tanam/buka (Main.scala baris 130)
  // ==========================================================================
  def bukaMasaTanamModal(
    publishedGuides: List[(PublishedGuide, String)],
    defaultGuideId: Option[String] = None
  ): Modifier =
    div(cls := "modal fade", id := "bukaMasaTanamModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border rounded-3")(
          form(action := "/masa-tanam/buka", method := "POST")(
            div(cls := "modal-header border-bottom py-3")(
              h5(cls := "modal-title fs-6 fw-bold text-dark", "Mulai Masa Tanam Baru"),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Pilih Panduan Budidaya Resmi yang Berlaku"),
                select(name := "guideSelection", cls := "form-select form-select-sm", required := true)(
                  if publishedGuides.isEmpty then
                    option(value := "", "Belum ada panduan resmi diterbitkan")
                  else
                    publishedGuides.map { case (pub, guideTitle) =>
                      val isSelected = defaultGuideId.contains(pub.guideId)
                      option(
                        value := s"${pub.guideId}|${pub.editionTag}",
                        if isSelected then selected := true else modifier(),
                        s"$guideTitle (${pub.editionTag})"
                      )
                    }
                ),
                div(cls := "form-text small", "Masa tanam akan mengunci panduan resmi ini sebagai acuan tetap budidaya.")
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Nama Lahan / Petak / Greenhouse"),
                input(`type` := "text", name := "plotName", cls := "form-control form-control-sm", placeholder := "Contoh: Greenhouse Blok 2 atau Lahan Petak Timur", required := true)
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Kode Masa Tanam (Unik)"),
                input(`type` := "text", name := "code", cls := "form-control form-control-sm", placeholder := "Contoh: MT-2026-TOMAT-02", required := true)
              )
            ),
            div(cls := "modal-footer border-top py-2")(
              button(`type` := "button", cls := "btn btn-sm btn-light border", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Mulai Masa Tanam")
            )
          )
        )
      )
    )

  // ==========================================================================
  // 3. MODAL: TAMBAH PANDUAN BARU (MANDOR)
  // Dipanggil dari: Beranda Mandor, Kelola Panduan
  // Form mengarah ke: POST /panduan/tambah (Main.scala baris 236)
  // ==========================================================================
  def tambahPanduanModal(): Modifier =
    div(cls := "modal fade", id := "tambahPanduanModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border rounded-3")(
          form(action := "/panduan/tambah", method := "POST")(
            div(cls := "modal-header border-bottom py-3")(
              h5(cls := "modal-title fs-6 fw-bold text-dark", "Buat Panduan Budidaya Baru"),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-medium text-muted", "Kode ID Panduan"),
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
                label(cls := "form-label small fw-medium text-muted", "Nama Penyusun / Mandor"),
                input(`type` := "text", name := "author", cls := "form-control form-control-sm", placeholder := "Contoh: Ir. Budi Santoso", required := true)
              )
            ),
            div(cls := "modal-footer border-top py-2")(
              button(`type` := "button", cls := "btn btn-sm btn-light border", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Buat Draf Panduan Awal")
            )
          )
        )
      )
    )

