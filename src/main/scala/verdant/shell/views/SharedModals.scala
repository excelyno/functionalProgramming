package verdant.shell.views

import scalatags.Text.all.*
import verdant.core.*

/**
 * Modal form yang dipakai petani dan mandor.
 *
 * Semua modal dirancang seragam, bersih, dan nyaman digunakan baik di desktop maupun ponsel (HP).
 */
trait SharedModals extends ViewHelpers:

  // --------------------------------------------------------------------------
  // Kerangka dasar modal
  // --------------------------------------------------------------------------
  private def modalFrame(
    modalId: String,
    heading: String,
    actionUrl: String,
    subtitle: Option[String] = None
  )(body: Modifier*)(footer: Modifier*): Modifier =
    val titleId = s"$modalId-title"
    div(cls := "modal fade", id := modalId, tabindex := "-1", attr("aria-labelledby") := titleId)(
      div(cls := "modal-dialog modal-dialog-centered", style := "max-width: 520px;")(
        div(cls := "modal-content rounded-2 border shadow-sm")(
          form(action := actionUrl, method := "POST")(
            div(cls := "modal-header py-3 px-4")(
              div(
                h5(cls := "modal-title fs-6 fw-semibold", id := titleId, heading),
                subtitle.map(s => p(cls := "small text-muted mb-0", s)).getOrElse(modifier())
              ),
              button(`type` := "button", cls := "btn-close", data("bs-dismiss") := "modal", attr("aria-label") := "Tutup")
            ),
            div(cls := "modal-body px-4 pt-3 pb-2")(body*),
            div(cls := "modal-footer py-3 px-4")(footer*)
          )
        )
      )
    )

  private def cancelButton: Modifier =
    button(`type` := "button", cls := "btn btn-sm btn-link text-secondary text-decoration-none", data("bs-dismiss") := "modal", "Batal")

  // ==========================================================================
  // 1. MODAL: CATAT KEGIATAN LAPANGAN (PETANI)
  // ==========================================================================
  def catatKegiatanModal(
    periods: List[(CultivationPeriod, String)],
    defaultPeriodCode: Option[String] = None,
    currentUser: Option[User] = None
  ): Modifier =
    val kinds = List(
      ("Penyiraman",        "Penyiraman",        "mis. 200 liter",        "100 liter|200 liter|1 drum"),
      ("Pemupukan",         "Pemupukan",         "mis. 25 kg NPK",        "10 kg NPK|25 kg NPK|50 kg kompos"),
      ("Pengendalian Hama", "Pengendalian hama", "mis. 1 tangki semprot", "1 tangki semprot nabati|5 lembar perangkap kuning"),
      ("Pemangkasan",       "Pemangkasan",       "mis. 50 pohon",         ""),
      ("Panen",             "Panen",             "mis. 50 kg",            "15 kg|50 kg|100 kg"),
      ("Penyiangan",        "Penyiangan",        "mis. 2 bedeng",         "")
    )

    val modalScript =
      """
      (function () {
        var root = document.getElementById('catatKegiatanModal');
        if (!root) return;
        var qty  = root.querySelector('#catat-qty');
        var list = root.querySelector('#catat-qty-list');
        root.querySelectorAll('input[name="activityType"]').forEach(function (r) {
          r.addEventListener('change', function () {
            if (qty) qty.placeholder = r.dataset.placeholder || '';
            if (list) {
              list.innerHTML = '';
              (r.dataset.suggest ? r.dataset.suggest.split('|') : []).forEach(function (s) {
                var o = document.createElement('option'); o.value = s; list.appendChild(o);
              });
            }
          });
        });
        var first = root.querySelector('input[name="activityType"]:checked');
        if (first) first.dispatchEvent(new Event('change'));
      })();
      """

    modalFrame("catatKegiatanModal", "Catat kegiatan", "/catatan/tambah")(
      // 1. Petak Lahan
      div(cls := "mb-3")(
        label(cls := "form-label small fw-medium mb-1", `for` := "catat-period", "Petak lahan"),
        select(name := "periodCode", id := "catat-period", cls := "form-select form-select-sm", required := true)(
          if periods.isEmpty then
            option(value := "", "Belum ada petak aktif")
          else
            periods.map { case (p, _) =>
              option(
                value := p.code,
                if defaultPeriodCode.contains(p.code) then selected := true else modifier(),
                s"${p.plotName} · ${p.cropVariety} (${p.code})"
              )
            }
        )
      ),

      // 2. Jenis Kegiatan
      div(cls := "mb-3")(
        span(cls := "form-label small fw-medium d-block mb-1", "Jenis kegiatan"),
        div(cls := "d-flex flex-wrap gap-2", role := "radiogroup")(
          kinds.zipWithIndex.map { case ((v, lbl, ph, sug), idx) =>
            val rid = s"catat-kind-$idx"
            frag(
              input(
                `type` := "radio", cls := "btn-check", name := "activityType", id := rid,
                value := v,
                data("placeholder") := ph,
                data("suggest") := sug,
                if idx == 0 then checked := true else modifier()
              ),
              label(cls := "btn btn-sm btn-outline-success rounded-pill px-3", `for` := rid, lbl)
            )
          }
        )
      ),

      // 3. Tanggal & Takaran
      div(cls := "row g-3 mb-3")(
        div(cls := "col-5")(
          label(cls := "form-label small fw-medium mb-1", `for` := "catat-date", "Tanggal"),
          input(`type` := "date", id := "catat-date", name := "activityDate", cls := "form-control form-control-sm", value := todayStr, required := true)
        ),
        div(cls := "col-7")(
          label(cls := "form-label small fw-medium mb-1", `for` := "catat-qty", "Takaran / hasil"),
          input(`type` := "text", id := "catat-qty", name := "quantity", cls := "form-control form-control-sm",
            attr("list") := "catat-qty-list", autocomplete := "off"),
          tag("datalist")(id := "catat-qty-list")
        )
      ),

      // 4. Catatan Lapangan
      div(cls := "mb-3")(
        label(cls := "form-label small fw-medium mb-1", `for` := "catat-notes", "Kondisi lapangan"),
        textarea(name := "notes", id := "catat-notes", cls := "form-control form-control-sm", rows := 3, required := true,
          placeholder := "Apa yang dilakukan, kondisi tanaman, cuaca, atau masalah yang ditemukan")
      ),

      // 5. Petani Pelaksana
      currentUser match
        case Some(u) =>
          div(cls := "mb-2")(
            label(cls := "form-label small fw-medium mb-1", "Petani pelaksana"),
            div(cls := "p-2 bg-light border rounded-2 small d-flex align-items-center justify-content-between")(
              span(cls := "fw-semibold text-dark", u.fullName),
              span(cls := "badge bg-success-subtle text-success border border-success-subtle", "Akun Terverifikasi")
            ),
            input(`type` := "hidden", id := "catat-farmer", name := "farmerName", value := u.fullName)
          )
        case None =>
          div(cls := "mb-2")(
            label(cls := "form-label small fw-medium mb-1", `for` := "catat-farmer", "Petani pelaksana"),
            input(`type` := "text", id := "catat-farmer", name := "farmerName", cls := "form-control form-control-sm",
              required := true, autocomplete := "off", attr("list") := "catat-farmer-list", value := "Pak Joko"),
            tag("datalist")(id := "catat-farmer-list")(
              List("Pak Joko", "Pak Slamet", "Bu Siti").map(n => option(value := n))
            )
          )
      ,

      script(raw(modalScript))
    )(
      p(cls := "text-muted me-auto mb-0", style := "font-size: 12px;", "Catatan tidak bisa diubah atau dihapus setelah disimpan."),
      cancelButton,
      button(`type` := "submit", cls := "btn btn-sm btn-verdant px-4", "Simpan")
    )

  // ==========================================================================
  // 2. MODAL: TERAPKAN CETAKAN KE LAHAN (PETANI)
  // ==========================================================================
  def bukaMasaTanamModal(
    publishedGuides: List[(PublishedGuide, String)],
    defaultGuideId: Option[String] = None
  ): Modifier =
    val modalScript =
      """
      (function () {
        var name = document.getElementById('plot-name');
        var code = document.getElementById('plot-code');
        if (!name || !code) return;
        var touched = false;
        code.addEventListener('input', function () { touched = code.value.length > 0; });
        name.addEventListener('input', function () {
          if (touched) return;
          code.value = name.value.trim().replace(/[^A-Za-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
        });
      })();
      """

    modalFrame(
      "bukaMasaTanamModal",
      "Terapkan cetakan ke lahan",
      "/masa-tanam/buka",
      Some("Takaran pupuk dan air di lahan akan mengikuti cetakan yang dipilih.")
    )(
      div(cls := "mb-3")(
        label(cls := "form-label small fw-medium mb-1", `for` := "plot-guide", "Cetakan budidaya"),
        select(name := "guideSelection", id := "plot-guide", cls := "form-select form-select-sm", required := true)(
          if publishedGuides.isEmpty then
            option(value := "", "Belum ada cetakan dari mandor")
          else
            publishedGuides.map { case (pub, guideTitle) =>
              option(
                value := s"${pub.guideId}|${pub.editionTag}",
                if defaultGuideId.contains(pub.guideId) then selected := true else modifier(),
                s"$guideTitle (${pub.editionTag})"
              )
            }
        ),
        div(cls := "form-text small", "Cetakan dikunci pada edisi ini dan tidak berubah walau mandor merilis edisi baru.")
      ),

      div(cls := "mb-3")(
        label(cls := "form-label small fw-medium mb-1", `for` := "plot-name", "Nama petak lahan"),
        input(`type` := "text", id := "plot-name", name := "plotName", cls := "form-control form-control-sm",
          placeholder := "mis. Petak Barat, Greenhouse 2A", required := true, autocomplete := "off")
      ),

      div(cls := "mb-3")(
        label(cls := "form-label small fw-medium mb-1", `for` := "plot-code", "Kode lahan"),
        input(`type` := "text", id := "plot-code", name := "code", cls := "form-control form-control-sm font-monospace",
          placeholder := "mis. Petak-Barat", required := true, autocomplete := "off"),
        div(cls := "form-text small", "Dipakai sebagai pengenal log kegiatan. Terisi otomatis dari nama petak, bisa diubah.")
      ),

      script(raw(modalScript))
    )(
      cancelButton,
      button(`type` := "submit", cls := "btn btn-sm btn-verdant px-4", "Mulai tanam")
    )

  // ==========================================================================
  // 3. MODAL: BUAT CETAKAN BUDIDAYA BARU (MANDOR)
  // ==========================================================================
  def tambahPanduanModal(currentUser: Option[User] = None): Modifier =
    modalFrame(
      "tambahPanduanModal",
      "Buat cetakan budidaya baru",
      "/panduan/tambah",
      Some("Mandor menyusun standar SOP acuan tanam petani.")
    )(
      div(cls := "mb-3")(
        label(cls := "form-label small fw-medium mb-1", `for` := "guide-id", "ID Cetakan"),
        input(`type` := "text", id := "guide-id", name := "id", cls := "form-control form-control-sm",
          placeholder := "mis. cetakan-cabai-01", required := true, autocomplete := "off")
      ),

      div(cls := "mb-3")(
        label(cls := "form-label small fw-medium mb-1", `for` := "guide-name", "Nama cetakan SOP"),
        input(`type` := "text", id := "guide-name", name := "name", cls := "form-control form-control-sm",
          placeholder := "mis. Cetakan Budidaya Cabai Rawit (Lahan 1)", required := true, autocomplete := "off")
      ),

      div(cls := "mb-3")(
        label(cls := "form-label small fw-medium mb-1", `for` := "guide-crop", "Komoditas & Varietas"),
        input(`type` := "text", id := "guide-crop", name := "cropVariety", cls := "form-control form-control-sm",
          placeholder := "mis. Cabai Rawit Merah Varietas Ori 212", required := true, autocomplete := "off")
      ),

      currentUser match
        case Some(u) =>
          div(cls := "mb-2")(
            label(cls := "form-label small fw-medium mb-1", "Mandor penyusun"),
            div(cls := "p-2 bg-light border rounded-2 small d-flex align-items-center justify-content-between")(
              span(cls := "fw-semibold text-dark", u.fullName),
              span(cls := "badge bg-primary-subtle text-primary border border-primary-subtle", "Mandor Terverifikasi")
            ),
            input(`type` := "hidden", id := "guide-author", name := "author", value := u.fullName)
          )
        case None =>
          div(cls := "mb-2")(
            label(cls := "form-label small fw-medium mb-1", `for` := "guide-author", "Nama mandor penyusun"),
            input(`type` := "text", id := "guide-author", name := "author", cls := "form-control form-control-sm",
              placeholder := "mis. Ir. Budi Santoso", required := true, value := "Ir. Budi Santoso")
          )
    )(
      cancelButton,
      button(`type` := "submit", cls := "btn btn-sm btn-verdant px-4", "Simpan draf")
    )
