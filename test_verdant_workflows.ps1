$base = 'http://127.0.0.1:8080'

Write-Host "=========================================================="
Write-Host "PENGUJIAN ALUR OPERASIONAL & ROLE SEPARATION VERDANT"
Write-Host "=========================================================="

# -----------------------------------------------------------------------------
# 1. Pengujian Role Selector & Navigasi Terpisah
# -----------------------------------------------------------------------------
Write-Host "`n[1. Pengujian Role Selector & Navigasi Terpisah]"

# Farmer Workspace
$farmerHome = Invoke-WebRequest -Uri "$base/?role=farmer" -UseBasicParsing
Write-Host "1. Akses Beranda Petani: Status $($farmerHome.StatusCode)"
$hasFarmerTitle = $farmerHome.Content.Contains("Beranda Petani")
$hasMasaTanamNav = $farmerHome.Content.Contains("Masa Tanam Saya")
$hasNoSupervisorNav = -not $farmerHome.Content.Contains("Kelola Panduan")
Write-Host "   - Judul Beranda Petani: $hasFarmerTitle"
Write-Host "   - Menu 'Masa Tanam Saya' Tampil: $hasMasaTanamNav"
Write-Host "   - Menu Mandor (Kelola Panduan) Tersembunyi untuk Petani: $hasNoSupervisorNav"

# Supervisor Workspace
$supHome = Invoke-WebRequest -Uri "$base/?role=supervisor" -UseBasicParsing
Write-Host "`n2. Akses Beranda Mandor: Status $($supHome.StatusCode)"
$hasSupTitle = $supHome.Content.Contains("Beranda Mandor")
$hasKelolaNav = $supHome.Content.Contains("Kelola Panduan")
$hasPerubahanNav = $supHome.Content.Contains("Pemeriksaan Perubahan")
Write-Host "   - Judul Beranda Mandor: $hasSupTitle"
Write-Host "   - Menu 'Kelola Panduan' Tampil: $hasKelolaNav"
Write-Host "   - Menu 'Pemeriksaan Perubahan' Tampil: $hasPerubahanNav"

# Demo Role Switcher Cookie Test
$roleSwitch = Invoke-WebRequest -Uri "$base/role?set=supervisor&redirect=/" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "`n3. Beralih Peran via /role?set=supervisor: Status $($roleSwitch.StatusCode) -> Location: $($roleSwitch.Headers['Location'])"
$hasCookie = $roleSwitch.Headers['Set-Cookie'].Contains("verdant_role=supervisor")
Write-Host "   - Cookie verdant_role=supervisor Terpasang: $hasCookie"

# -----------------------------------------------------------------------------
# 2. Pengujian Proteksi Akses & 403 Forbidden
# -----------------------------------------------------------------------------
Write-Host "`n[2. Pengujian Proteksi Rute Mandor dari Petani (403 Forbidden)]"

# Petani mencoba mengakses rute kelola panduan mandor
$unauthGet = Invoke-WebRequest -Uri "$base/kelola-panduan?role=farmer" -UseBasicParsing -SkipHttpErrorCheck
Write-Host "1. Petani coba buka /kelola-panduan: Status $($unauthGet.StatusCode)"
$has403Text = $unauthGet.Content.Contains("403 Forbidden") -or $unauthGet.Content.Contains("Akses Ditolak")
Write-Host "   - Pesan Akses Ditolak 403 Diterima: $has403Text"

# Petani mencoba POST tambah panduan
$unauthPost = Invoke-WebRequest -Uri "$base/panduan/tambah?role=farmer" -Method POST -Body "id=illegal&name=illegal" -SkipHttpErrorCheck
Write-Host "2. Petani coba POST ke /panduan/tambah: Status $($unauthPost.StatusCode)"
Write-Host "   - Proteksi Mutasi Backend Berjalan: $($unauthPost.StatusCode -eq 403)"

# -----------------------------------------------------------------------------
# 3. Pengujian Alur Penuh (End-to-End Business Workflow)
# -----------------------------------------------------------------------------
Write-Host "`n[3. Pengujian Alur Bisnis End-to-End]"

# Langkah 1: Mandor membuat panduan budidaya baru
Write-Host "Langkah 1: Mandor membuat panduan budidaya 'panduan-melon-01'..."
$createGuideBody = "id=panduan-melon-01&name=SOP+Budidaya+Melon+Golden+Aroma&cropVariety=Melon+Golden+Aroma&author=Ir.+Budi+Santoso"
$createGuideRes = Invoke-WebRequest -Uri "$base/panduan/tambah?role=supervisor" -Method POST -Body $createGuideBody -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "   - Status Buat Panduan: $($createGuideRes.StatusCode)"

# Ambil ID revisi awal
$melonDetail = Invoke-WebRequest -Uri "$base/panduan/panduan-melon-01?role=supervisor" -UseBasicParsing
$regexRev = [regex]'Revisi:\s*#([a-f0-9]{8})'
$revMatch = $regexRev.Match($melonDetail.Content)
$melonRevId = $revMatch.Groups[1].Value
Write-Host "   - Draf Awal Dibuat dengan ID Hash: $melonRevId"

# Langkah 2: Mandor menerbitkan edisi resmi Edisi 2026.1
Write-Host "`nLangkah 2: Mandor menyetujui & menerbitkan Edisi 2026.1..."
# Ambil revisi lengkap dari DB lewat list
$tinjauMelon = Invoke-WebRequest -Uri "$base/panduan/panduan-melon-01/tinjau?role=supervisor" -UseBasicParsing
$regexInputRev = [regex]'name="revisionId" value="([a-f0-9]{8})"'
$fullRevId = $regexInputRev.Match($tinjauMelon.Content).Groups[1].Value

