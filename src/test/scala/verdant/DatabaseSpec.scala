package verdant

import munit.FunSuite
import verdant.core.*
import verdant.shell.Database
import java.io.File

class DatabaseSpec extends FunSuite:

  val testDbFile = "test_verdant.db"
  var db: Database = _

  override def beforeEach(context: BeforeEach): Unit =
    val f = new File(testDbFile)
    if f.exists() then f.delete()
    db = new Database(testDbFile)
    db.initSchema()

  override def afterEach(context: AfterEach): Unit =
    val f = new File(testDbFile)
    if f.exists() then f.delete()
    val wal = new File(testDbFile + "-wal")
    if wal.exists() then wal.delete()
    val shm = new File(testDbFile + "-shm")
    if shm.exists() then shm.delete()

  test("Database menginisialisasi dan menaburkan data awal pertanian Indonesia") {
    val guides = db.listGuides()
    assertEquals(guides.length, 2)
    val tomat = guides.find(_.id == "panduan-tomat-01").get
    assertEquals(tomat.cropVariety, "Tomat Beef Roma")

    val periods = db.listPeriods()
    assertEquals(periods.length, 2)
    val pTomat = periods.find(_._1.code == "MT-2026-TOMAT-01").get._1
    assertEquals(pTomat.plotName, "Greenhouse Blok 2A")
    assertEquals(pTomat.status, "BERJALAN")

    val activities = db.listActivities(Some("MT-2026-TOMAT-01"))
    assertEquals(activities.length, 3)
  }

  test("Alur Petani: Mencatat kegiatan budidaya baru di lahan") {
    val actRes = db.addActivity(
      id = "act-test-01",
      periodCode = "MT-2026-TOMAT-01",
      activityType = "Pemupukan",
      activityDate = "2026-10-27",
      notes = "Pemberian nutrisi kalsium pencegah busuk pantat buah (BER).",
      quantity = Some("25 kg Kalsium Nitrat"),
      farmerName = "Pak Joko"
    )
    assert(actRes.isRight)

    val updatedActs = db.listActivities(Some("MT-2026-TOMAT-01"))
    assertEquals(updatedActs.length, 4)
    val latest = updatedActs.head
    assertEquals(latest.activityType, "Pemupukan")
    assertEquals(latest.quantity, Some("25 kg Kalsium Nitrat"))
  }

  test("Alur Mandor: Ubah panduan -> Tinjau perubahan -> Setujui & Terbitkan edisi resmi") {
    val guide = db.getGuide("panduan-tomat-01").get
    val headRev = db.getLatestRevision(guide.id).get

    // 1. Mandor ubah dosis EC pada tahap pembungaan
    val snapBaru = VerdantCore.transformStages(headRev.snapshot) { stage =>
      if stage.name == "Fase Pembungaan & Pembentukan Buah" then
        val paramsBaru = stage.parameters.map { p =>
          if p.name == "Konsentrasi Nutrisi EC" then p.copy(targetValue = 2.8)
          else p
        }
        stage.copy(parameters = paramsBaru)
      else stage
    }

    val revBaru = VerdantCore.createGuideRevision(
      parentIds = List(headRev.id),
      snapshot = snapBaru,
      author = "Ir. Budi Santoso",
      changeNotes = "Penyesuaian nutrisi pembungaan musim hujan",
      timestamp = System.currentTimeMillis()
    )
    db.saveRevision(guide.id, revBaru)

    // 2. Mandor tinjau apa yang berubah
    val diff = VerdantCore.computeReadableDiff(headRev.snapshot, revBaru.snapshot)
    assertEquals(diff.totalChanges, 1)
    val change = diff.changes.head
    assertEquals(change.changeType, "Diubah")
    assert(change.humanDescription.contains("dinaikkan dari 2.6 mS/cm menjadi 2.8 mS/cm"))

    // 3. Mandor setujui dan terbitkan panduan resmi baru
    val validasi = VerdantCore.validateGuideForPublishing(revBaru.snapshot)
    assert(validasi.isRight)

    val pubRes = db.publishGuide(
      guideId = guide.id,
      editionTag = "Edisi 2026.2 - Perbaikan Nutrisi",
      revisionId = revBaru.id,
      approvedBy = "H. Suwandi (Kepala Kebun)",
      notes = "Telah divalidasi dan disetujui."
    )
    assert(pubRes.isRight)

    // 4. Buka masa tanam baru yang terkunci pada edisi resmi ini
    val masaTanamRes = db.createPeriod(
      code = "MT-2026-TOMAT-02",
      guideId = guide.id,
      publishedEdition = "Edisi 2026.2 - Perbaikan Nutrisi",
      plotName = "Greenhouse Blok 2B"
    )
    assert(masaTanamRes.isRight)

    // 5. Periksa masa tanam baru terkunci pada revisi yang tepat
    val lookup = db.getPeriod("MT-2026-TOMAT-02")
    assert(lookup.isDefined)
    val (period, _, rev, pub) = lookup.get
    assertEquals(period.publishedEdition, "Edisi 2026.2 - Perbaikan Nutrisi")
    assertEquals(pub.approvedBy, "H. Suwandi (Kepala Kebun)")
    val stageBunga = rev.snapshot.stages.find(_.name == "Fase Pembungaan & Pembentukan Buah").get
    val paramEcBaru = stageBunga.parameters.find(_.name == "Konsentrasi Nutrisi EC").get
    assertEquals(paramEcBaru.targetValue, 2.8)
  }

  test("Alur End-to-End: Penerbitan edisi baru tidak mengubah masa tanam lama (Immutable Batch Traceability)") {
    // 1. Masa tanam awal MT-2026-TOMAT-01 menggunakan Edisi 2026.1
    val initialPeriodOpt = db.getPeriod("MT-2026-TOMAT-01")
    assert(initialPeriodOpt.isDefined)
    val (p1, _, rev1, pub1) = initialPeriodOpt.get
    assertEquals(p1.publishedEdition, "Edisi 2026.1 - Musim Basah")
    val initialRevId = rev1.id

    // 2. Petani mencatat kegiatan di masa tanam ini
    val act = db.addActivity("act-trace-1", p1.code, "Penyiraman", "2026-10-27", "Siram pagi", Some("100 L"), "Pak Joko")
    assert(act.isRight)

    // 3. Mandor kemudian membuat revisi baru dan mempublikasikan Edisi 2026.3
    val guide = db.getGuide(p1.guideId).get
    val snapBaru = rev1.snapshot.copy(name = "Panduan Tomat Edisi Pembaruan Musim Depan")
    val newRev = VerdantCore.createGuideRevision(List(initialRevId), snapBaru, "Mandor", "Pembaruan musim depan", System.currentTimeMillis())
    db.saveRevision(guide.id, newRev)
    val pubRes = db.publishGuide(guide.id, "Edisi 2026.3 - Musim Kering", newRev.id, "Kepala Kebun", "Catatan rilis baru")
    assert(pubRes.isRight)

    // 4. Verifikasi panduan resmi terkini sudah menjadi Edisi 2026.3
    val latestPub = db.getLatestPublishedGuide(guide.id).get
    assertEquals(latestPub.editionTag, "Edisi 2026.3 - Musim Kering")

    // 5. CRITICAL CHECK: Masa tanam lama MT-2026-TOMAT-01 TETAP terkunci pada revisi aslinya!
    val oldPeriodCheck = db.getPeriod("MT-2026-TOMAT-01").get
    assertEquals(oldPeriodCheck._1.publishedEdition, "Edisi 2026.1 - Musim Basah")
    assertEquals(oldPeriodCheck._3.id, initialRevId)
    assertEquals(oldPeriodCheck._3.snapshot.name, rev1.snapshot.name)
  }

  test("Alur Mandor: Deteksi draf menunggu peninjauan dan penolakan draf (Reject Draft)") {
    val guideId = "panduan-cabai-02"
    val headRev = db.getLatestRevision(guideId).get

    // 1. Awalnya tidak ada draf yang menunggu karena sudah terbit
    val initialDrafts = db.getDraftsAwaitingReview().filter(_._1.id == guideId)
    assertEquals(initialDrafts.length, 0)

    // 2. Mandor membuat draf perubahan
    val modifiedSnap = headRev.snapshot.copy(name = "Draf Eksperimen Dosis Pupuk Cabai")
    val draftRev = VerdantCore.createGuideRevision(List(headRev.id), modifiedSnap, "Penyuluh Lapangan", "Eksperimen pupuk", System.currentTimeMillis())
    db.saveRevision(guideId, draftRev)

    // 3. Sistem mendeteksi draf menunggu peninjauan
    val pendingDrafts = db.getDraftsAwaitingReview().filter(_._1.id == guideId)
    assertEquals(pendingDrafts.length, 1)
    assertEquals(pendingDrafts.head._2.changeNotes, "Eksperimen pupuk")

    // 4. Mandor pemeriksa menolak draf tersebut
    val rejectRes = db.rejectDraft(guideId, "Mandor Utama", "Takaran belum diuji coba di lahan")
    assert(rejectRes.isRight)

    // 5. Setelah ditolak, draf dipulihkan ke versi resmi dan tidak ada lagi draf pending
    val remainingDrafts = db.getDraftsAwaitingReview().filter(_._1.id == guideId)
    assertEquals(remainingDrafts.length, 0)
    val restoredHead = db.getLatestRevision(guideId).get
    assertEquals(restoredHead.id, headRev.id)
  }

