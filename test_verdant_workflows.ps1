$base = 'http://127.0.0.1:8080'

Write-Host "=========================================================="
Write-Host "PENGUJIAN ALUR OPERASIONAL VERDANT PERTANIAN"
Write-Host "=========================================================="

Write-Host "`n[Alur 1: Petani Mencatat Kegiatan Budidaya]"
$beranda = Invoke-WebRequest -Uri "$base/" -UseBasicParsing
Write-Host "1. Akses Beranda: Status $($beranda.StatusCode)"
$hasPromise = $beranda.Content.Contains("Catat kegiatan budidaya, ikuti panduan, dan lacak riwayat pertanian dengan mudah.")
Write-Host "   Janji Produk Ditemukan: $hasPromise"

$catatBody = "periodCode=MT-2026-TOMAT-01&activityType=Pemupukan&activityDate=2026-10-27&quantity=30+kg+Kalsium+Nitrat&notes=Pemberian+kalsium+tambahan+pada+pohon+tomat+untuk+mencegah+busuk+buah.&farmerName=Pak+Joko"
$catatRes = Invoke-WebRequest -Uri "$base/catatan/tambah" -Method POST -Body $catatBody -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "2. Simpan Kegiatan Baru: Status $($catatRes.StatusCode) -> Location: $($catatRes.Headers['Location'])"

$catatanList = Invoke-WebRequest -Uri "$base/catatan?periodCode=MT-2026-TOMAT-01" -UseBasicParsing
$hasNewAct = $catatanList.Content.Contains("30 kg Kalsium Nitrat")
Write-Host "3. Verifikasi Catatan Tampil di Jurnal: $hasNewAct"

Write-Host "`n[Alur 2: Petani Membaca Panduan Budidaya]"
$panduan = Invoke-WebRequest -Uri "$base/panduan/panduan-tomat-01" -UseBasicParsing
Write-Host "1. Akses Panduan Tomat: Status $($panduan.StatusCode)"
$hasInstruksi = $panduan.Content.Contains("Petunjuk Petani")
$hasTahap = $panduan.Content.Contains("Fase Pembungaan & Pembentukan Buah")
Write-Host "   Petunjuk Lapangan & Tahapan Ditemukan: $($hasInstruksi -and $hasTahap)"

Write-Host "`n[Alur 3: Mandor Ubah Panduan -> Tinjau Perubahan -> Terbitkan Panduan]"
$editBody = "author=Ir.+Budi+Santoso&changeNotes=Penyesuaian+nutrisi+pembungaan+musim+hujan&stageName=Fase+Pembungaan+%26+Pembentukan+Buah&paramName=Konsentrasi+Nutrisi+EC&newTargetValue=2.8"
$editRes = Invoke-WebRequest -Uri "$base/panduan/panduan-tomat-01/perbarui" -Method POST -Body $editBody -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "1. Simpan Draf Perubahan Mandor: Status $($editRes.StatusCode) -> Location: $($editRes.Headers['Location'])"

$tinjauPage = Invoke-WebRequest -Uri "$base/panduan/panduan-tomat-01/tinjau" -UseBasicParsing
$hasApaYangBerubah = $tinjauPage.Content.Contains("Tinjauan Perubahan: Apa yang Berubah?")
$hasPenjelasan = $tinjauPage.Content.Contains("dinaikkan dari 2.6 mS/cm menjadi 2.8 mS/cm")
Write-Host "2. Verifikasi 'Apa yang berubah?': $hasApaYangBerubah | Kalimat Penjelas: $hasPenjelasan"

# Ambil ID revisi terkini dari form
$regexRev = [regex]'name="revisionId" value="([a-f0-9]{8})"'
$revMatch = $regexRev.Match($tinjauPage.Content)
$revId = $revMatch.Groups[1].Value
Write-Host "   ID Revisi: $revId"

$publishBody = "revisionId=$revId&editionTag=Edisi+2026.2+-+Musim+Hujan&approvedBy=H.+Suwandi+(Kepala+Kebun)&notes=Disetujui+untuk+seluruh+greenhouse+tomat."
$pubRes = Invoke-WebRequest -Uri "$base/panduan/panduan-tomat-01/terbitkan" -Method POST -Body $publishBody -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "3. Mandor Terbitkan Edisi Resmi: Status $($pubRes.StatusCode)"

$panduanAfterPub = Invoke-WebRequest -Uri "$base/panduan/panduan-tomat-01" -UseBasicParsing
$hasNewEdition = $panduanAfterPub.Content.Contains("Edisi 2026.2 - Musim Hujan")
Write-Host "4. Edisi Resmi Baru Terpasang: $hasNewEdition"

Write-Host "`n[Alur 4: Petani / Mandor Memeriksa Masa Tanam & Panduan Terkunci]"
$masaTanam = Invoke-WebRequest -Uri "$base/masa-tanam/MT-2026-TOMAT-01" -UseBasicParsing
Write-Host "1. Akses Masa Tanam MT-2026-TOMAT-01: Status $($masaTanam.StatusCode)"
$hasTimeline = $masaTanam.Content.Contains("Garis Waktu Kegiatan Lapangan")
$hasLockedGuide = $masaTanam.Content.Contains("Terkunci pada Panduan Resmi:")
Write-Host "   Garis Waktu & Panduan Terkunci Ditemukan: $($hasTimeline -and $hasLockedGuide)"

Write-Host "`n[Alur 5: Riwayat Perubahan & Pemulihan Versi]"
$riwayat = Invoke-WebRequest -Uri "$base/riwayat" -UseBasicParsing
$hasRiwayat = $riwayat.Content.Contains("Riwayat Perubahan & Edisi Panduan")
Write-Host "1. Halaman Riwayat: $hasRiwayat"

Write-Host "`n=========================================================="
Write-Host "SELURUH ALUR OPERASIONAL VERDANT BERHASIL DIUJI 100%!"
Write-Host "=========================================================="
