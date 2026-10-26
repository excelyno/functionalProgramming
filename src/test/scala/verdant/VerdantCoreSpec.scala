package verdant

import munit.FunSuite
import verdant.core.*

class VerdantCoreSpec extends FunSuite:

  val paramEC: AgronomicParam = AgronomicParam("Konsentrasi Nutrisi EC", 2.2, 1.8, 2.6, "mS/cm")
  val paramPH: AgronomicParam = AgronomicParam("Target pH Air", 6.0, 5.5, 6.5, "pH")
  val paramNPK: AgronomicParam = AgronomicParam("Dosis Pupuk NPK", 180.0, 150.0, 220.0, "ppm")

  val stageSemai: CultivationStage = CultivationStage(
    name = "Persemaian & Bibit",
    durationDays = 14,
    instructions = "Siram air bersih dua kali sehari.",
    parameters = List(paramEC.copy(targetValue = 1.2, minValue = 1.0, maxValue = 1.5), paramPH)
  )

  val stageVegetatif: CultivationStage = CultivationStage(
    name = "Fase Vegetatif",
    durationDays = 28,
    instructions = "Berikan nutrisi daun teratur dan wiwil tunas air.",
    parameters = List(paramEC, paramPH, paramNPK)
  )

  val sampleGuide: GuideSnapshot = GuideSnapshot(
    name = "Panduan Budidaya Tomat Unggul",
    cropVariety = "  tomat beef roma  ",
    stages = List(stageSemai, stageVegetatif)
  )

  // 1. Immutable Case Classes
  test("Kasus kelas abadi (immutable case classes) mempertahankan nilai dan mendukung copy") {
    val stageBaru = stageSemai.copy(durationDays = 20)
    assertEquals(stageSemai.durationDays, 14) // Asli tidak berubah
    assertEquals(stageBaru.durationDays, 20)
    assertEquals(paramEC.targetValue, 2.2)
  }

  // 2. First-class Function Values & Lambdas
  test("Nilai fungsi kelas-pertama dan lambda memvalidasi batas aman takaran") {
    assert(VerdantCore.isSafeParam(paramEC))
    val paramBahaya = paramEC.copy(targetValue = 4.0) // melebihi batas maks 2.6
    assert(!VerdantCore.isSafeParam(paramBahaya))
    assert(VerdantCore.isPositiveStage(stageSemai))
  }

  // 3. Komposisi Fungsi Menggunakan andThen
  test("Komposisi fungsi (andThen) membersihkan dan memformat nama komoditas") {
    val namaBersih = VerdantCore.formatCropName("  cabai rawit merah ori 212  ")
    assertEquals(namaBersih, "Cabai rawit merah ori 212")

    val estimasiAir = VerdantCore.estimateDailyIrrigation(100.0)
    assertEquals(estimasiAir, "220.0 Liter/hari")
  }

  // 4. Custom Higher-Order Functions: transformStages & auditParameters
  test("Fungsi tingkat tinggi transformStages mengubah seluruh tahap secara murni") {
    val panduanDiubah = VerdantCore.transformStages(sampleGuide) { s =>
      s.copy(durationDays = s.durationDays + 2)
    }
    assertEquals(panduanDiubah.stages.map(_.durationDays), List(16, 30))
    assertEquals(sampleGuide.stages.map(_.durationDays), List(14, 28)) // Asli tetap murni
  }

  test("Fungsi tingkat tinggi auditParameters mendeteksi anjuran yang melampaui batas aman") {
    val panduanBuruk = sampleGuide.copy(stages = List(
      stageSemai.copy(parameters = List(paramEC.copy(targetValue = 9.9)))
    ))
    val pelanggaran = VerdantCore.auditParameters(panduanBuruk) { p =>
      if VerdantCore.isSafeParam(p) then Right(())
      else Left(s"Takaran ${p.targetValue} ${p.unit} tidak aman.")
    }
    assertEquals(pelanggaran.length, 1)
    assert(pelanggaran.head.contains("9.9"))
  }

  // 5. Rantai 3 Operasi Koleksi, foldLeft, map, dan groupBy pada summarizeGuide
  test("summarizeGuide menghitung agregat hari dan satuan dengan foldLeft dan groupBy") {
    val ringkasan = VerdantCore.summarizeGuide(sampleGuide)
    assertEquals(ringkasan.totalStages, 2)
    assertEquals(ringkasan.totalDurationDays, 42) // 14 + 28
    assertEquals(ringkasan.totalParameters, 5) // 2 + 3
    assertEquals(ringkasan.paramsByUnit("mS/cm"), 2)
    assertEquals(ringkasan.paramsByUnit("pH"), 2)
    assertEquals(ringkasan.paramsByUnit("ppm"), 1)
    assertEquals(ringkasan.averageValueByUnit("mS/cm"), 1.7) // (1.2 + 2.2) / 2
  }

  // 6. Rangkuman Kegiatan Petani dengan groupBy dan foldLeft
  test("summarizeActivities mengelompokkan dan menghitung frekuensi kegiatan petani") {
    val activities = List(
      CultivationActivity("1", "MT-01", "Penyiraman", "2026-10-25", "Pagi", None, "Joko", 100L),
      CultivationActivity("2", "MT-01", "Penyiraman", "2026-10-26", "Pagi", None, "Joko", 200L),
      CultivationActivity("3", "MT-01", "Pemupukan", "2026-10-26", "NPK", Some("50 kg"), "Siti", 300L)
    )
    val counts = VerdantCore.summarizeActivities(activities)
    assertEquals(counts("Penyiraman"), 2)
    assertEquals(counts("Pemupukan"), 1)
  }

  // 7. Pembuatan Revisi Deterministik (createGuideRevision)
  test("createGuideRevision menghasilkan ID revisi deterministik") {
    val r1 = VerdantCore.createGuideRevision(Nil, sampleGuide, "Ir. Budi", "Draf Awal", 1000L)
    val r2 = VerdantCore.createGuideRevision(Nil, sampleGuide, "Ir. Budi", "Draf Awal", 1000L)
    assertEquals(r1.id, r2.id)
    assertEquals(r1.author, "Ir. Budi")
  }

  // 8. Perbedaan yang Mudah Dibaca Petani ("Apa yang berubah?")
  test("computeReadableDiff menghasilkan kalimat perubahan dalam bahasa Indonesia yang ramah") {
    val panduanBaru = sampleGuide.copy(
      stages = List(
        stageSemai,
        stageVegetatif.copy(
          parameters = List(
            paramEC.copy(targetValue = 2.4), // 2.2 -> 2.4 (dinaikkan)
            paramPH,
            paramNPK
          )
        )
      )
    )

    val diffSummary = VerdantCore.computeReadableDiff(sampleGuide, panduanBaru)
    assertEquals(diffSummary.totalChanges, 1)
    val pChange = diffSummary.changes.head
    assertEquals(pChange.changeType, "Diubah")
    assertEquals(pChange.oldValue, Some(2.2))
    assertEquals(pChange.newValue, Some(2.4))
    assert(pChange.humanDescription.contains("dinaikkan dari 2.2 mS/cm menjadi 2.4 mS/cm"))
  }

  // 9. Pemulihan Versi Panduan Sebelumnya (Revert)
  test("restoreGuideRevision membuat revisi baru yang memulihkan draf sebelumnya") {
    val revAwal = VerdantCore.createGuideRevision(Nil, sampleGuide, "Pak Budi", "Revisi 1", 100L)
    val panduanDiubah = sampleGuide.copy(name = "Panduan Hasil Coba-Coba")
    val revKedua = VerdantCore.createGuideRevision(List(revAwal.id), panduanDiubah, "Mandor", "Revisi 2 Salah", 200L)

    val hasilPulih = VerdantCore.restoreGuideRevision(revAwal, revKedua, "Kepala Kebun", 300L)
    assert(hasilPulih.isRight)
    val revPulih = hasilPulih.toOption.get
    assertEquals(revPulih.parentIds, List(revKedua.id))
    assertEquals(revPulih.snapshot.name, "Panduan Budidaya Tomat Unggul")
  }

  // 10. Fungsi Rekursif: Silsilah Riwayat Revisi (getRevisionLineage)
  test("getRevisionLineage menelusuri riwayat revisi secara rekursif hingga ke awal") {
    val r0 = VerdantCore.createGuideRevision(Nil, sampleGuide, "Awal", "r0", 10L)
    val r1 = VerdantCore.createGuideRevision(List(r0.id), sampleGuide, "Penyuluh", "r1", 20L)
    val r2 = VerdantCore.createGuideRevision(List(r1.id), sampleGuide, "Mandor", "r2", 30L)

    val lookup = Map(r0.id -> r0, r1.id -> r1, r2.id -> r2)
    val lineage = VerdantCore.getRevisionLineage(r2.id, lookup)
    assertEquals(lineage.map(_.id), List(r2.id, r1.id, r0.id))
  }

  // 11. Validasi Kelayakan Terbit Panduan
  test("validateGuideForPublishing menolak panduan tanpa tahap atau durasi 0 hari") {
    val panduanKosong = sampleGuide.copy(stages = Nil)
    assert(VerdantCore.validateGuideForPublishing(panduanKosong).isLeft)

    val tahapNol = stageSemai.copy(durationDays = 0)
    val panduanNolHari = sampleGuide.copy(stages = List(tahapNol))
    assert(VerdantCore.validateGuideForPublishing(panduanNolHari).isLeft)

    assert(VerdantCore.validateGuideForPublishing(sampleGuide).isRight)
  }

  // 12. Pengecekan Kepatuhan Takaran Kegiatan Lapangan
  test("checkActivityCompliance mendeteksi takaran pupuk yang melebihi batas anjuran panduan") {
    val actNormal = CultivationActivity("1", "MT-01", "Pemupukan NPK", "2026-10-27", "Normal", Some("180 ppm"), "Joko", 100L)
    assertEquals(VerdantCore.checkActivityCompliance(actNormal, List(paramNPK)), None)

    val actBerlebih = CultivationActivity("2", "MT-01", "Pemupukan NPK", "2026-10-27", "Berlebih", Some("300 ppm"), "Joko", 200L)
    val warning = VerdantCore.checkActivityCompliance(actBerlebih, List(paramNPK))
    assert(warning.isDefined)
    assert(warning.get.contains("di luar batas anjuran panduan"))
  }
