package verdant.core

import java.security.MessageDigest

/**
 * Functional Core for Verdant.
 * 
 * Strict purity guarantees:
 * - 100% pure functions, no side-effects (no DB, no network, no clock)
 * - Deterministic inputs and outputs
 * - Strict adherence to the Functional Programming rubric
 */
object VerdantCore:

  // ==========================================================================
  // 1. FIRST-CLASS FUNCTION VALUES & FUNCTION COMPOSITION (andThen / compose)
  // ==========================================================================

  /** Nilai fungsi kelas-pertama: memeriksa apakah target anjuran berada dalam rentang aman */
  val isSafeParam: AgronomicParam => Boolean = (p: AgronomicParam) =>
    p.minValue <= p.targetValue && p.targetValue <= p.maxValue

  /** Nilai fungsi kelas-pertama: memeriksa apakah durasi tahap bernilai positif */
  val isPositiveStage: CultivationStage => Boolean = (s: CultivationStage) =>
    s.durationDays > 0

  // Komposisi fungsi menggunakan andThen: membersihkan dan memformat nama komoditas
  val cleanText: String => String = (s: String) => s.trim
  val capitalizeFirst: String => String = (s: String) =>
    if s.isEmpty then ""
    else s.substring(0, 1).toUpperCase + s.substring(1)

  /** Pipa komposisi fungsi: memangkas spasi lalu mengkapitalkan huruf awal */
  val formatCropName: String => String = cleanText.andThen(capitalizeFirst)

  // Komposisi fungsi kedua: perkiraan kebutuhan air harian
  val calculateDailyWaterNeed: Double => Double = (plantCount: Double) => plantCount * 2.2
  val formatWaterVolume: Double => String = (volume: Double) => f"$volume%.1f Liter/hari"

  /** Pipa komposisi fungsi: menghitung estimasi kebutuhan air tanaman */
  val estimateDailyIrrigation: Double => String = calculateDailyWaterNeed.andThen(formatWaterVolume)

  // ==========================================================================
  // 2. CUSTOM HIGHER-ORDER FUNCTIONS
  // ==========================================================================

  /**
   * Custom Higher-Order Function:
   * Mengubah setiap tahap budidaya secara murni menggunakan fungsi transformasi `f`.
   */
  def transformStages(guide: GuideSnapshot)(f: CultivationStage => CultivationStage): GuideSnapshot =
    guide.copy(stages = guide.stages.map(f))

  /**
   * Custom Higher-Order Function:
   * Memeriksa seluruh takaran parameter dalam panduan terhadap aturan kepatuhan `rule`.
   */
  def auditParameters(guide: GuideSnapshot)(rule: AgronomicParam => Either[String, Unit]): List[String] =
    guide.stages.flatMap { stage =>
      stage.parameters.flatMap { param =>
        rule(param) match
          case Left(violation) => Some(s"Tahap '${stage.name}', Parameter '${param.name}': $violation")
          case Right(_)        => None
      }
    }

  // ==========================================================================
  // 3. PURE DOMAIN FUNCTIONS
  // ==========================================================================

  /**
   * Pure Function 1: Membuat revisi panduan baru secara murni dengan ID deterministik.
   */
  def createGuideRevision(
    parentIds: List[String],
    snapshot: GuideSnapshot,
    author: String,
    changeNotes: String,
    timestamp: Long
  ): GuideRevision =
    val seed = s"${parentIds.sorted.mkString(",")}:${snapshot.name}:${snapshot.cropVariety}:${snapshot.stages.length}:$author:$changeNotes:$timestamp"
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(seed.getBytes("UTF-8")).take(8).map("%02x".format(_)).mkString
    GuideRevision(
      id = hash,
      parentIds = parentIds,
      snapshot = snapshot,
      author = author.trim,
      changeNotes = changeNotes.trim,
      timestamp = timestamp
    )

  /**
   * Pure Function 2: Menghitung perbedaan yang mudah dibaca petani/mandor ("Apa yang berubah?").
   */
  def computeReadableDiff(oldSnap: GuideSnapshot, newSnap: GuideSnapshot): ChangeSummary =
    val oldStagesMap = oldSnap.stages.map(s => s.name -> s).toMap
    val newStagesMap = newSnap.stages.map(s => s.name -> s).toMap

    val allStageNames = (oldStagesMap.keySet ++ newStagesMap.keySet).toList.sorted

    val changesList = allStageNames.flatMap { stageName =>
      (oldStagesMap.get(stageName), newStagesMap.get(stageName)) match
        case (Some(oldS), None) =>
          oldS.parameters.map { p =>
            ParamChange(
              stageName = stageName,
              paramName = p.name,
              changeType = "Dihapus",
              oldValue = Some(p.targetValue),
              newValue = None,
              unit = p.unit,
              humanDescription = s"Tahap '${stageName}' beserta anjuran ${p.name} (${p.targetValue} ${p.unit}) dihapus dari panduan."
            )
          }

        case (None, Some(newS)) =>
          newS.parameters.map { p =>
            ParamChange(
              stageName = stageName,
              paramName = p.name,
              changeType = "Ditambahkan",
              oldValue = None,
              newValue = Some(p.targetValue),
              unit = p.unit,
              humanDescription = s"Tahap baru '${stageName}' ditambahkan dengan anjuran ${p.name} sebesar ${p.targetValue} ${p.unit}."
            )
          }

        case (Some(oldS), Some(newS)) =>
          val oldParams = oldS.parameters.map(p => p.name -> p).toMap
          val newParams = newS.parameters.map(p => p.name -> p).toMap
          val allParamNames = (oldParams.keySet ++ newParams.keySet).toList.sorted

          allParamNames.flatMap { pName =>
            (oldParams.get(pName), newParams.get(pName)) match
              case (Some(oldP), None) =>
                Some(ParamChange(
                  stageName = stageName,
                  paramName = pName,
                  changeType = "Dihapus",
                  oldValue = Some(oldP.targetValue),
                  newValue = None,
                  unit = oldP.unit,
                  humanDescription = s"Pada tahap ${stageName}, anjuran ${pName} (${oldP.targetValue} ${oldP.unit}) dihapus."
                ))

              case (None, Some(newP)) =>
                Some(ParamChange(
                  stageName = stageName,
                  paramName = pName,
                  changeType = "Ditambahkan",
                  oldValue = None,
                  newValue = Some(newP.targetValue),
                  unit = newP.unit,
                  humanDescription = s"Pada tahap ${stageName}, ditambahkan anjuran baru ${pName} sebesar ${newP.targetValue} ${newP.unit}."
                ))

              case (Some(oldP), Some(newP)) =>
                if oldP.targetValue != newP.targetValue then
                  val actionWord = if newP.targetValue > oldP.targetValue then "dinaikkan" else "diturunkan"
                  Some(ParamChange(
                    stageName = stageName,
                    paramName = pName,
                    changeType = "Diubah",
                    oldValue = Some(oldP.targetValue),
                    newValue = Some(newP.targetValue),
                    unit = newP.unit,
                    humanDescription = s"Pada tahap ${stageName}, anjuran ${pName} $actionWord dari ${oldP.targetValue} ${oldP.unit} menjadi ${newP.targetValue} ${newP.unit}."
                  ))
                else None

              case (None, None) => None
          }

        case (None, None) => Nil
    }

    ChangeSummary(
      oldGuideName = oldSnap.name,
      newGuideName = newSnap.name,
      changes = changesList,
      totalChanges = changesList.length
    )

  /**
   * Pure Function 3: Mengembalikan panduan ke versi revisi sebelumnya (Revert).
   */
  def restoreGuideRevision(
    targetRevisionToRestore: GuideRevision,
    currentHeadRevision: GuideRevision,
    author: String,
    timestamp: Long
  ): Either[String, GuideRevision] =
    if targetRevisionToRestore.id == currentHeadRevision.id then
      Left("Panduan sudah berada pada versi revisi ini. Tidak ada perubahan yang perlu dipulihkan.")
    else
      val restoredSnapshot = targetRevisionToRestore.snapshot.copy(
        name = targetRevisionToRestore.snapshot.name
      )
      val note = s"Memulihkan panduan ke versi sebelumnya (${targetRevisionToRestore.changeNotes})"
      Right(createGuideRevision(
        parentIds = List(currentHeadRevision.id),
        snapshot = restoredSnapshot,
        author = author,
        changeNotes = note,
        timestamp = timestamp
      ))

  /**
   * Pure Function 4: Validasi kelayakan sebelum panduan resmi diterbitkan.
   */
  def validateGuideForPublishing(snapshot: GuideSnapshot): Either[String, GuideSnapshot] =
    if snapshot.stages.isEmpty then
      Left("Panduan harus memiliki setidaknya satu tahap budidaya sebelum diterbitkan.")
    else if snapshot.stages.exists(!isPositiveStage(_)) then
      Left("Seluruh tahapan budidaya harus memiliki perkiraan durasi lebih dari 0 hari.")
    else
      val violations = auditParameters(snapshot) { p =>
        if isSafeParam(p) then Right(())
        else Left(s"Anjuran ${p.targetValue} ${p.unit} berada di luar batas aman rekomendasi pertanian [${p.minValue} - ${p.maxValue}]")
      }
      if violations.nonEmpty then
        Left(s"Ditemukan anjuran di luar batas aman:\n${violations.mkString("\n")}")
      else
        Right(snapshot)

  /**
   * Pure Function 5: Menghitung ringkasan statistik panduan budidaya.
   * Menggunakan:
   * - `foldLeft` untuk akumulasi hari dan parameter
   * - `groupBy` untuk mengelompokkan anjuran berdasarkan satuan
   * - `map` untuk rata-rata nilai
   * - Rantai 3 operasi koleksi (`flatMap` -> `filter` -> `map`)
   */
  def summarizeGuide(snapshot: GuideSnapshot): GuideSummary =
    // Rantai 3 operasi koleksi:
    val activeParamNames: List[String] = snapshot.stages
      .flatMap(_.parameters)
      .filter(_.targetValue > 0.0)
      .map(_.name.toLowerCase)

    // foldLeft:
    val totalDays = snapshot.stages.foldLeft(0)((acc, stage) => acc + stage.durationDays)
    val totalParams = snapshot.stages.foldLeft(0)((acc, stage) => acc + stage.parameters.length)

    val allParams = snapshot.stages.flatMap(_.parameters)

    // groupBy:
    val groupedByUnit: Map[String, List[AgronomicParam]] = allParams.groupBy(_.unit)

    val countsByUnit: Map[String, Int] = groupedByUnit.map { (unit, params) =>
      unit -> params.length
    }

    val averagesByUnit: Map[String, Double] = groupedByUnit.map { (unit, params) =>
      val sum = params.foldLeft(0.0)((acc, p) => acc + p.targetValue)
      val avg = if params.nonEmpty then sum / params.length else 0.0
      unit -> (math.round(avg * 10.0) / 10.0)
    }

    GuideSummary(
      totalStages = snapshot.stages.length,
      totalDurationDays = totalDays,
      totalParameters = totalParams,
      paramsByUnit = countsByUnit,
      averageValueByUnit = averagesByUnit
    )

  /**
   * Pure Function 6: Merangkum seluruh catatan kegiatan budidaya petani.
   * Menggunakan `groupBy` jenis kegiatan, `foldLeft` hitung kegiatan, dan `map`.
   */
  def summarizeActivities(activities: List[CultivationActivity]): Map[String, Int] =
    val grouped = activities.groupBy(_.activityType)
    grouped.map { (actType, list) =>
      actType -> list.foldLeft(0)((acc, _) => acc + 1)
    }

  /**
   * Pure Function 7: Memeriksa apakah anjuran pupuk/air dalam kegiatan petani sesuai panduan.
   */
  def checkActivityCompliance(
    activity: CultivationActivity,
    params: List[AgronomicParam]
  ): Option[String] =
    activity.quantity.flatMap { qStr =>
      val numOpt = qStr.trim.split("\\s+").headOption.flatMap(_.toDoubleOption)
      numOpt.flatMap { num =>
        val textToSearch = s"${activity.activityType} ${activity.notes}".toLowerCase
        params.find { p =>
          val pWords = p.name.toLowerCase.split("\\s+").filter(_.length >= 2)
          pWords.exists(w => textToSearch.contains(w)) ||
          (textToSearch.contains("pupuk") || textToSearch.contains("mupuk")) && p.name.toLowerCase.contains("pupuk") ||
          (textToSearch.contains("siram") || textToSearch.contains("nyiram")) && (p.name.toLowerCase.contains("siram") || p.name.toLowerCase.contains("air")) ||
          (textToSearch.contains("ec") || textToSearch.contains("nutrisi")) && (p.name.toLowerCase.contains("ec") || p.name.toLowerCase.contains("nutrisi"))
        }.flatMap { p =>
          if num < p.minValue || num > p.maxValue then
            Some(s"Pemberian $num ${p.unit} berada di luar batas anjuran panduan [${p.minValue} - ${p.maxValue}]")
          else None
        }
      }
    }

  // ==========================================================================
  // 4. RECURSIVE FUNCTION: Menelusuri Riwayat Revisi Panduan
  // ==========================================================================

  /**
   * Fungsi rekursif untuk menelusuri riwayat silsilah revisi panduan secara mundur ke revisi awal.
   */
  def getRevisionLineage(
    currentRevisionId: String,
    revisionLookup: Map[String, GuideRevision],
    visited: Set[String] = Set.empty
  ): List[GuideRevision] =
    if visited.contains(currentRevisionId) then Nil
    else
      revisionLookup.get(currentRevisionId) match
        case None => Nil
        case Some(rev) =>
          val newVisited = visited + currentRevisionId
          val parentLineages = rev.parentIds.flatMap { parentId =>
            getRevisionLineage(parentId, revisionLookup, newVisited)
          }
          rev :: parentLineages.distinctBy(_.id)

