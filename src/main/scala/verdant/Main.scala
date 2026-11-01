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

  // ==========================================================================
  // REQUEST HELPERS, USER SESSIONS & ROLE AUTHORIZATION
  // ==========================================================================

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

  def getCurrentUser(req: cask.Request): Option[User] =
    Option(req.exchange.getRequestCookies.get("verdant_user_id"))
      .map(_.getValue)
      .flatMap(db.getUser)

  def getRole(req: cask.Request): Role =
    getCurrentUser(req).map(_.userRole).getOrElse(Role.Farmer)

  private def requireRole(req: cask.Request, required: Role)(action: => cask.Response[String]): cask.Response[String] =
    getCurrentUser(req) match
      case None =>
        cask.Redirect("/login")
      case Some(user) =>
        val current = user.userRole
        if current == required then action
        else
          val requiredName = if required == Role.Supervisor then "Mandor" else "Petani"
          val currentName = if current == Role.Supervisor then "Mandor" else "Petani"
          htmlResponse(
            Views.layout("Akses Ditolak", "beranda", current.toString.toLowerCase, Some(("danger", s"Akses Ditolak (403): Fitur ini memerlukan hak akses $requiredName.")), Some(user))(
              Views.forbiddenView(requiredName, currentName)
            ),
            status = 403
          )

  // ==========================================================================
  // AUTHENTICATION & SESSION ROUTES
  // ==========================================================================

  @cask.get("/login")
  def loginPage(req: cask.Request, error: Option[String] = None): cask.Response[String] =
    getCurrentUser(req) match
      case Some(_) =>
        cask.Redirect("/")
      case None =>
        htmlResponse(Views.loginPage(error))

  @cask.post("/login")
  def loginSubmit(req: cask.Request): cask.Response[String] =
    val form = parseForm(req)
    val username = form.getOrElse("username", "").trim
    val password = form.getOrElse("password", "").trim

    db.authenticate(username, password) match
      case Some(user) =>
        cask.Response(
          data = "",
          statusCode = 302,
          headers = Seq(
            "Location" -> "/",
            "Set-Cookie" -> s"verdant_user_id=${user.id}; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400",
            "Set-Cookie" -> s"verdant_role=${user.role}; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400"
          )
        )
      case None =>
        htmlResponse(
          Views.loginPage(
            errorMsg = Some("Nama pengguna atau kata sandi tidak cocok. Silakan coba lagi."),
            registeredUsername = username
          ),
          status = 401
        )

  @cask.get("/quick-login/:userId")
  def quickLogin(userId: String, req: cask.Request): cask.Response[String] =
    db.getUser(userId) match
      case Some(user) =>
        cask.Response(
          data = "",
          statusCode = 302,
          headers = Seq(
            "Location" -> "/",
            "Set-Cookie" -> s"verdant_user_id=${user.id}; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400",
            "Set-Cookie" -> s"verdant_role=${user.role}; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400"
          )
        )
      case None =>
        cask.Redirect("/login?error=Pengguna+tidak+ditemukan")

  @cask.get("/logout")
  def logout(req: cask.Request): cask.Response[String] =
    cask.Response(
      data = "",
      statusCode = 302,
      headers = Seq(
        "Location" -> "/login",
        "Set-Cookie" -> "verdant_user_id=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0",
        "Set-Cookie" -> "verdant_role=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0"
      )
    )

  // ==========================================================================
  // 1. BERANDA (HOME / ROLE-SPECIFIC DASHBOARD)
  // ==========================================================================

  @cask.get("/")
  def beranda(req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    getCurrentUser(req) match
      case None =>
        cask.Redirect("/login")
      case Some(user) =>
        val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)
        user.userRole match
          case Role.Farmer =>
            val periods = db.listPeriods()
            val recentActivities = db.listActivities(None)
            val publishedGuides = db.listPublishedGuidesWithSnapshots()
            htmlResponse(Views.layout("Beranda Petani", "beranda", "farmer", alert, Some(user))(
              Views.farmerBerandaView(periods, recentActivities, publishedGuides, Some(user))
            ))

          case Role.Supervisor =>
            val draftsAwaiting = db.getDraftsAwaitingReview()
            val publishedGuides = db.listPublishedGuides()
            val guides = db.listGuides()
            val recentRevisions = guides.flatMap(g => db.listRevisionsForGuide(g.id).take(2).map(r => (r, g.name)))
            htmlResponse(Views.layout("Beranda Mandor", "beranda", "supervisor", alert, Some(user))(
              Views.supervisorBerandaView(draftsAwaiting, publishedGuides, recentRevisions, Some(user))
            ))

  // ==========================================================================
  // 2. FARMER WORKSPACE: MASA TANAM SAYA
  // ==========================================================================

  @cask.get("/masa-tanam")
  def masaTanamList(req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    val user = getCurrentUser(req)
    val periods = db.listPeriods()
    val publishedGuides = db.listPublishedGuides()
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    htmlResponse(Views.layout("Masa Tanam", "masa-tanam", getRole(req).toString.toLowerCase, alert, user)(
      Views.farmerMasaTanamListView(periods, publishedGuides)
    ))

  @cask.post("/masa-tanam/buka")
  def bukaMasaTanam(req: cask.Request): cask.Response[String] =
    requireRole(req, Role.Farmer) {
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
    }

  @cask.get("/masa-tanam/:code")
  def masaTanamDetail(code: String, req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    val user = getCurrentUser(req)
    db.getPeriod(code) match
      case None =>
        cask.Redirect("/masa-tanam?alertKind=warning&alertMsg=Masa+tanam+tidak+ditemukan.")
      case Some((period, guide, revision, publishedGuide)) =>
        val activities = db.listActivities(Some(code))
        val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

        htmlResponse(Views.layout(s"Masa Tanam · ${period.code}", "masa-tanam", getRole(req).toString.toLowerCase, alert, user)(
          Views.farmerMasaTanamDetailView(period, guide, revision, publishedGuide, activities, user)
        ))

  // ==========================================================================
  // 3. FARMER WORKSPACE: PANDUAN BUDIDAYA (READ-ONLY)
  // ==========================================================================

  @cask.get("/panduan")
  def panduanList(req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    val role = getRole(req)
    val user = getCurrentUser(req)
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    role match
      case Role.Farmer =>
        val publishedGuides = db.listPublishedGuidesWithSnapshots()
        htmlResponse(Views.layout("Panduan Budidaya", "panduan", "farmer", alert, user)(
          Views.farmerPanduanListView(publishedGuides)
        ))
      case Role.Supervisor =>
        cask.Redirect("/kelola-panduan")

  @cask.get("/panduan/:id")
  def panduanDetail(id: String, req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    val role = getRole(req)
    val user = getCurrentUser(req)
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    db.getGuide(id) match
      case None =>
        cask.Redirect("/panduan?alertKind=warning&alertMsg=Panduan+tidak+ditemukan.")
      case Some(guide) =>
        role match
          case Role.Farmer =>
            db.getLatestPublishedGuide(id) match
              case None =>
                cask.Redirect("/panduan?alertKind=warning&alertMsg=Panduan+ini+belum+memiliki+edisi+resmi+yang+diterbitkan+untuk+petani.")
              case Some(pub) =>
                db.getRevision(pub.revisionId) match
                  case None =>
                    cask.Redirect("/panduan?alertKind=danger&alertMsg=Revisi+edisi+resmi+tidak+ditemukan.")
                  case Some(pubRev) =>
                    val summary = VerdantCore.summarizeGuide(pubRev.snapshot)
                    htmlResponse(Views.layout(guide.name, "panduan", "farmer", alert, user)(
                      Views.farmerPanduanDetailView(guide, pub, pubRev.snapshot, summary)
                    ))

          case Role.Supervisor =>
            db.getLatestRevision(id) match
              case None =>
                cask.Redirect("/kelola-panduan?alertKind=danger&alertMsg=Revisi+panduan+tidak+ditemukan.")
              case Some(latestRevision) =>
                val publishedOpt = db.getLatestPublishedGuide(id)
                val summary = VerdantCore.summarizeGuide(latestRevision.snapshot)
                htmlResponse(Views.layout(guide.name, "kelola-panduan", "supervisor", alert, user)(
                  Views.supervisorPanduanDetailView(guide, latestRevision, publishedOpt, summary)
                ))

  // ==========================================================================
  // 4. SUPERVISOR WORKSPACE: KELOLA PANDUAN & TAMBAH PANDUAN
  // ==========================================================================

  @cask.get("/kelola-panduan")
  def kelolaPanduan(req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      val guides = db.listGuides().flatMap { g =>
        db.getLatestRevision(g.id).map { rev =>
          (g, rev, db.getLatestPublishedGuide(g.id))
        }
      }
      val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)
      htmlResponse(Views.layout("Kelola Panduan", "kelola-panduan", "supervisor", alert, user)(
        Views.supervisorKelolaPanduanView(guides, user)
      ))
    }

  @cask.post("/panduan/tambah")
  def tambahPanduan(req: cask.Request): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      val form = parseForm(req)
      val id = form.getOrElse("id", "").trim
      val name = form.getOrElse("name", "").trim
      val rawVariety = form.getOrElse("cropVariety", "").trim
      val author = user.map(_.fullName).getOrElse(form.getOrElse("author", "Mandor Kebun").trim)

      if id.isEmpty || name.isEmpty || rawVariety.isEmpty then
        cask.Redirect("/kelola-panduan?alertKind=danger&alertMsg=Semua+data+panduan+wajib+diisi.")
      else
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
          cask.Redirect(s"/panduan/$id?alertKind=success&alertMsg=Panduan+baru+'$name'+berhasil+dibuat.+Silakan+periksa+atau+terbitkan+edisi+resmi.")
        catch
          case ex: Throwable =>
            cask.Redirect(s"/kelola-panduan?alertKind=danger&alertMsg=Gagal+membuat+panduan:+${ex.getMessage}")
    }

  // ==========================================================================
  // 5. SUPERVISOR WORKSPACE: PEMERIKSAAN PERUBAHAN & TINJAUAN DIFF
  // ==========================================================================

  @cask.get("/pemeriksaan-perubahan")
  def pemeriksaanPerubahan(req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      val drafts = db.getDraftsAwaitingReview().map { case (g, currRev, pubOpt) =>
        val baseSnapshot = pubOpt.flatMap(p => db.getRevision(p.revisionId)).map(_.snapshot).getOrElse(currRev.snapshot)
        val diffSummary = VerdantCore.computeReadableDiff(baseSnapshot, currRev.snapshot)
        (g, currRev, Some(baseSnapshot), diffSummary, pubOpt)
      }
      val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)
      htmlResponse(Views.layout("Pemeriksaan Perubahan", "pemeriksaan-perubahan", "supervisor", alert, user)(
        Views.supervisorPemeriksaanPerubahanView(drafts)
      ))
    }

  @cask.post("/panduan/:id/perbarui")
  def perbaruiDrafPanduan(id: String, req: cask.Request): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      val form = parseForm(req)
      val author = user.map(_.fullName).getOrElse(form.getOrElse("author", "Mandor Kebun").trim)
      val changeNotes = form.getOrElse("changeNotes", "Penyesuaian rekomendasi budidaya").trim
      val targetStageName = form.getOrElse("stageName", "").trim
      val targetParamName = form.getOrElse("paramName", "").trim
      val rawTargetVal = form.getOrElse("newTargetValue", "").toDoubleOption

      db.getGuide(id) match
        case None => cask.Redirect("/kelola-panduan")
        case Some(guide) =>
          db.getLatestRevision(id) match
            case None => cask.Redirect(s"/panduan/$id")
            case Some(headRevision) =>
              rawTargetVal match
                case None =>
                  cask.Redirect(s"/panduan/$id?alertKind=danger&alertMsg=Nilai+target+anjuran+harus+berupa+angka.")
                case Some(newVal) =>
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
    }

  @cask.get("/panduan/:id/tinjau")
  def tinjauPerubahan(id: String, req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      db.getGuide(id) match
        case None => cask.Redirect("/kelola-panduan")
        case Some(guide) =>
          db.getLatestRevision(id) match
            case None => cask.Redirect(s"/panduan/$id")
            case Some(currRev) =>
              val pubOpt = db.getLatestPublishedGuide(id)
              val baseSnapshot = pubOpt.flatMap(p => db.getRevision(p.revisionId)).map(_.snapshot)
                .orElse(currRev.parentIds.headOption.flatMap(pid => db.getRevision(pid)).map(_.snapshot))
                .getOrElse(currRev.snapshot)

              val diffSummary = VerdantCore.computeReadableDiff(baseSnapshot, currRev.snapshot)
              val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

              htmlResponse(Views.layout("Tinjauan Perubahan", "pemeriksaan-perubahan", "supervisor", alert, user)(
                Views.supervisorTinjauPerubahanView(guide, currRev, None, diffSummary, pubOpt)
              ))
    }

  @cask.post("/panduan/:id/terbitkan")
  def terbitkanPanduan(id: String, req: cask.Request): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      val form = parseForm(req)
      val revisionId = form.getOrElse("revisionId", "").trim
      val editionTag = form.getOrElse("editionTag", "").trim
      val approvedBy = user.map(_.fullName).getOrElse(form.getOrElse("approvedBy", "").trim)
      val notes = form.getOrElse("notes", "Disetujui untuk operasional kebun").trim

      (for {
        revision <- db.getRevision(revisionId).toRight("Revisi panduan tidak ditemukan.")
        _ <- VerdantCore.validateGuideForPublishing(revision.snapshot)
        pub <- db.publishGuide(id, editionTag, revisionId, approvedBy, notes)
      } yield pub) match
        case Left(err) =>
          cask.Redirect(s"/panduan/$id/tinjau?alertKind=danger&alertMsg=${err.replace(" ", "+")}")
        case Right(pub) =>
          cask.Redirect(s"/panduan/$id?alertKind=success&alertMsg=Panduan+resmi+berhasil+diterbitkan+sebagai+'${pub.editionTag}'.")
    }

  @cask.post("/panduan/:id/tolak")
  def tolakDrafPanduan(id: String, req: cask.Request): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      val form = parseForm(req)
      val rejectedBy = user.map(_.fullName).getOrElse(form.getOrElse("rejectedBy", "Mandor Pemeriksa").trim)
      val reason = form.getOrElse("reason", "Takaran belum sesuai kebutuhan").trim

      db.rejectDraft(id, rejectedBy, reason) match
        case Left(err) =>
          cask.Redirect(s"/panduan/$id/tinjau?alertKind=danger&alertMsg=${err.replace(" ", "+")}")
        case Right(_) =>
          cask.Redirect(s"/pemeriksaan-perubahan?alertKind=info&alertMsg=Draf+perubahan+berhasil+ditolak+dan+dipulihkan+ke+edisi+resmi+sebelumnya.")
    }

  // ==========================================================================
  // 6. SUPERVISOR WORKSPACE: RIWAYAT & SILSILAH REVISI
  // ==========================================================================

  @cask.get("/riwayat")
  def riwayat(req: cask.Request, alertKind: Option[String] = None, alertMsg: Option[String] = None): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      val guides = db.listGuides()
      val allRevisions = guides.flatMap(g => db.listRevisionsForGuide(g.id))
      val publishedEditions = db.listPublishedGuides()
      val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

      htmlResponse(Views.layout("Riwayat Panduan", "riwayat", "supervisor", alert, user)(
        Views.supervisorRiwayatView(allRevisions, publishedEditions)
      ))
    }

  @cask.post("/riwayat/pulihkan")
  def pulihkanRevisi(req: cask.Request): cask.Response[String] =
    requireRole(req, Role.Supervisor) {
      val user = getCurrentUser(req)
      val form = parseForm(req)
      val targetRevisionId = form.getOrElse("targetRevisionId", "").trim
      val author = user.map(_.fullName).getOrElse(form.getOrElse("author", "Mandor Pemulih").trim)

      (for {
        targetRev <- db.getRevision(targetRevisionId).toRight("Revisi target tidak ditemukan.")
        guide <- db.listGuides().find(_.name == targetRev.snapshot.name).orElse(db.listGuides().headOption).toRight("Panduan tidak ditemukan.")
        headRev <- db.getLatestRevision(guide.id).toRight("Revisi terkini tidak ditemukan.")
        newRevision <- VerdantCore.restoreGuideRevision(targetRev, headRev, author, System.currentTimeMillis())
      } yield (guide, newRevision)) match
        case Left(err) =>
          cask.Redirect(s"/riwayat?alertKind=danger&alertMsg=$err")
        case Right((guide, newRev)) =>
          db.saveRevision(guide.id, newRev)
          cask.Redirect(s"/panduan/${guide.id}?alertKind=success&alertMsg=Panduan+berhasil+dipulihkan+ke+versi+sebelumnya.")
    }

  // ==========================================================================
  // 7. JURNAL KEGIATAN BUDIDAYA (SHARED / FARMER JOURNAL)
  // ==========================================================================

  @cask.get("/catatan")
  def catatan(
    req: cask.Request,
    periodCode: Option[String] = None,
    alertKind: Option[String] = None,
    alertMsg: Option[String] = None
  ): cask.Response[String] =
    val user = getCurrentUser(req)
    val filterCode = periodCode.filter(_.trim.nonEmpty)
    val activities = db.listActivities(filterCode)
    val periods = db.listPeriods()
    val alert = for { k <- alertKind; m <- alertMsg } yield (k, m)

    htmlResponse(Views.layout("Catatan Budidaya", "catatan", getRole(req).toString.toLowerCase, alert, user)(
      Views.catatanView(activities, periods, filterCode, user)
    ))

  @cask.post("/catatan/tambah")
  def tambahCatatan(req: cask.Request): cask.Response[String] =
    requireRole(req, Role.Farmer) {
      val user = getCurrentUser(req)
      val form = parseForm(req)
      val periodCode = form.getOrElse("periodCode", "").trim
      val activityType = form.getOrElse("activityType", "").trim
      val activityDate = form.getOrElse("activityDate", "").trim
      val notes = form.getOrElse("notes", "").trim
      val quantity = form.get("quantity").filter(_.trim.nonEmpty)
      val farmerName = user.map(_.fullName).getOrElse(form.getOrElse("farmerName", "Petani").trim)

      if periodCode.isEmpty || activityType.isEmpty || activityDate.isEmpty || notes.isEmpty then
        cask.Redirect("/catatan?alertKind=danger&alertMsg=Masa+tanam,+jenis+kegiatan,+tanggal,+dan+catatan+wajib+diisi.")
      else
        val actId = s"act-${UUID.randomUUID().toString.take(8)}"
        db.addActivity(actId, periodCode, activityType, activityDate, notes, quantity, farmerName) match
          case Left(err) =>
            cask.Redirect(s"/catatan?alertKind=danger&alertMsg=$err")
          case Right(_) =>
            cask.Redirect(s"/masa-tanam/$periodCode?alertKind=success&alertMsg=Kegiatan+'$activityType'+berhasil+dicatat+di+lahan.")
    }

  override def main(args: Array[String]): Unit =
    super.main(args)
    println(s"=== Verdant Pertanian aktif di http://$host:$port ===")
    Thread.currentThread().join()

  initialize()
