# Update Attendance Service Imports - Safe Migration Script
Write-Host "Updating Attendance Service imports for microservices architecture..." -ForegroundColor Cyan

$attendanceServicePath = "backend/microservices/attendance-service/src/main/java/com/example/attendancesystem/attendance"

# Define import replacements
$importReplacements = @{
    "import com.example.attendancesystem.shared.model.AttendanceLog;" = "import com.example.attendancesystem.attendance.model.AttendanceLog;"
    "import com.example.attendancesystem.shared.model.AttendanceSession;" = "import com.example.attendancesystem.attendance.model.AttendanceSession;"
    "import com.example.attendancesystem.shared.model.CheckInMethod;" = "import com.example.attendancesystem.attendance.model.CheckInMethod;"
    "import com.example.attendancesystem.shared.model.ScheduledSession;" = "import com.example.attendancesystem.attendance.model.ScheduledSession;"
    "import com.example.attendancesystem.shared.repository.AttendanceLogRepository;" = "import com.example.attendancesystem.attendance.repository.AttendanceLogRepository;"
    "import com.example.attendancesystem.shared.repository.AttendanceSessionRepository;" = "import com.example.attendancesystem.attendance.repository.AttendanceSessionRepository;"
    "import com.example.attendancesystem.shared.repository.ScheduledSessionRepository;" = "import com.example.attendancesystem.attendance.repository.ScheduledSessionRepository;"
    # Remove external entity imports (these should use gRPC)
    "import com.example.attendancesystem.shared.model.Organization;" = ""
    "import com.example.attendancesystem.shared.model.Subscriber;" = ""
    "import com.example.attendancesystem.shared.repository.OrganizationRepository;" = ""
    "import com.example.attendancesystem.shared.repository.SubscriberRepository;" = ""
    # Wildcard imports
    "import com.example.attendancesystem.shared.model.*;" = "import com.example.attendancesystem.attendance.model.*;"
    "import com.example.attendancesystem.shared.repository.*;" = "import com.example.attendancesystem.attendance.repository.*;"
}

# Get all Java files in the attendance service
$javaFiles = Get-ChildItem -Path $attendanceServicePath -Recurse -Filter "*.java"

$filesModified = 0
$totalReplacements = 0

foreach ($file in $javaFiles) {
    Write-Host "Processing: $($file.Name)" -ForegroundColor Yellow
    
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    $fileModified = $false
    $fileReplacements = 0
    
    foreach ($oldImport in $importReplacements.Keys) {
        $newImport = $importReplacements[$oldImport]
        
        if ($content -match [regex]::Escape($oldImport)) {
            if ($newImport -eq "") {
                # Remove the import line entirely
                $content = $content -replace [regex]::Escape($oldImport), ""
                # Also remove any empty lines left behind
                $content = $content -replace "(?m)^\s*$\n", ""
            } else {
                $content = $content -replace [regex]::Escape($oldImport), $newImport
            }
            $fileModified = $true
            $fileReplacements++
            Write-Host "  ✅ Replaced: $oldImport" -ForegroundColor Green
        }
    }
    
    if ($fileModified) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        $filesModified++
        $totalReplacements += $fileReplacements
        Write-Host "  📝 Updated $fileReplacements imports in $($file.Name)" -ForegroundColor Cyan
    } else {
        Write-Host "  ⏭️ No changes needed" -ForegroundColor Gray
    }
}

Write-Host "`n📊 IMPORT UPDATE SUMMARY:" -ForegroundColor Magenta
Write-Host "Files processed: $($javaFiles.Count)" -ForegroundColor White
Write-Host "Files modified: $filesModified" -ForegroundColor Green
Write-Host "Total replacements: $totalReplacements" -ForegroundColor Green

if ($filesModified -gt 0) {
    Write-Host "`n✅ Import updates completed successfully!" -ForegroundColor Green
    Write-Host "Next step: Compile and test the Attendance Service" -ForegroundColor Yellow
} else {
    Write-Host "`n⚠️ No files were modified. Check if imports are already correct." -ForegroundColor Yellow
}
