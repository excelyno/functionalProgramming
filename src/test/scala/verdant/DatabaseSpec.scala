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
