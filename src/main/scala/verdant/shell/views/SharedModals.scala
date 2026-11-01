package verdant.shell.views

import scalatags.Text.all.*
import verdant.core.*

/**
 * ============================================================================
 * SHARED MODALS: Pop-up Form untuk Petani dan Mandor
 * ============================================================================
 * 
 * Modul ini berisi dialog pop-up yang disederhanakan agar mudah dipahami:
 * 1. catatKegiatanModal -> Petani mencatat aktivitas lapangan yang tersimpan abadi (anti-hapus).
 * 2. bukaMasaTanamModal -> Petani menerapkan Cetakan Budidaya Mandor ke petak lahan tertentu.
 * 3. tambahPanduanModal  -> Mandor menyusun Cetakan Budidaya (SOP Master) baru.
 */
trait SharedModals extends ViewHelpers:

  // ==========================================================================
  // 1. MODAL: CATAT KEGIATAN LAPANGAN (LOG KEKAL ALA GIT)
  // Dipanggil dari: Beranda Petani, Detail Lahan, dan Buku Catatan
  // ==========================================================================
  def catatKegiatanModal(
    periods: List[(CultivationPeriod, String)],
    defaultPeriodCode: Option[String] = None
  ): Modifier =
    div(cls := "modal fade", id := "catatKegiatanModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border-0 shadow rounded-4")(
          form(action := "/catatan/tambah", method := "POST")(
            div(cls := "modal-header border-bottom py-3 px-4 bg-light rounded-top-4")(
              div(
                h5(cls := "modal-title fs-6 fw-bold text-dark mb-0", "Catat Kegiatan Lapangan"),
                span(cls := "badge-git-immutable mt-1",
                  i(cls := "bi bi-lock-fill"), "Tersimpan Abadi (Anti-Hapus ala Git)"
                )
              ),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3 px-4")(
              div(cls := "p-2 mb-3 rounded-3 bg-light border text-muted small",
                i(cls := "bi bi-info-circle me-1 text-primary"),
                "Catatan ini akan langsung masuk ke buku log lahan yang terkunci. Tidak dapat diedit atau dihapus diam-diam untuk menjaga keaslian riwayat panen."
              ),
              // Pilihan Lahan / Masa Tanam
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Pilih Petak Lahan"),
                select(name := "periodCode", cls := "form-select form-select-sm", required := true)(
                  if periods.isEmpty then
                    option(value := "", "Belum ada lahan aktif dengan cetakan")
                  else
                    periods.map { case (p, _) =>
                      val isSelected = defaultPeriodCode.contains(p.code)
                      option(
                        value := p.code,
                        if isSelected then selected := true else modifier(),
                        s"${p.plotName} (${p.cropVariety}) — [${p.code}]"
                      )
                    }
                )
              ),
              // Jenis Kegiatan & Tanggal
              div(cls := "row g-2 mb-3")(
                div(cls := "col-7")(
                  label(cls := "form-label small fw-semibold text-secondary", "Jenis Kegiatan"),
                  select(name := "activityType", cls := "form-select form-select-sm", required := true)(
                    option(value := "Penyiraman", "💧 Penyiraman"),
                    option(value := "Pemupukan", "🌱 Pemupukan Nutrisi"),
                    option(value := "Pengendalian Hama", "🛡️ Pengendalian Hama"),
                    option(value := "Pemangkasan", "✂️ Pemangkasan Daun"),
                    option(value := "Penyiangan", "🌿 Penyiangan Gulma"),
                    option(value := "Panen", "🧺 Panen"),
                    option(value := "Perawatan Lain", "⚙️ Perawatan Lain")
                  )
                ),
                div(cls := "col-5")(
                  label(cls := "form-label small fw-semibold text-secondary", "Tanggal Kegiatan"),
                  input(`type` := "date", name := "activityDate", cls := "form-control form-control-sm", value := todayStr, required := true)
                )
              ),
              // Jumlah / Takaran
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Takaran / Volume / Hasil"),
                input(`type` := "text", name := "quantity", cls := "form-control form-control-sm", placeholder := "Contoh: 200 Liter air, 25 kg NPK, atau 80 kg Panen")
              ),
              // Catatan Lapangan
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Catatan Observasi Lapangan"),
                textarea(name := "notes", cls := "form-control form-control-sm", rows := 3, placeholder := "Misal: Tanaman tampak subur, daun hijau pekat, tanah cukup lembap.", required := true)
              ),
              // Nama Petani
              div(cls := "mb-2")(
                label(cls := "form-label small fw-semibold text-secondary", "Nama Petani Pelaksana"),
                input(`type` := "text", name := "farmerName", cls := "form-control form-control-sm", placeholder := "Contoh: Pak Joko / Bu Siti", required := true)
              )
            ),
            div(cls := "modal-footer border-top py-2 px-4 bg-light rounded-bottom-4")(
              button(`type` := "button", cls := "btn btn-sm btn-light border", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant",
                i(cls := "bi bi-lock me-1"), "Simpan Permanen ke Log Lahan"
              )
            )
          )
        )
      )
    )

  // ==========================================================================
  // 2. MODAL: TERAPKAN CETAKAN MANDOR KE LAHAN (PETANI)
  // Dipanggil dari: Beranda Petani, Lahan Saya, Katalog Cetakan
  // ==========================================================================
  def bukaMasaTanamModal(
    publishedGuides: List[(PublishedGuide, String)],
    defaultGuideId: Option[String] = None
  ): Modifier =
    div(cls := "modal fade", id := "bukaMasaTanamModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border-0 shadow rounded-4")(
          form(action := "/masa-tanam/buka", method := "POST")(
            div(cls := "modal-header border-bottom py-3 px-4 bg-light rounded-top-4")(
              div(
                h5(cls := "modal-title fs-6 fw-bold text-dark mb-0", "Terapkan Cetakan Mandor ke Lahan"),
                span(cls := "text-muted small", "Kunci standar budidaya resmi pada petak lahan Anda")
              ),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3 px-4")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Pilih Cetakan Budidaya Resmi dari Mandor"),
                select(name := "guideSelection", cls := "form-select form-select-sm", required := true)(
                  if publishedGuides.isEmpty then
                    option(value := "", "Belum ada cetakan resmi dari mandor")
                  else
                    publishedGuides.map { case (pub, guideTitle) =>
                      val isSelected = defaultGuideId.contains(pub.guideId)
                      option(
                        value := s"${pub.guideId}|${pub.editionTag}",
                        if isSelected then selected := true else modifier(),
                        s"$guideTitle — [${pub.editionTag}]"
                      )
                    }
                ),
                div(cls := "form-text small", "🔒 Cetakan ini akan terkunci permanen untuk lahan ini sehingga takaran tetap konsisten.")
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Nama Lahan / Petak / Greenhouse"),
                input(`type` := "text", name := "plotName", cls := "form-control form-control-sm", placeholder := "Contoh: Greenhouse Blok 2A atau Petak Timur", required := true)
              ),
              div(cls := "mb-2")(
                label(cls := "form-label small fw-semibold text-secondary", "Kode Unik Lahan / Masa Tanam"),
                input(`type` := "text", name := "code", cls := "form-control form-control-sm", placeholder := "Contoh: MT-2026-TOMAT-02", required := true),
                div(cls := "form-text small", "Kode ini menjadi identitas buku catatan budidaya Anda.")
              )
            ),
            div(cls := "modal-footer border-top py-2 px-4 bg-light rounded-bottom-4")(
              button(`type` := "button", cls := "btn btn-sm btn-light border", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Mulai Tanam dengan Cetakan Ini")
            )
          )
        )
      )
    )

  // ==========================================================================
  // 3. MODAL: BUAT CETAKAN BUDIDAYA BARU (MANDOR)
  // Dipanggil dari: Beranda Mandor, Kelola Cetakan
  // ==========================================================================
  def tambahPanduanModal(): Modifier =
    div(cls := "modal fade", id := "tambahPanduanModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border-0 shadow rounded-4")(
          form(action := "/panduan/tambah", method := "POST")(
            div(cls := "modal-header border-bottom py-3 px-4 bg-light rounded-top-4")(
              div(
                h5(cls := "modal-title fs-6 fw-bold text-dark mb-0", "Buat Cetakan Budidaya Baru (SOP Master)"),
                span(cls := "text-muted small", "Mandor menyusun standar panduan yang dapat dipakai petani")
              ),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3 px-4")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "ID Unik Cetakan"),
                input(`type` := "text", name := "id", cls := "form-control form-control-sm", placeholder := "Contoh: cetakan-jagung-manis", required := true)
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Nama Lengkap Cetakan SOP"),
                input(`type` := "text", name := "name", cls := "form-control form-control-sm", placeholder := "Contoh: SOP Budidaya Jagung Manis Hibrida", required := true)
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-semibold text-secondary", "Komoditas & Varietas Tanaman"),
                input(`type` := "text", name := "cropVariety", cls := "form-control form-control-sm", placeholder := "Contoh: Jagung Manis Talenta", required := true)
              ),
              div(cls := "mb-2")(
                label(cls := "form-label small fw-semibold text-secondary", "Nama Mandor / Penyusun"),
                input(`type` := "text", name := "author", cls := "form-control form-control-sm", placeholder := "Contoh: Ir. Budi Santoso", required := true)
              )
            ),
            div(cls := "modal-footer border-top py-2 px-4 bg-light rounded-bottom-4")(
              button(`type` := "button", cls := "btn btn-sm btn-light border", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant", "Buat Draf Cetakan Awal")
            )
          )
        )
      )
    )
