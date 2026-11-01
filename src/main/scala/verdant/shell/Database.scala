package verdant.shell

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet}
import verdant.core.*
import upickle.default.*

/**
 * Imperative Shell: SQLite Persistence Layer for Verdant.
 * Mengelola penyimpanan panduan budidaya, revisi historis, penerbitan resmi,
 * masa tanam, dan catatan kegiatan harian petani.
 */
class Database(dbPath: String = "verdant.db"):

  private val url = s"jdbc:sqlite:$dbPath"

  Class.forName("org.sqlite.JDBC")

  private def withConnection[A](f: Connection => A): A =
    val conn = DriverManager.getConnection(url)
    try
      val stmt = conn.createStatement()
      try
        stmt.execute("PRAGMA foreign_keys = ON;")
        stmt.execute("PRAGMA journal_mode = WAL;")
      finally
        stmt.close()
      f(conn)
    finally
      conn.close()

  def initSchema(): Unit = withConnection { conn =>
    val stmt = conn.createStatement()
    try
      stmt.execute(
        """
        CREATE TABLE IF NOT EXISTS guides (
          id TEXT PRIMARY KEY,
          name TEXT NOT NULL,
          crop_variety TEXT NOT NULL,
          created_at INTEGER NOT NULL
        );
        """
      )
      stmt.execute(
        """
        CREATE TABLE IF NOT EXISTS guide_revisions (
          id TEXT PRIMARY KEY,
          guide_id TEXT NOT NULL,
          parent_ids TEXT NOT NULL,
          snapshot_json TEXT NOT NULL,
          author TEXT NOT NULL,
          change_notes TEXT NOT NULL,
          timestamp INTEGER NOT NULL,
          FOREIGN KEY (guide_id) REFERENCES guides(id) ON DELETE CASCADE
        );
        """
      )
      stmt.execute(
        """
        CREATE TABLE IF NOT EXISTS published_guides (
          edition_tag TEXT NOT NULL,
          guide_id TEXT NOT NULL,
          revision_id TEXT NOT NULL,
          status TEXT NOT NULL,
          approved_by TEXT NOT NULL,
          notes TEXT NOT NULL,
          published_at INTEGER NOT NULL,
          PRIMARY KEY (edition_tag, guide_id),
          FOREIGN KEY (guide_id) REFERENCES guides(id) ON DELETE CASCADE,
          FOREIGN KEY (revision_id) REFERENCES guide_revisions(id) ON DELETE CASCADE
        );
        """
      )
      stmt.execute(
        """
        CREATE TABLE IF NOT EXISTS cultivation_periods (
          code TEXT PRIMARY KEY,
          guide_id TEXT NOT NULL,
          published_edition TEXT NOT NULL,
          revision_id TEXT NOT NULL,
          plot_name TEXT NOT NULL,
          crop_variety TEXT NOT NULL,
          status TEXT NOT NULL,
          start_date INTEGER NOT NULL,
          FOREIGN KEY (guide_id) REFERENCES guides(id) ON DELETE CASCADE,
          FOREIGN KEY (revision_id) REFERENCES guide_revisions(id) ON DELETE CASCADE
        );
        """
      )
      stmt.execute(
        """
        CREATE TABLE IF NOT EXISTS activities (
          id TEXT PRIMARY KEY,
          period_code TEXT NOT NULL,
          activity_type TEXT NOT NULL,
          activity_date TEXT NOT NULL,
          notes TEXT NOT NULL,
          quantity TEXT,
          farmer_name TEXT NOT NULL,
          created_at INTEGER NOT NULL,
          FOREIGN KEY (period_code) REFERENCES cultivation_periods(code) ON DELETE CASCADE
        );
        """
      )
    finally
      stmt.close()

    seedInitialDataIfEmpty()
  }

  // ==========================================================================
  // PANDUAN BUDIDAYA (CULTIVATION GUIDES)
  // ==========================================================================

  case class GuideRow(
    id: String,
    name: String,
    cropVariety: String,
    createdAt: Long
  )

  def listGuides(): List[GuideRow] = withConnection { conn =>
    val ps = conn.prepareStatement("SELECT id, name, crop_variety, created_at FROM guides ORDER BY created_at DESC")
    val rs = ps.executeQuery()
    val buf = collection.mutable.ListBuffer[GuideRow]()
    while rs.next() do
      buf += GuideRow(
        rs.getString("id"),
        rs.getString("name"),
        rs.getString("crop_variety"),
        rs.getLong("created_at")
      )
    rs.close()
    ps.close()
    buf.toList
  }

  def getGuide(id: String): Option[GuideRow] = withConnection { conn =>
    val ps = conn.prepareStatement("SELECT id, name, crop_variety, created_at FROM guides WHERE id = ?")
    ps.setString(1, id)
    val rs = ps.executeQuery()
    val res = if rs.next() then
      Some(GuideRow(
        rs.getString("id"),
        rs.getString("name"),
        rs.getString("crop_variety"),
        rs.getLong("created_at")
      ))
    else None
    rs.close()
    ps.close()
    res
  }

  def createGuide(
    id: String,
    name: String,
    cropVariety: String,
    initialSnapshot: GuideSnapshot,
    author: String
  ): Unit = withConnection { conn =>
    conn.setAutoCommit(false)
    try
      val timestamp = System.currentTimeMillis()
      val initialRevision = VerdantCore.createGuideRevision(
        parentIds = Nil,
        snapshot = initialSnapshot,
        author = author,
        changeNotes = s"Inisiasi draf awal panduan $name",
        timestamp = timestamp
      )

      val psGuide = conn.prepareStatement(
        "INSERT INTO guides (id, name, crop_variety, created_at) VALUES (?, ?, ?, ?)"
      )
      psGuide.setString(1, id)
      psGuide.setString(2, name)
      psGuide.setString(3, cropVariety)
      psGuide.setLong(4, timestamp)
      psGuide.executeUpdate()
      psGuide.close()

      val psRev = conn.prepareStatement(
        "INSERT INTO guide_revisions (id, guide_id, parent_ids, snapshot_json, author, change_notes, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)"
      )
      psRev.setString(1, initialRevision.id)
      psRev.setString(2, id)
      psRev.setString(3, write(initialRevision.parentIds))
      psRev.setString(4, write(initialRevision.snapshot))
      psRev.setString(5, initialRevision.author)
      psRev.setString(6, initialRevision.changeNotes)
      psRev.setLong(7, initialRevision.timestamp)
      psRev.executeUpdate()
      psRev.close()

      conn.commit()
    catch
      case ex: Throwable =>
        conn.rollback()
        throw ex
    finally
      conn.setAutoCommit(true)
  }

  // ==========================================================================
  // REVISI PANDUAN (REVISIONS / COMMITS)
  // ==========================================================================

  def getLatestRevision(guideId: String): Option[GuideRevision] = withConnection { conn =>
    val ps = conn.prepareStatement(
      "SELECT id, parent_ids, snapshot_json, author, change_notes, timestamp FROM guide_revisions WHERE guide_id = ? ORDER BY timestamp DESC LIMIT 1"
    )
    ps.setString(1, guideId)
    val rs = ps.executeQuery()
    val res = if rs.next() then
      val parents = read[List[String]](rs.getString("parent_ids"))
      val snapshot = read[GuideSnapshot](rs.getString("snapshot_json"))
      Some(GuideRevision(
        id = rs.getString("id"),
        parentIds = parents,
        snapshot = snapshot,
        author = rs.getString("author"),
        changeNotes = rs.getString("change_notes"),
        timestamp = rs.getLong("timestamp")
      ))
    else None
    rs.close()
    ps.close()
    res
  }

  def getRevision(revisionId: String): Option[GuideRevision] = withConnection { conn =>
    val ps = conn.prepareStatement(
      "SELECT id, parent_ids, snapshot_json, author, change_notes, timestamp FROM guide_revisions WHERE id = ?"
    )
    ps.setString(1, revisionId)
    val rs = ps.executeQuery()
    val res = if rs.next() then
      val parents = read[List[String]](rs.getString("parent_ids"))
      val snapshot = read[GuideSnapshot](rs.getString("snapshot_json"))
      Some(GuideRevision(
        id = rs.getString("id"),
        parentIds = parents,
        snapshot = snapshot,
        author = rs.getString("author"),
        changeNotes = rs.getString("change_notes"),
        timestamp = rs.getLong("timestamp")
      ))
    else None
    rs.close()
    ps.close()
    res
  }

  def listRevisionsForGuide(guideId: String): List[GuideRevision] = withConnection { conn =>
    val ps = conn.prepareStatement(
      "SELECT id, parent_ids, snapshot_json, author, change_notes, timestamp FROM guide_revisions WHERE guide_id = ? ORDER BY timestamp DESC"
    )
    ps.setString(1, guideId)
    val rs = ps.executeQuery()
    val buf = collection.mutable.ListBuffer[GuideRevision]()
    while rs.next() do
      val parents = read[List[String]](rs.getString("parent_ids"))
      val snapshot = read[GuideSnapshot](rs.getString("snapshot_json"))
      buf += GuideRevision(
        id = rs.getString("id"),
        parentIds = parents,
        snapshot = snapshot,
        author = rs.getString("author"),
        changeNotes = rs.getString("change_notes"),
        timestamp = rs.getLong("timestamp")
      )
    rs.close()
    ps.close()
    buf.toList
  }

  def getRevisionLookup(guideId: String): Map[String, GuideRevision] =
    listRevisionsForGuide(guideId).map(r => r.id -> r).toMap

  def saveRevision(guideId: String, revision: GuideRevision): Unit = withConnection { conn =>
    val ps = conn.prepareStatement(
      "INSERT OR IGNORE INTO guide_revisions (id, guide_id, parent_ids, snapshot_json, author, change_notes, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)"
    )
    ps.setString(1, revision.id)
    ps.setString(2, guideId)
    ps.setString(3, write(revision.parentIds))
    ps.setString(4, write(revision.snapshot))
    ps.setString(5, revision.author)
    ps.setString(6, revision.changeNotes)
    ps.setLong(7, revision.timestamp)
    ps.executeUpdate()
    ps.close()
  }

  // ==========================================================================
  // PANDUAN RESMI TERBIT (PUBLISHED GUIDES / RELEASES)
  // ==========================================================================

  def listPublishedGuides(): List[(PublishedGuide, String)] = withConnection { conn =>
    val ps = conn.prepareStatement(
      "SELECT p.edition_tag, p.guide_id, p.revision_id, p.status, p.approved_by, p.notes, p.published_at, g.name FROM published_guides p JOIN guides g ON g.id = p.guide_id ORDER BY p.published_at DESC"
    )
    val rs = ps.executeQuery()
    val buf = collection.mutable.ListBuffer[(PublishedGuide, String)]()
    while rs.next() do
      val pub = PublishedGuide(
        rs.getString("edition_tag"),
        rs.getString("guide_id"),
        rs.getString("revision_id"),
        rs.getString("status"),
        rs.getString("approved_by"),
        rs.getString("notes"),
        rs.getLong("published_at")
      )
      buf += ((pub, rs.getString("name")))
    rs.close()
    ps.close()
    buf.toList
  }

  def getLatestPublishedGuide(guideId: String): Option[PublishedGuide] = withConnection { conn =>
    val ps = conn.prepareStatement(
      "SELECT edition_tag, guide_id, revision_id, status, approved_by, notes, published_at FROM published_guides WHERE guide_id = ? ORDER BY published_at DESC LIMIT 1"
    )
    ps.setString(1, guideId)
    val rs = ps.executeQuery()
    val res = if rs.next() then
      Some(PublishedGuide(
        rs.getString("edition_tag"),
        rs.getString("guide_id"),
        rs.getString("revision_id"),
        rs.getString("status"),
        rs.getString("approved_by"),
        rs.getString("notes"),
        rs.getLong("published_at")
      ))
    else None
    rs.close()
    ps.close()
    res
  }

  def publishGuide(
    guideId: String,
    editionTag: String,
    revisionId: String,
    approvedBy: String,
    notes: String
  ): Either[String, PublishedGuide] = withConnection { conn =>
    val checkPs = conn.prepareStatement("SELECT 1 FROM published_guides WHERE edition_tag = ? AND guide_id = ?")
    checkPs.setString(1, editionTag)
    checkPs.setString(2, guideId)
    val exists = checkPs.executeQuery().next()
    checkPs.close()

    if exists then
      Left(s"Nama edisi '$editionTag' sudah pernah diterbitkan sebelumnya.")
    else
      val ts = System.currentTimeMillis()
      val insPs = conn.prepareStatement(
        "INSERT INTO published_guides (edition_tag, guide_id, revision_id, status, approved_by, notes, published_at) VALUES (?, ?, ?, 'DITERBITKAN', ?, ?, ?)"
      )
      insPs.setString(1, editionTag)
      insPs.setString(2, guideId)
      insPs.setString(3, revisionId)
      insPs.setString(4, approvedBy)
      insPs.setString(5, notes)
      insPs.setLong(6, ts)
      insPs.executeUpdate()
      insPs.close()

      Right(PublishedGuide(editionTag, guideId, revisionId, "DITERBITKAN", approvedBy, notes, ts))
  }

  def getDraftsAwaitingReview(): List[(GuideRow, GuideRevision, Option[PublishedGuide])] =
    listGuides().flatMap { guide =>
      val latestRevOpt = getLatestRevision(guide.id)
      val latestPubOpt = getLatestPublishedGuide(guide.id)
      latestRevOpt.flatMap { latestRev =>
        latestPubOpt match
          case None =>
            Some((guide, latestRev, None))
          case Some(pub) if pub.revisionId != latestRev.id =>
            Some((guide, latestRev, Some(pub)))
          case _ =>
            None
      }
    }

  def listPublishedGuidesWithSnapshots(): List[(GuideRow, PublishedGuide, GuideSnapshot)] =
    listGuides().flatMap { guide =>
      getLatestPublishedGuide(guide.id).flatMap { pub =>
        getRevision(pub.revisionId).map { rev =>
          (guide, pub, rev.snapshot)
        }
      }
    }

  def rejectDraft(
    guideId: String,
    rejectedBy: String,
    reason: String
  ): Either[String, Unit] = withConnection { conn =>
    getLatestPublishedGuide(guideId) match
      case None =>
        Left("Panduan ini belum memiliki edisi resmi yang pernah terbit. Draf awal tidak dapat dibatalkan ke edisi sebelumnya.")
      case Some(pub) =>
        val currHead = getLatestRevision(guideId)
        if currHead.map(_.id).contains(pub.revisionId) then
          Left("Tidak ada draf yang berbeda dari edisi resmi saat ini.")
        else
          val ps = conn.prepareStatement(
            """
            DELETE FROM guide_revisions 
            WHERE guide_id = ? 
              AND id != ? 
              AND id NOT IN (SELECT revision_id FROM published_guides)
              AND id NOT IN (SELECT revision_id FROM cultivation_periods)
            """
          )
          ps.setString(1, guideId)
          ps.setString(2, pub.revisionId)
          ps.executeUpdate()
          ps.close()
          Right(())
  }

  // ==========================================================================
  // MASA TANAM (CULTIVATION PERIODS)
  // ==========================================================================

  def listPeriods(): List[(CultivationPeriod, String)] = withConnection { conn =>
    val ps = conn.prepareStatement(
      "SELECT cp.code, cp.guide_id, cp.published_edition, cp.revision_id, cp.plot_name, cp.crop_variety, cp.status, cp.start_date, g.name FROM cultivation_periods cp JOIN guides g ON g.id = cp.guide_id ORDER BY cp.start_date DESC"
    )
    val rs = ps.executeQuery()
    val buf = collection.mutable.ListBuffer[(CultivationPeriod, String)]()
    while rs.next() do
      val cp = CultivationPeriod(
        rs.getString("code"),
        rs.getString("guide_id"),
        rs.getString("published_edition"),
        rs.getString("revision_id"),
        rs.getString("plot_name"),
        rs.getString("crop_variety"),
        rs.getString("status"),
        rs.getLong("start_date")
      )
      buf += ((cp, rs.getString("name")))
    rs.close()
    ps.close()
    buf.toList
  }

  def getPeriod(code: String): Option[(CultivationPeriod, GuideRow, GuideRevision, PublishedGuide)] = withConnection { conn =>
    val ps = conn.prepareStatement(
      """
      SELECT cp.code, cp.guide_id, cp.published_edition, cp.revision_id, cp.plot_name, cp.crop_variety, cp.status, cp.start_date,
             g.id as g_id, g.name as g_name, g.crop_variety as g_crop, g.created_at as g_created,
             r.parent_ids, r.snapshot_json, r.author as r_author, r.change_notes, r.timestamp as r_timestamp,
             p.status as p_status, p.approved_by, p.notes as p_notes, p.published_at
      FROM cultivation_periods cp
      JOIN guides g ON g.id = cp.guide_id
      JOIN guide_revisions r ON r.id = cp.revision_id
      JOIN published_guides p ON p.guide_id = cp.guide_id AND p.edition_tag = cp.published_edition
      WHERE cp.code = ?
      """
    )
    ps.setString(1, code)
    val rs = ps.executeQuery()
    val res = if rs.next() then
      val cp = CultivationPeriod(
        rs.getString("code"),
        rs.getString("guide_id"),
        rs.getString("published_edition"),
        rs.getString("revision_id"),
        rs.getString("plot_name"),
        rs.getString("crop_variety"),
        rs.getString("status"),
        rs.getLong("start_date")
      )
      val g = GuideRow(
        rs.getString("g_id"),
        rs.getString("g_name"),
        rs.getString("g_crop"),
        rs.getLong("g_created")
      )
      val parents = read[List[String]](rs.getString("parent_ids"))
      val snap = read[GuideSnapshot](rs.getString("snapshot_json"))
      val rev = GuideRevision(
        rs.getString("revision_id"),
        parents,
        snap,
        rs.getString("r_author"),
        rs.getString("change_notes"),
        rs.getLong("r_timestamp")
      )
      val pub = PublishedGuide(
        rs.getString("published_edition"),
        rs.getString("guide_id"),
        rs.getString("revision_id"),
        rs.getString("p_status"),
        rs.getString("approved_by"),
        rs.getString("p_notes"),
        rs.getLong("published_at")
      )
      Some((cp, g, rev, pub))
    else None
    rs.close()
    ps.close()
    res
  }

  def createPeriod(
    code: String,
    guideId: String,
    publishedEdition: String,
    plotName: String
  ): Either[String, CultivationPeriod] = withConnection { conn =>
    // Pastikan edisi panduan valid & ambil revisionId
    val psPub = conn.prepareStatement("SELECT revision_id FROM published_guides WHERE edition_tag = ? AND guide_id = ?")
    psPub.setString(1, publishedEdition)
    psPub.setString(2, guideId)
    val rsPub = psPub.executeQuery()
    val revIdOpt = if rsPub.next() then Some(rsPub.getString("revision_id")) else None
    rsPub.close()
    psPub.close()

    revIdOpt match
      case None =>
        Left(s"Edisi panduan '$publishedEdition' belum diterbitkan untuk panduan ini.")
      case Some(revisionId) =>
        // Periksa keunikan kode masa tanam
        val checkPs = conn.prepareStatement("SELECT 1 FROM cultivation_periods WHERE code = ?")
        checkPs.setString(1, code)
        val exists = checkPs.executeQuery().next()
        checkPs.close()

        if exists then
          Left(s"Kode masa tanam '$code' sudah ada. Gunakan kode lain.")
        else
          // Ambil nama komoditas
          val gPs = conn.prepareStatement("SELECT crop_variety FROM guides WHERE id = ?")
          gPs.setString(1, guideId)
          val gRs = gPs.executeQuery()
          val variety = if gRs.next() then gRs.getString("crop_variety") else "Tanaman"
          gRs.close()
          gPs.close()

          val ts = System.currentTimeMillis()
          val insPs = conn.prepareStatement(
            "INSERT INTO cultivation_periods (code, guide_id, published_edition, revision_id, plot_name, crop_variety, status, start_date) VALUES (?, ?, ?, ?, ?, ?, 'BERJALAN', ?)"
          )
          insPs.setString(1, code)
          insPs.setString(2, guideId)
          insPs.setString(3, publishedEdition)
          insPs.setString(4, revisionId)
          insPs.setString(5, plotName)
          insPs.setString(6, variety)
          insPs.setLong(7, ts)
          insPs.executeUpdate()
          insPs.close()

          Right(CultivationPeriod(code, guideId, publishedEdition, revisionId, plotName, variety, "BERJALAN", ts))
  }

  // ==========================================================================
  // CATATAN KEGIATAN BUDIDAYA (CULTIVATION ACTIVITIES)
  // ==========================================================================

  def listActivities(periodCode: Option[String] = None): List[CultivationActivity] = withConnection { conn =>
    val query = periodCode match
      case Some(_) => "SELECT id, period_code, activity_type, activity_date, notes, quantity, farmer_name, created_at FROM activities WHERE period_code = ? ORDER BY activity_date DESC, created_at DESC"
      case None    => "SELECT id, period_code, activity_type, activity_date, notes, quantity, farmer_name, created_at FROM activities ORDER BY activity_date DESC, created_at DESC"

    val ps = conn.prepareStatement(query)
    periodCode.foreach(pc => ps.setString(1, pc))
    val rs = ps.executeQuery()
    val buf = collection.mutable.ListBuffer[CultivationActivity]()
    while rs.next() do
      buf += CultivationActivity(
        id = rs.getString("id"),
        periodCode = rs.getString("period_code"),
        activityType = rs.getString("activity_type"),
        activityDate = rs.getString("activity_date"),
        notes = rs.getString("notes"),
        quantity = Option(rs.getString("quantity")),
        farmerName = rs.getString("farmer_name"),
        createdAt = rs.getLong("created_at")
      )
    rs.close()
    ps.close()
    buf.toList
  }

  def addActivity(
    id: String,
    periodCode: String,
    activityType: String,
    activityDate: String,
    notes: String,
    quantity: Option[String],
    farmerName: String
  ): Either[String, CultivationActivity] = withConnection { conn =>
    val checkPs = conn.prepareStatement("SELECT 1 FROM cultivation_periods WHERE code = ?")
    checkPs.setString(1, periodCode)
    val exists = checkPs.executeQuery().next()
    checkPs.close()

    if !exists then
      Left(s"Masa tanam '$periodCode' tidak ditemukan.")
    else
      val ts = System.currentTimeMillis()
      val ps = conn.prepareStatement(
        "INSERT INTO activities (id, period_code, activity_type, activity_date, notes, quantity, farmer_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
      )
      ps.setString(1, id)
      ps.setString(2, periodCode)
      ps.setString(3, activityType)
      ps.setString(4, activityDate)
      ps.setString(5, notes)
      ps.setString(6, quantity.orNull)
      ps.setString(7, farmerName)
      ps.setLong(8, ts)
      ps.executeUpdate()
      ps.close()

      Right(CultivationActivity(id, periodCode, activityType, activityDate, notes, quantity, farmerName, ts))
  }

  // ==========================================================================
  // DATA AWAL (REALISTIC INDONESIAN SEED DATA)
  // ==========================================================================

  private def seedInitialDataIfEmpty(): Unit = withConnection { conn =>
    val checkPs = conn.createStatement()
    val rs = checkPs.executeQuery("SELECT COUNT(*) FROM guides")
    val count = if rs.next() then rs.getInt(1) else 0
    rs.close()
    checkPs.close()

    if count == 0 then
      // 1. Panduan Tomat Hidroponik Unggul
      val tomatGuide = GuideSnapshot(
        name = "Panduan Budidaya Tomat Hidroponik Unggul",
        cropVariety = "Tomat Beef Roma",
        stages = List(
          CultivationStage(
            name = "Persemaian & Pembibitan",
            durationDays = 14,
            instructions = "Jaga kelembapan media rockwool dengan menyemprotkan air bersih dua kali sehari. Jauhkan dari sinar matahari terik langsung pada 4 hari pertama hingga benih berkecambah.",
            parameters = List(
              AgronomicParam("Konsentrasi Nutrisi EC", 1.2, 1.0, 1.5, "mS/cm"),
              AgronomicParam("Target pH Air", 5.8, 5.5, 6.2, "pH"),
              AgronomicParam("Suhu Lingkungan", 24.0, 20.0, 28.0, "°C")
            )
          ),
          CultivationStage(
            name = "Fase Vegetatif Aktif",
            durationDays = 28,
            instructions = "Pindahkan bibit ke talang hidroponik. Berikan nutrisi AB Mix daun secara teratur. Lakukan pemangkasan tunas air (wiwil) setiap 7 hari sekali agar batang utama kokoh.",
            parameters = List(
              AgronomicParam("Konsentrasi Nutrisi EC", 2.2, 1.8, 2.5, "mS/cm"),
              AgronomicParam("Target pH Air", 6.0, 5.8, 6.4, "pH"),
              AgronomicParam("Dosis Pupuk NPK", 180.0, 150.0, 220.0, "ppm"),
              AgronomicParam("Volume Siram Harian", 2.5, 2.0, 3.2, "L/tanaman")
            )
          ),
          CultivationStage(
            name = "Fase Pembungaan & Pembentukan Buah",
            durationDays = 35,
            instructions = "Tingkatkan unsur Kalium untuk merangsang bunga menjadi buah dan mencegah kerontokan. Jaga sirkulasi udara greenhouse agar penyerbukan lebah atau angin berjalan optimal.",
            parameters = List(
              AgronomicParam("Konsentrasi Nutrisi EC", 2.6, 2.2, 3.0, "mS/cm"),
              AgronomicParam("Target pH Air", 6.0, 5.8, 6.5, "pH"),
              AgronomicParam("Dosis Kalium K2O", 320.0, 280.0, 360.0, "ppm")
            )
          ),
          CultivationStage(
            name = "Pematangan Buah & Panen",
            durationDays = 21,
            instructions = "Kurangi sedikit frekuensi penyiraman untuk meningkatkan kemanisan (kadar Brix) buah tomat. Petik buah saat semburat warna merah mencapai 70-80% pada pagi hari.",
            parameters = List(
              AgronomicParam("Konsentrasi Nutrisi EC", 2.5, 2.0, 3.0, "mS/cm"),
              AgronomicParam("Target pH Air", 6.0, 5.8, 6.4, "pH"),
              AgronomicParam("Standar Kemanisan Brix", 8.5, 7.5, 10.5, "°Bx")
            )
          )
        )
      )

      createGuide(
        id = "panduan-tomat-01",
        name = "Panduan Budidaya Tomat Hidroponik Unggul",
        cropVariety = "Tomat Beef Roma",
        initialSnapshot = tomatGuide,
        author = "Ir. Budi Santoso (Penyuluh Pertanian Utama)"
      )

      // Terbitkan versi resmi awal Edisi 2026.1
      val tomatRev = getLatestRevision("panduan-tomat-01").get
      publishGuide(
        guideId = "panduan-tomat-01",
        editionTag = "Edisi 2026.1 - Musim Basah",
        revisionId = tomatRev.id,
        approvedBy = "H. Suwandi (Kepala Kebun)",
        notes = "Telah divalidasi sesuai standar Good Agricultural Practices (GAP) hortikultura."
      )

      // 2. Panduan Cabai Rawit Merah Intensif
      val cabaiGuide = GuideSnapshot(
        name = "SOP Budidaya Cabai Rawit Merah Intensif",
        cropVariety = "Cabai Rawit Merah Varietas Ori 212",
        stages = List(
          CultivationStage(
            name = "Persemaian & Media Semai",
            durationDays = 21,
            instructions = "Gunakan tray semai dengan campuran tanah subur dan kompos halus 1:1. Tutup dengan sungkup plastik transparan.",
            parameters = List(
              AgronomicParam("Kelembapan Media Semai", 70.0, 60.0, 80.0, "%"),
              AgronomicParam("Target pH Tanah", 6.2, 5.8, 6.8, "pH")
            )
          ),
          CultivationStage(
            name = "Fase Vegetatif Lahan Terbuka",
            durationDays = 30,
            instructions = "Tanam bibit pada bedengan berpupuk dasar. Lakukan penyemprotan insektisida nabati atau perangkap lalat buah sejak awal.",
            parameters = List(
              AgronomicParam("Dosis Pupuk NPK Susulan", 50.0, 40.0, 60.0, "kg/ha"),
              AgronomicParam("Target pH Tanah", 6.5, 6.0, 7.0, "pH")
            )
          ),
          CultivationStage(
            name = "Pembungaan & Panen Berkala",
            durationDays = 60,
            instructions = "Panen berkala setiap 5-7 hari sekali saat cabai sudah berwarna merah merata. Petik beserta tangkainya.",
            parameters = List(
              AgronomicParam("Dosis Pupuk Kalium KNO3", 30.0, 20.0, 40.0, "kg/ha"),
              AgronomicParam("Target Panen per Siklus", 150.0, 100.0, 250.0, "kg")
            )
          )
        )
      )

      createGuide(
        id = "panduan-cabai-02",
        name = "SOP Budidaya Cabai Rawit Merah Intensif",
        cropVariety = "Cabai Rawit Merah Varietas Ori 212",
        initialSnapshot = cabaiGuide,
        author = "Hj. Nuraini, S.P. (Mandor Kebun Hortikultura)"
      )

      val cabaiRev = getLatestRevision("panduan-cabai-02").get
      publishGuide(
        guideId = "panduan-cabai-02",
        editionTag = "Edisi 2026.1 - Standar GAP",
        revisionId = cabaiRev.id,
        approvedBy = "H. Suwandi (Kepala Kebun)",
        notes = "Disetujui untuk musim tanam gadu di lahan terbuka."
      )

      // 3. Masa Tanam Aktif
      createPeriod(
        code = "MT-2026-TOMAT-01",
        guideId = "panduan-tomat-01",
        publishedEdition = "Edisi 2026.1 - Musim Basah",
        plotName = "Greenhouse Blok 2A"
      )

      createPeriod(
        code = "MT-2026-CABAI-02",
        guideId = "panduan-cabai-02",
        publishedEdition = "Edisi 2026.1 - Standar GAP",
        plotName = "Lahan Terbuka Petak Selatan"
      )

      // 4. Catatan Kegiatan Lapangan
      addActivity(
        id = "act-01",
        periodCode = "MT-2026-TOMAT-01",
        activityType = "Penyiraman",
        activityDate = "2026-10-26",
        notes = "Pemberian nutrisi AB Mix pagi hari. Debit pompa berjalan normal.",
        quantity = Some("200 Liter"),
        farmerName = "Pak Joko (Petani Pelaksana)"
      )

      addActivity(
        id = "act-02",
        periodCode = "MT-2026-TOMAT-01",
        activityType = "Pemangkasan",
        activityDate = "2026-10-26",
        notes = "Pewiwilan tunas air ketiak daun pada 120 pohon baris A dan B.",
        quantity = None,
        farmerName = "Pak Joko (Petani Pelaksana)"
      )

      addActivity(
        id = "act-03",
        periodCode = "MT-2026-TOMAT-01",
        activityType = "Pengendalian Hama",
        activityDate = "2026-10-27",
        notes = "Pembersihan yellow sticky trap dari kutu kebul. Kondisi daun hijau bersih.",
        quantity = Some("10 Lembar Trap"),
        farmerName = "Bu Siti (Petugas Kebun)"
      )

      addActivity(
        id = "act-04",
        periodCode = "MT-2026-CABAI-02",
        activityType = "Pemupukan",
        activityDate = "2026-10-25",
        notes = "Pemupukan susulan NPK 16-16-16 dikocorkan pada lubang tanam samping mulsa.",
        quantity = Some("50 kg NPK"),
        farmerName = "Pak Slamet (Petani Penggarap)"
      )
  }