$publishMelonBody = "revisionId=$fullRevId&editionTag=Edisi+2026.1+-+Musim+Kemarau&approvedBy=H.+Suwandi&notes=Telah+divalidasi+untuk+greenhouse+melon."
$pubMelonRes = Invoke-WebRequest -Uri "$base/panduan/panduan-melon-01/terbitkan?role=supervisor" -Method POST -Body $publishMelonBody -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "   - Status Publikasi Edisi 2026.1: $($pubMelonRes.StatusCode)"

# Langkah 3: Petani memulai Masa Tanam baru menggunakan Edisi 2026.1
Write-Host "`nLangkah 3: Petani memulai Masa Tanam baru 'MT-2026-MELON-01'..."
$bukaMasaTanamBody = "code=MT-2026-MELON-01&guideSelection=panduan-melon-01|Edisi+2026.1+-+Musim+Kemarau&plotName=Greenhouse+Melon+Blok+C"
$bukaRes = Invoke-WebRequest -Uri "$base/masa-tanam/buka?role=farmer" -Method POST -Body $bukaMasaTanamBody -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "   - Status Buka Masa Tanam: $($bukaRes.StatusCode)"

# Langkah 4: Petani mencatat kegiatan budidaya di masa tanam tersebut
Write-Host "`nLangkah 4: Petani mencatat kegiatan penyiraman & nutrisi..."
$catatBody = "periodCode=MT-2026-MELON-01&activityType=Penyiraman&activityDate=2026-10-27&quantity=150+Liter+AB+Mix&notes=Penyiraman+pagi+EC+1.8+berjalan+lancar.&farmerName=Pak+Slamet"
$catatRes = Invoke-WebRequest -Uri "$base/catatan/tambah?role=farmer" -Method POST -Body $catatBody -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "   - Status Simpan Kegiatan: $($catatRes.StatusCode)"

# Langkah 5: Petani memeriksa masa tanam & verifikasi panduan resmi terkunci
Write-Host "`nLangkah 5: Petani memeriksa detail masa tanam MT-2026-MELON-01..."
$periodDetail = Invoke-WebRequest -Uri "$base/masa-tanam/MT-2026-MELON-01?role=farmer" -UseBasicParsing
$hasLockedEdition = $periodDetail.Content.Contains("Edisi 2026.1 - Musim Kemarau")
$hasLoggedAct = $periodDetail.Content.Contains("150 Liter AB Mix")
Write-Host "   - Panduan Resmi Terkunci (Edisi 2026.1) Ditemukan: $hasLockedEdition"
Write-Host "   - Catatan Kegiatan Petani Tampil di Garis Waktu: $hasLoggedAct"

# Langkah 6: Mandor memperbarui panduan dan menerbitkan Edisi 2026.2
Write-Host "`nLangkah 6: Mandor memperbarui panduan melon dan menerbitkan Edisi 2026.2..."
$editMelonBody = "author=Ir.+Budi+Santoso&changeNotes=Peningkatan+dosis+pupuk+awal&stageName=Fase+Persemaian+%26+Awal+Tanam&paramName=Dosis+Pupuk+Awal&newTargetValue=25.0"
$editMelonRes = Invoke-WebRequest -Uri "$base/panduan/panduan-melon-01/perbarui?role=supervisor" -Method POST -Body $editMelonBody -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "   - Status Simpan Draf Baru Mandor: $($editMelonRes.StatusCode)"

$tinjauMelon2 = Invoke-WebRequest -Uri "$base/panduan/panduan-melon-01/tinjau?role=supervisor" -UseBasicParsing
$newRevId = $regexInputRev.Match($tinjauMelon2.Content).Groups[1].Value

$pubMelon2Body = "revisionId=$newRevId&editionTag=Edisi+2026.2+-+Nutrisi+Tinggi&approvedBy=H.+Suwandi&notes=Edisi+revisi+nutrisi+tinggi."
$pubMelon2Res = Invoke-WebRequest -Uri "$base/panduan/panduan-melon-01/terbitkan?role=supervisor" -Method POST -Body $pubMelon2Body -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "   - Status Publikasi Edisi 2026.2: $($pubMelon2Res.StatusCode)"

# Langkah 7: VERIFIKASI KRITIS IMMUTABILITY
Write-Host "`nLangkah 7: Verifikasi Kritis: Masa tanam lama TETAP terkunci pada Edisi 2026.1..."
$periodCheckAfter = Invoke-WebRequest -Uri "$base/masa-tanam/MT-2026-MELON-01?role=farmer" -UseBasicParsing
$stillLockedToOld = $periodCheckAfter.Content.Contains("Edisi 2026.1 - Musim Kemarau")
$notCorruptedToNew = -not $periodCheckAfter.Content.Contains("Edisi 2026.2 - Nutrisi Tinggi")
Write-Host "   - Masa tanam MT-2026-MELON-01 tetap pada Edisi 2026.1: $stillLockedToOld"
Write-Host "   - Masa tanam tidak tertimpa oleh Edisi 2026.2: $notCorruptedToNew"

Write-Host "`n=========================================================="
if ($hasFarmerTitle -and $hasSupTitle -and $hasCookie -and $has403Text -and $hasLockedEdition -and $hasLoggedAct -and $stillLockedToOld -and $notCorruptedToNew) {
    Write-Host "HASIL PENGUJIAN: SELURUH ALUR & ROLE SEPARATION SUKSES 100%! 🎉" -ForegroundColor Green
} else {
    Write-Host "HASIL PENGUJIAN: ADA BAGIAN YANG TIDAK LOLOS." -ForegroundColor Red
}
Write-Host "=========================================================="
