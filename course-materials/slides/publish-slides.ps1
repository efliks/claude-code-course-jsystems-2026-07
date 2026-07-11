# Publish course slide decks to the DevPowers site repo.
# Junctions/symlinks do not work across two git repos (git tracks the link,
# not content) - this explicit copy is the sync mechanism. Run after any
# deck change, then review + commit in the DevPowers repo.
# Usage: pwsh course-materials/slides/publish-slides.ps1

$slidesDir = $PSScriptRoot
$repoRoot  = Split-Path (Split-Path $slidesDir -Parent) -Parent
$dst = "C:\Users\BiuroEdukey\DEV\Projects\DevPowers\szkolenia\claude-code-jsystems"

New-Item -ItemType Directory -Force -Path $dst | Out-Null
$copied = @()
foreach ($n in 1..3) {
    $from = Join-Path $slidesDir "day-$n.html"
    if (Test-Path $from) {
        $to = Join-Path $dst "Prezentacja_Dzien$n.html"
        $html = Get-Content $from -Raw -Encoding UTF8
        # On the published site the survey dashboard sits next to the decks
        $html = $html.Replace("../survey/dashboard/index.html", "ankieta.html")
        Set-Content -Path $to -Value $html -Encoding UTF8 -NoNewline
        $copied += "Prezentacja_Dzien$n.html"
    }
}
# Survey dashboard (anonymized) published next to the decks
$dash = Join-Path $repoRoot "course-materials\survey\dashboard\index.html"
if (Test-Path $dash) {
    Copy-Item $dash (Join-Path $dst "ankieta.html") -Force
    $copied += "ankieta.html"
}
Write-Host "Copied to ${dst}: $($copied -join ', ')"
Write-Host "NEXT: cd to the DevPowers repo, review the diff, commit. Ask Lucas before pushing (production site)."
