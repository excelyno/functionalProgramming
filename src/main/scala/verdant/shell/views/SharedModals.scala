package verdant.shell.views

import scalatags.Text.all.*
import verdant.core.*

/**
 * ============================================================================
 * SHARED MODALS: Pop-up Form Praktis untuk Petani dan Mandor
 * ============================================================================
 * 
 * Modul ini dirancang agar pengisian data oleh petani SANGAT MUDAH & CEPAT:
 * - Pilihan kegiatan jelas: Penyiraman, Ngasi Pupuk, Ngasi Obat/Hama, Pemangkasan, Panen.
 * - Tombol-tombol pilihan takaran cepat (Tinggal klik: 100L, 200L, 10kg, 25kg, 50kg).
 * - Tombol-tombol catatan cepat (Tinggal klik: "Tanaman subur", "Tanah cukup lembap").
 * - Petani di lapangan tidak perlu mengetik panjang-panjang di HP.
 */
trait SharedModals extends ViewHelpers:

  // ==========================================================================
  // 1. MODAL: CATAT KEGIATAN LAPANGAN (LOG KEKAL ALA GIT)
  // ==========================================================================
  def catatKegiatanModal(
    periods: List[(CultivationPeriod, String)],
    defaultPeriodCode: Option[String] = None
  ): Modifier =
    div(cls := "modal fade", id := "catatKegiatanModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered modal-lg")(
        div(cls := "modal-content border-0 shadow rounded-4 overflow-hidden")(
          form(action := "/catatan/tambah", method := "POST")(
            // Header Modal
            div(cls := "modal-header border-bottom py-3 px-4 bg-light")(
              div(
                h5(cls := "modal-title fs-6 fw-bold text-dark mb-1 d-flex align-items-center gap-2",
                  i(cls := "bi bi-journal-plus text-success"),
                  span("Catat Kegiatan Lapangan")
                ),
                span(cls := "badge-git-immutable",
                  i(cls := "bi bi-lock-fill"), "Log Kekal (Anti-Hapus ala Git) · Tercatat Permanen"
                )
              ),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),

            // Body Modal
            div(cls := "modal-body py-3 px-4")(
              div(cls := "p-3 mb-3 rounded-3 bg-light border text-muted small d-flex align-items-start gap-2",
                i(cls := "bi bi-shield-check text-success fs-5 mt-n1"),
                div(
                  strong(cls := "text-dark d-block mb-1", "Buku Log Lapangan Tanpa Hapus"),
                  "Tiap kegiatan yang Anda simpan akan memiliki kode verifikasi paten. Data tidak bisa dihapus diam-diam demi menjaga keaslian jejak panen."
                )
              ),

              // 1. PILIH PETAK LAHAN
              div(cls := "mb-3")(
                label(cls := "form-label small fw-bold text-dark", "1. Pilih Petak Lahan"),
                select(name := "periodCode", cls := "form-select form-select-sm py-2", required := true)(
                  if periods.isEmpty then
                    option(value := "", "Belum ada lahan aktif dengan cetakan")
                  else
                    periods.map { case (p, _) =>
                      val isSelected = defaultPeriodCode.contains(p.code)
                      option(
                        value := p.code,
                        if isSelected then selected := true else modifier(),
                        s"📍 ${p.plotName} — Komoditas: ${p.cropVariety} [Kode: ${p.code}]"
                      )
                    }
                )
              ),

              // 2. PILIHAN KEGIATAN (PENYIRAMAN KAH, NGASI PUPUK KAH, DLL)
              div(cls := "mb-3")(
                label(cls := "form-label small fw-bold text-dark d-block", "2. Jenis Kegiatan yang Dilakukan"),
                div(cls := "row g-2 mb-2")(
                  div(cls := "col-6 col-md-4")(
                    input(`type` := "radio", cls := "btn-check", name := "activityTypeRadio", id := "actPenyiraman", value := "Penyiraman", checked := true,
                      onchange := "document.getElementById('inputActivityType').value = 'Penyiraman'; document.getElementById('inputQuantity').placeholder = 'Contoh: 200 Liter Air'; document.getElementById('qtyChipsPenyiraman').style.display='flex'; document.getElementById('qtyChipsPupuk').style.display='none'; document.getElementById('qtyChipsHama').style.display='none'; document.getElementById('qtyChipsPanen').style.display='none';"
                    ),
                    label(cls := "btn btn-outline-success w-100 py-2 text-start small d-flex align-items-center gap-2", `for` := "actPenyiraman",
                      span(cls := "fs-5", "💧"),
                      div(
                        strong(cls := "d-block", "Penyiraman"),
                        span(cls := "text-muted", style := "font-size: 11px;", "Siram air / nutrisi")
                      )
                    )
                  ),
                  div(cls := "col-6 col-md-4")(
                    input(`type` := "radio", cls := "btn-check", name := "activityTypeRadio", id := "actPemupukan", value := "Pemupukan",
                      onchange := "document.getElementById('inputActivityType').value = 'Pemupukan'; document.getElementById('inputQuantity').placeholder = 'Contoh: 25 kg Pupuk NPK'; document.getElementById('qtyChipsPenyiraman').style.display='none'; document.getElementById('qtyChipsPupuk').style.display='flex'; document.getElementById('qtyChipsHama').style.display='none'; document.getElementById('qtyChipsPanen').style.display='none';"
                    ),
                    label(cls := "btn btn-outline-success w-100 py-2 text-start small d-flex align-items-center gap-2", `for` := "actPemupukan",
                      span(cls := "fs-5", "🌱"),
                      div(
                        strong(cls := "d-block", "Ngasi Pupuk"),
                        span(cls := "text-muted", style := "font-size: 11px;", "Pupuk NPK / Kompos")
                      )
                    )
                  ),
                  div(cls := "col-6 col-md-4")(
                    input(`type` := "radio", cls := "btn-check", name := "activityTypeRadio", id := "actHama", value := "Pengendalian Hama",
                      onchange := "document.getElementById('inputActivityType').value = 'Pengendalian Hama'; document.getElementById('inputQuantity').placeholder = 'Contoh: 1 Tangki Semprot'; document.getElementById('qtyChipsPenyiraman').style.display='none'; document.getElementById('qtyChipsPupuk').style.display='none'; document.getElementById('qtyChipsHama').style.display='flex'; document.getElementById('qtyChipsPanen').style.display='none';"
                    ),
                    label(cls := "btn btn-outline-success w-100 py-2 text-start small d-flex align-items-center gap-2", `for` := "actHama",
                      span(cls := "fs-5", "🛡️"),
                      div(
                        strong(cls := "d-block", "Ngasi Obat Hama"),
                        span(cls := "text-muted", style := "font-size: 11px;", "Semprot nabati/hama")
                      )
                    )
                  ),
                  div(cls := "col-6 col-md-4")(
                    input(`type` := "radio", cls := "btn-check", name := "activityTypeRadio", id := "actPemangkasan", value := "Pemangkasan",
                      onchange := "document.getElementById('inputActivityType').value = 'Pemangkasan'; document.getElementById('inputQuantity').placeholder = 'Contoh: Pemangkasan 50 pohon'; document.getElementById('qtyChipsPenyiraman').style.display='none'; document.getElementById('qtyChipsPupuk').style.display='none'; document.getElementById('qtyChipsHama').style.display='none'; document.getElementById('qtyChipsPanen').style.display='none';"
                    ),
                    label(cls := "btn btn-outline-success w-100 py-2 text-start small d-flex align-items-center gap-2", `for` := "actPemangkasan",
                      span(cls := "fs-5", "✂️"),
                      div(
                        strong(cls := "d-block", "Pemangkasan"),
                        span(cls := "text-muted", style := "font-size: 11px;", "Tunas air / daun tua")
                      )
                    )
                  ),
                  div(cls := "col-6 col-md-4")(
                    input(`type` := "radio", cls := "btn-check", name := "activityTypeRadio", id := "actPanen", value := "Panen",
                      onchange := "document.getElementById('inputActivityType').value = 'Panen'; document.getElementById('inputQuantity').placeholder = 'Contoh: 50 kg Cabai Segar'; document.getElementById('qtyChipsPenyiraman').style.display='none'; document.getElementById('qtyChipsPupuk').style.display='none'; document.getElementById('qtyChipsHama').style.display='none'; document.getElementById('qtyChipsPanen').style.display='flex';"
                    ),
                    label(cls := "btn btn-outline-success w-100 py-2 text-start small d-flex align-items-center gap-2", `for` := "actPanen",
                      span(cls := "fs-5", "🧺"),
                      div(
                        strong(cls := "d-block", "Panen"),
                        span(cls := "text-muted", style := "font-size: 11px;", "Petik hasil panen")
                      )
                    )
                  ),
                  div(cls := "col-6 col-md-4")(
                    input(`type` := "radio", cls := "btn-check", name := "activityTypeRadio", id := "actPerawatan", value := "Penyiangan",
                      onchange := "document.getElementById('inputActivityType').value = 'Penyiangan'; document.getElementById('inputQuantity').placeholder = 'Contoh: Bersih gulma 2 bedeng'; document.getElementById('qtyChipsPenyiraman').style.display='none'; document.getElementById('qtyChipsPupuk').style.display='none'; document.getElementById('qtyChipsHama').style.display='none'; document.getElementById('qtyChipsPanen').style.display='none';"
                    ),
                    label(cls := "btn btn-outline-success w-100 py-2 text-start small d-flex align-items-center gap-2", `for` := "actPerawatan",
                      span(cls := "fs-5", "🌿"),
                      div(
                        strong(cls := "d-block", "Bersih Gulma"),
                        span(cls := "text-muted", style := "font-size: 11px;", "Cabut rumput liar")
                      )
                    )
                  )
                ),
                // Input hidden untuk menampung jenis kegiatan yang terpilih
                input(`type` := "hidden", id := "inputActivityType", name := "activityType", value := "Penyiraman")
              ),

              // 3. TANGGAL & TAKARAN / DOSIS DENGAN CHIP PILIHAN CEPAT
              div(cls := "row g-3 mb-3")(
                div(cls := "col-md-5")(
                  label(cls := "form-label small fw-bold text-dark", "3. Tanggal"),
                  input(`type` := "date", name := "activityDate", cls := "form-control form-control-sm py-2", value := todayStr, required := true)
                ),
                div(cls := "col-md-7")(
                  label(cls := "form-label small fw-bold text-dark", "4. Takaran / Jumlah / Hasil"),
                  input(`type` := "text", id := "inputQuantity", name := "quantity", cls := "form-control form-control-sm py-2 mb-2", placeholder := "Contoh: 200 Liter Air"),
                  // Preset Takaran Cepat (Tinggal Klik)
                  div(cls := "d-flex gap-1 flex-wrap align-items-center", id := "qtyChipsPenyiraman")(
                    span(cls := "text-muted small me-1", style := "font-size: 11px;", "Pilihan Cepat:"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='100 Liter';", "💧 100 Liter"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='200 Liter';", "💧 200 Liter"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='Siram Pagi 1 Drum';", "💧 1 Drum Pagi")
                  ),
                  div(cls := "gap-1 flex-wrap align-items-center", id := "qtyChipsPupuk", style := "display:none;")(
                    span(cls := "text-muted small me-1", style := "font-size: 11px;", "Pilihan Cepat:"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='10 kg Pupuk NPK';", "🌱 10 kg NPK"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='25 kg Pupuk NPK';", "🌱 25 kg NPK"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='50 kg Pupuk Kompos';", "🌱 50 kg Kompos")
                  ),
                  div(cls := "gap-1 flex-wrap align-items-center", id := "qtyChipsHama", style := "display:none;")(
                    span(cls := "text-muted small me-1", style := "font-size: 11px;", "Pilihan Cepat:"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='1 Tangki Semprot Nabati';", "🛡️ 1 Tangki Semprot"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='5 Lembar Perangkap Kuning';", "🛡️ 5 Lembar Trap")
                  ),
                  div(cls := "gap-1 flex-wrap align-items-center", id := "qtyChipsPanen", style := "display:none;")(
                    span(cls := "text-muted small me-1", style := "font-size: 11px;", "Pilihan Cepat:"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='15 kg Cabai';", "🧺 15 kg"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='50 kg Cabai';", "🧺 50 kg"),
                    button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputQuantity').value='100 kg Panen Raya';", "🧺 100 kg")
                  )
                )
              ),

              // 4. CATATAN LAPANGAN DENGAN PILIHAN KONDISI CEPAT
              div(cls := "mb-3")(
                label(cls := "form-label small fw-bold text-dark", "5. Catatan Kondisi Lapangan"),
                textarea(name := "notes", id := "inputNotes", cls := "form-control form-control-sm mb-2", rows := 2, placeholder := "Pilih kondisi cepat di bawah atau ketik catatan sendiri...", required := true,
                  "Penyiraman pagi hari selesai, tanah lembap dan tanaman segar."
                ),
                div(cls := "d-flex gap-1 flex-wrap align-items-center")(
                  span(cls := "text-muted small me-1", style := "font-size: 11px;", "Klik Cepat:"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputNotes').value='Tanaman tumbuh subur, daun hijau segar, tanah cukup lembap.';", "🌿 Tanaman Subur Segar"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputNotes').value='Cuaca panas terik, tanah agak kering, telah disiram tuntas.';", "☀️ Cuaca Panas"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputNotes').value='Ada sedikit ulat daun pada 3 pohon, langsung disemprot obat nabati.';", "🐛 Kutu/Hama Ditangani"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputNotes').value='Cabai matang merah merata siap petik pagi besok.';", "🌶️ Buah Matang Siap Petik")
                )
              ),

              // 5. NAMA PETANI DENGAN PILIHAN CEPAT
              div(cls := "mb-2")(
                label(cls := "form-label small fw-bold text-dark", "6. Nama Petani Pelaksana"),
                input(`type` := "text", id := "inputFarmerName", name := "farmerName", cls := "form-control form-control-sm py-2 mb-2", value := "Pak Joko", required := true),
                div(cls := "d-flex gap-1 align-items-center")(
                  span(cls := "text-muted small me-1", style := "font-size: 11px;", "Pilih Nama:"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputFarmerName').value='Pak Joko';", "Pak Joko"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputFarmerName').value='Pak Slamet';", "Pak Slamet"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputFarmerName').value='Bu Siti';", "Bu Siti")
                )
              )
            ),

            // Footer Modal
            div(cls := "modal-footer border-top py-3 px-4 bg-light")(
              button(`type` := "button", cls := "btn btn-sm btn-light border px-3", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant px-4 py-2",
                i(cls := "bi bi-lock-fill me-1"), "Simpan Permanen ke Log Lahan (Anti-Hapus)"
              )
            )
          )
        )
      )
    )

  // ==========================================================================
  // 2. MODAL: TERAPKAN CETAKAN MANDOR KE LAHAN (PETANI)
  // ==========================================================================
  def bukaMasaTanamModal(
    publishedGuides: List[(PublishedGuide, String)],
    defaultGuideId: Option[String] = None
  ): Modifier =
    div(cls := "modal fade", id := "bukaMasaTanamModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border-0 shadow rounded-4 overflow-hidden")(
          form(action := "/masa-tanam/buka", method := "POST")(
            div(cls := "modal-header border-bottom py-3 px-4 bg-light")(
              div(
                h5(cls := "modal-title fs-6 fw-bold text-dark mb-0", "Terapkan Cetakan Mandor ke Lahan"),
                span(cls := "text-muted small", "Pilih cetakan tanaman untuk petak kebun Anda")
              ),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3 px-4")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-bold text-dark", "1. Pilih Cetakan Budidaya dari Mandor"),
                select(name := "guideSelection", cls := "form-select form-select-sm py-2", required := true)(
                  if publishedGuides.isEmpty then
                    option(value := "", "Belum ada cetakan resmi dari mandor")
                  else
                    publishedGuides.map { case (pub, guideTitle) =>
                      val isSelected = defaultGuideId.contains(pub.guideId)
                      option(
                        value := s"${pub.guideId}|${pub.editionTag}",
                        if isSelected then selected := true else modifier(),
                        s"🌱 $guideTitle — [${pub.editionTag}]"
                      )
                    }
                ),
                div(cls := "form-text small", "🔒 Cetakan ini akan menjadi acuan tetap takaran pupuk dan air di lahan Anda.")
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-bold text-dark", "2. Nama Petak Lahan"),
                input(`type` := "text", id := "inputPlotName", name := "plotName", cls := "form-control form-control-sm py-2 mb-2", placeholder := "Misal: Lahan 1 (Cabai) atau Petak Barat", value := "Lahan 1 (Cabai Rawit)", required := true),
                div(cls := "d-flex gap-1 flex-wrap align-items-center")(
                  span(cls := "text-muted small me-1", style := "font-size: 11px;", "Pilihan Nama Cepat:"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputPlotName').value='Lahan 1 (Cabai Rawit)'; document.getElementById('inputPlotCode').value='Lahan-1-Cabai';", "Lahan 1 (Cabai)"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputPlotName').value='Lahan 2 (Tomat)'; document.getElementById('inputPlotCode').value='Lahan-2-Tomat';", "Lahan 2 (Tomat)"),
                  button(`type` := "button", cls := "btn btn-sm btn-light border py-0 px-2 small", onclick := "document.getElementById('inputPlotName').value='Greenhouse Blok 2A'; document.getElementById('inputPlotCode').value='Greenhouse-2A';", "Greenhouse 2A")
                )
              ),
              div(cls := "mb-2")(
                label(cls := "form-label small fw-bold text-dark", "3. Kode Unik Lahan (Buku Log)"),
                input(`type` := "text", id := "inputPlotCode", name := "code", cls := "form-control form-control-sm py-2", placeholder := "Contoh: Lahan-1-Cabai", value := "Lahan-1-Cabai", required := true),
                div(cls := "form-text small", "Kode ini menjadi pengenal buku log catatan yang terkunci.")
              )
            ),
            div(cls := "modal-footer border-top py-3 px-4 bg-light")(
              button(`type` := "button", cls := "btn btn-sm btn-light border px-3", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant px-4", "Kunci Cetakan & Mulai Tanam")
            )
          )
        )
      )
    )

  // ==========================================================================
  // 3. MODAL: BUAT CETAKAN BUDIDAYA BARU (MANDOR)
  // ==========================================================================
  def tambahPanduanModal(): Modifier =
    div(cls := "modal fade", id := "tambahPanduanModal", tabindex := "-1")(
      div(cls := "modal-dialog modal-dialog-centered")(
        div(cls := "modal-content border-0 shadow rounded-4 overflow-hidden")(
          form(action := "/panduan/tambah", method := "POST")(
            div(cls := "modal-header border-bottom py-3 px-4 bg-light")(
              div(
                h5(cls := "modal-title fs-6 fw-bold text-dark mb-0", "Buat Cetakan Budidaya Baru (Mandor)"),
                span(cls := "text-muted small", "Mandor menyusun standar SOP acuan tanam petani")
              ),
              button(`type` := "button", cls := "btn-close small", data("bs-dismiss") := "modal")
            ),
            div(cls := "modal-body py-3 px-4")(
              div(cls := "mb-3")(
                label(cls := "form-label small fw-bold text-dark", "ID Cetakan"),
                input(`type` := "text", name := "id", cls := "form-control form-control-sm py-2", placeholder := "Contoh: cetakan-cabai-01", required := true)
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-bold text-dark", "Nama Lengkap Cetakan SOP"),
                input(`type` := "text", name := "name", cls := "form-control form-control-sm py-2", placeholder := "Contoh: Cetakan Budidaya Cabai Rawit (Lahan 1)", required := true)
              ),
              div(cls := "mb-3")(
                label(cls := "form-label small fw-bold text-dark", "Komoditas & Varietas"),
                input(`type` := "text", name := "cropVariety", cls := "form-control form-control-sm py-2", placeholder := "Contoh: Cabai Rawit Merah", required := true)
              ),
              div(cls := "mb-2")(
                label(cls := "form-label small fw-bold text-dark", "Nama Mandor Penyusun"),
                input(`type` := "text", name := "author", cls := "form-control form-control-sm py-2", placeholder := "Contoh: Ir. Budi Santoso", required := true)
              )
            ),
            div(cls := "modal-footer border-top py-3 px-4 bg-light")(
              button(`type` := "button", cls := "btn btn-sm btn-light border px-3", data("bs-dismiss") := "modal", "Batal"),
              button(`type` := "submit", cls := "btn btn-sm btn-verdant px-4", "Simpan Draf Cetakan Awal")
            )
          )
        )
      )
    )
