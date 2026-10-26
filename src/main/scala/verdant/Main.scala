package verdant

import cask.*
import verdant.core.*
import verdant.shell.*
import java.net.URLDecoder
import java.util.UUID

object Main extends cask.MainRoutes:

  override def port: Int = 8080
  override def host: String = "127.0.0.1"

  val db = new Database("verdant.db")
  db.initSchema()

  // Helper aman untuk membaca URL-encoded form submissions dengan Undertow
  private def parseForm(req: cask.Request): Map[String, String] =
    val exchange = req.exchange
    if !exchange.isBlocking then exchange.startBlocking()
    val parser = new io.undertow.server.handlers.form.FormEncodedDataDefinition().create(exchange)
    val formData = if parser != null then parser.parseBlocking() else null
    if formData == null then Map.empty
    else
      val builder = collection.mutable.Map[String, String]()
      val it = formData.iterator()
      while it.hasNext do
        val key = it.next()
        val firstVal = formData.getFirst(key)
        if firstVal != null then
          builder += (key -> firstVal.getValue)
      builder.toMap

  private def htmlResponse(bodyHtml: String, status: Int = 200): cask.Response[String] =
    cask.Response(
      data = bodyHtml,
      statusCode = status,
      headers = Seq("Content-Type" -> "text/html; charset=utf-8")
    )

  // ==========================================================================
  // 1. BERANDA (HOME / DASHBOARD)
  // ==========================================================================

  @cask.get("/")
  def beranda(alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    val periods = db.listPeriods()
    val recentActivities = db.listActivities(None)
    val guides = db.listGuides()
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    htmlResponse(Views.layout("Beranda", "beranda", alert)(
      Views.berandaView(periods, recentActivities, guides)
    ))

  // ==========================================================================
  // 2. CATATAN BUDIDAYA (CULTIVATION ACTIVITY JOURNAL)
  // ==========================================================================

  @cask.get("/catatan")
  def catatan(
    periodCode: Option[String] = None,
    alertKind: Option[String] = None,
    alertMsg: Option[String] = None
  ): cask.Response[String] =
    val filterCode = periodCode.filter(_.trim.nonEmpty)
    val activities = db.listActivities(filterCode)
    val periods = db.listPeriods()
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    htmlResponse(Views.layout("Catatan Budidaya", "catatan", alert)(
      Views.catatanView(activities, periods, filterCode)
    ))

  @cask.post("/catatan/tambah")
  def tambahCatatan(req: cask.Request): cask.Response[String] =
    val form = parseForm(req)
    val periodCode = form.getOrElse("periodCode", "").trim
    val activityType = form.getOrElse("activityType", "").trim
    val activityDate = form.getOrElse("activityDate", "").trim
    val notes = form.getOrElse("notes", "").trim
    val quantity = form.get("quantity").filter(_.trim.nonEmpty)
    val farmerName = form.getOrElse("farmerName", "Petani").trim

    if periodCode.isEmpty || activityType.isEmpty || activityDate.isEmpty || notes.isEmpty then
      cask.Redirect("/catatan?alertKind=danger&alertMsg=Masa+tanam,+jenis+kegiatan,+tanggal,+dan+catatan+wajib+diisi.")
    else
      val actId = s"act-${UUID.randomUUID().toString.take(8)}"
      db.addActivity(actId, periodCode, activityType, activityDate, notes, quantity, farmerName) match
        case Left(err) =>
          cask.Redirect(s"/catatan?alertKind=danger&alertMsg=$err")
        case Right(_) =>
          cask.Redirect(s"/catatan?periodCode=$periodCode&alertKind=success&alertMsg=Kegiatan+'$activityType'+berhasil+dicatat+di+lahan.")

  // ==========================================================================
  // 3. PANDUAN BUDIDAYA (CULTIVATION GUIDES)
  // ==========================================================================

  @cask.get("/panduan")
  def panduanList(alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    val guides = db.listGuides()
    val publishedMap = guides.flatMap { g =>
      db.getLatestPublishedGuide(g.id).map(pub => g.id -> pub)
    }.toMap
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    htmlResponse(Views.layout("Panduan Budidaya", "panduan", alert)(
      Views.panduanListView(guides, publishedMap)
    ))

  @cask.post("/panduan/tambah")
  def tambahPanduan(req: cask.Request): cask.Response[String] =
    val form = parseForm(req)
    val id = form.getOrElse("id", "").trim
    val name = form.getOrElse("name", "").trim
    val rawVariety = form.getOrElse("cropVariety", "").trim
    val author = form.getOrElse("author", "Penyuluh Pertanian").trim

    if id.isEmpty || name.isEmpty || rawVariety.isEmpty then
      cask.Redirect("/panduan?alertKind=danger&alertMsg=Semua+data+panduan+wajib+diisi.")
    else
      // Menggunakan komposisi fungsi FP untuk menstandarkan nama varietas
      val cropVariety = VerdantCore.formatCropName(rawVariety)

      val initialSnapshot = GuideSnapshot(
        name = name,
        cropVariety = cropVariety,
        stages = List(
          CultivationStage(
            name = "Fase Persemaian & Awal Tanam",
            durationDays = 14,
            instructions = "Siapkan media tanam yang gembur dan cukup air. Lindungi bibit muda dari terik matahari langsung dan hujan lebat.",
            parameters = List(
              AgronomicParam("Target pH Media", 6.0, 5.5, 6.5, "pH"),
              AgronomicParam("Dosis Pupuk Awal", 20.0, 15.0, 30.0, "kg/ha")
            )
          ),
          CultivationStage(
            name = "Fase Vegetatif & Pertumbuhan",
            durationDays = 28,
            instructions = "Lakukan penyiraman dan pemupukan daun secara berkala. Periksa gulma di sekitar perakaran.",
            parameters = List(
              AgronomicParam("Target pH Media", 6.0, 5.8, 6.5, "pH"),
              AgronomicParam("Dosis Pupuk Susulan", 50.0, 40.0, 60.0, "kg/ha")
            )
          )
        )
      )

      try
        db.createGuide(id, name, cropVariety, initialSnapshot, author)
        cask.Redirect(s"/panduan/$id?alertKind=success&alertMsg=Panduan+baru+'$name'+berhasil+dibuat.")
      catch
        case ex: Throwable =>
          cask.Redirect(s"/panduan?alertKind=danger&alertMsg=Gagal+membuat+panduan:+${ex.getMessage}")

  @cask.get("/panduan/:id")
  def panduanDetail(id: String, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    db.getGuide(id) match
      case None =>
        cask.Redirect("/panduan?alertKind=warning&alertMsg=Panduan+tidak+ditemukan.")
      case Some(guide) =>
        db.getLatestRevision(id) match
          case None =>
            cask.Redirect("/panduan?alertKind=danger&alertMsg=Revisi+panduan+tidak+ditemukan.")
          case Some(latestRevision) =>
            val publishedOpt = db.getLatestPublishedGuide(id)
            val summary = VerdantCore.summarizeGuide(latestRevision.snapshot)
            val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

            htmlResponse(Views.layout(guide.name, "panduan", alert)(
              Views.panduanDetailView(guide, latestRevision, publishedOpt, summary)
            ))

  // ==========================================================================
  // 4. SUPERVISOR ACTIONS: EDIT DRAFT, REVIEW CHANGES, AND PUBLISH
  // ==========================================================================

  @cask.post("/panduan/:id/perbarui")
  def perbaruiDrafPanduan(id: String, req: cask.Request): cask.Response[String] =
    val form = parseForm(req)
    val author = form.getOrElse("author", "Mandor Kebun").trim
    val changeNotes = form.getOrElse("changeNotes", "Penyesuaian rekomendasi budidaya").trim
    val targetStageName = form.getOrElse("stageName", "").trim
    val targetParamName = form.getOrElse("paramName", "").trim
    val rawTargetVal = form.getOrElse("newTargetValue", "").toDoubleOption

    db.getGuide(id) match
      case None => cask.Redirect("/panduan")
      case Some(guide) =>
        db.getLatestRevision(id) match
          case None => cask.Redirect(s"/panduan/$id")
          case Some(headRevision) =>
            rawTargetVal match
              case None =>
                cask.Redirect(s"/panduan/$id?alertKind=danger&alertMsg=Nilai+target+anjuran+harus+berupa+angka.")
              case Some(newVal) =>
                // Pure higher-order function: transformStages modifies the snapshot purely
                val updatedSnapshot = VerdantCore.transformStages(headRevision.snapshot) { stage =>
                  if stage.name == targetStageName then
                    val updatedParams = stage.parameters.map { p =>
                      if p.name == targetParamName then p.copy(targetValue = newVal)
                      else p
                    }
                    stage.copy(parameters = updatedParams)
                  else stage
                }

                val timestamp = System.currentTimeMillis()
                val newRevision = VerdantCore.createGuideRevision(
                  parentIds = List(headRevision.id),
                  snapshot = updatedSnapshot,
                  author = author,
                  changeNotes = changeNotes,
                  timestamp = timestamp
                )

                db.saveRevision(id, newRevision)
                cask.Redirect(s"/panduan/$id/tinjau?alertKind=info&alertMsg=Draf+perubahan+berhasil+disimpan.+Silakan+tinjau+ringkasan+perubahan+di+bawah.")

  @cask.get("/panduan/:id/tinjau")
  def tinjauPerubahan(id: String, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    db.getGuide(id) match
      case None => cask.Redirect("/panduan")
      case Some(guide) =>
        db.getLatestRevision(id) match
          case None => cask.Redirect(s"/panduan/$id")
          case Some(currRev) =>
            val parentRevOpt = currRev.parentIds.headOption.flatMap(pid => db.getRevision(pid))
            val baseSnapshot = parentRevOpt.map(_.snapshot).getOrElse(currRev.snapshot)

            // Pure FP readable diff computation
            val diffSummary = VerdantCore.computeReadableDiff(baseSnapshot, currRev.snapshot)
            val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

            htmlResponse(Views.layout("Tinjauan Perubahan", "panduan", alert)(
              Views.reviewPerubahanView(guide, currRev, parentRevOpt, diffSummary)
            ))

  @cask.post("/panduan/:id/terbitkan")
  def terbitkanPanduan(id: String, req: cask.Request): cask.Response[String] =
    val form = parseForm(req)
    val revisionId = form.getOrElse("revisionId", "").trim
    val editionTag = form.getOrElse("editionTag", "").trim
    val approvedBy = form.getOrElse("approvedBy", "").trim
    val notes = form.getOrElse("notes", "Disetujui untuk operasional kebun").trim

    (for {
      revision <- db.getRevision(revisionId).toRight("Revisi panduan tidak ditemukan.")
      // Pure validation function: ensures no 0-day stages or dangerous parameters
      _ <- VerdantCore.validateGuideForPublishing(revision.snapshot)
      pub <- db.publishGuide(id, editionTag, revisionId, approvedBy, notes)
    } yield pub) match
      case Left(err) =>
        cask.Redirect(s"/panduan/$id/tinjau?alertKind=danger&alertMsg=${err.replace(" ", "+")}")
      case Right(pub) =>
        cask.Redirect(s"/panduan/$id?alertKind=success&alertMsg=Panduan+resmi+berhasil+diterbitkan+sebagai+'${pub.editionTag}'.")

  // ==========================================================================
  // 5. MASA TANAM (CULTIVATION PERIODS)
  // ==========================================================================

  @cask.get("/masa-tanam")
  def masaTanamList(alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    val periods = db.listPeriods()
    val publishedGuides = db.listPublishedGuides()
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    htmlResponse(Views.layout("Masa Tanam", "masa-tanam", alert)(
      Views.masaTanamListView(periods, publishedGuides)
    ))

  @cask.post("/masa-tanam/buka")
  def bukaMasaTanam(req: cask.Request): cask.Response[String] =
    val form = parseForm(req)
    val code = form.getOrElse("code", "").trim
    val guideSelection = form.getOrElse("guideSelection", "") // format: guideId|editionTag
    val plotName = form.getOrElse("plotName", "").trim

    val parts = guideSelection.split("\\|")
    if code.isEmpty || parts.length != 2 || plotName.isEmpty then
      cask.Redirect("/masa-tanam?alertKind=danger&alertMsg=Kode+masa+tanam,+panduan+resmi,+dan+nama+lahan+wajib+diisi.")
    else
      val guideId = parts(0)
      val publishedEdition = parts(1)

      db.createPeriod(code, guideId, publishedEdition, plotName) match
        case Left(err) =>
          cask.Redirect(s"/masa-tanam?alertKind=danger&alertMsg=$err")
        case Right(_) =>
          cask.Redirect(s"/masa-tanam/$code?alertKind=success&alertMsg=Masa+tanam+'$code'+berhasil+dibuka+di+$plotName.")

  @cask.get("/masa-tanam/:code")
  def masaTanamDetail(code: String, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    db.getPeriod(code) match
      case None =>
        cask.Redirect("/masa-tanam?alertKind=warning&alertMsg=Masa+tanam+tidak+ditemukan.")
      case Some((period, guide, revision, publishedGuide)) =>
        val activities = db.listActivities(Some(code))
        val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

        htmlResponse(Views.layout(s"Masa Tanam · ${period.code}", "masa-tanam", alert)(
          Views.masaTanamDetailView(period, guide, revision, publishedGuide, activities)
        ))

  // ==========================================================================
  // 6. RIWAYAT (HISTORY & REVISION RESTORE)
  // ==========================================================================

  @cask.get("/riwayat")
  def riwayat(alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    // Ambil panduan utama dan silsilah revisinya
    val guides = db.listGuides()
    val allRevisions = guides.flatMap(g => db.listRevisionsForGuide(g.id))
    val publishedEditions = db.listPublishedGuides()
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    htmlResponse(Views.layout("Riwayat Perubahan", "riwayat", alert)(
      Views.riwayatView(allRevisions, publishedEditions)
    ))

  @cask.post("/riwayat/pulihkan")
  def pulihkanRevisi(req: cask.Request): cask.Response[String] =
    val form = parseForm(req)
    val targetRevisionId = form.getOrElse("targetRevisionId", "").trim
    val author = form.getOrElse("author", "Mandor Pemulih").trim

    (for {
      targetRev <- db.getRevision(targetRevisionId).toRight("Revisi target tidak ditemukan.")
      guide <- db.listGuides().find(_.name == targetRev.snapshot.name).orElse(db.listGuides().headOption).toRight("Panduan tidak ditemukan.")
      headRev <- db.getLatestRevision(guide.id).toRight("Revisi terkini tidak ditemukan.")
      // Pure restoration calculation
      newRevision <- VerdantCore.restoreGuideRevision(targetRev, headRev, author, System.currentTimeMillis())
    } yield (guide, newRevision)) match
      case Left(err) =>
        cask.Redirect(s"/riwayat?alertKind=danger&alertMsg=$err")
      case Right((guide, newRev)) =>
        db.saveRevision(guide.id, newRev)
        cask.Redirect(s"/panduan/${guide.id}?alertKind=success&alertMsg=Panduan+berhasil+dipulihkan+ke+versi+sebelumnya.")

  override def main(args: Array[String]): Unit =
    super.main(args)
    println(s"=== Verdant Pertanian aktif di http://$host:$port ===")
    Thread.currentThread().join()

  initialize()

