param([string]$Jdk = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $Jdk -or -not (Test-Path -LiteralPath (Join-Path $Jdk 'bin/javac.exe'))) {
    throw 'Set JAVA_HOME to a JDK 21+ installation, or pass -Jdk <JDK directory>.'
}
$projectRoot = Split-Path $PSScriptRoot -Parent
& (Join-Path $projectRoot 'native/d3d11/Build.ps1') -Jdk $Jdk
$outputRoot = Join-Path $projectRoot 'dist/KernelN64'
$classRoot = Join-Path $projectRoot 'target/launcher-classes'
$tempRoot = Join-Path $projectRoot 'target/launcher-tmp'
if (Test-Path -LiteralPath $classRoot) {
    $resolvedClasses = (Resolve-Path -LiteralPath $classRoot).Path
    $expectedClasses = [IO.Path]::GetFullPath((Join-Path $projectRoot 'target/launcher-classes'))
    if ($resolvedClasses -ne $expectedClasses -or -not $resolvedClasses.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid class output path' }
    Remove-Item -LiteralPath $resolvedClasses -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $outputRoot, $classRoot, $tempRoot, (Join-Path $outputRoot 'lib') | Out-Null
$libraries = @('jogl.jar', 'gluegen-rt.jar', 'luaj-jse-3.0.1.jar') | ForEach-Object { Join-Path $projectRoot "KernelN64winportable/$_" }
$sources = @(Get-ChildItem (Join-Path $projectRoot 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& (Join-Path $Jdk 'bin/javac.exe') --release 21 -encoding UTF-8 -cp ($libraries -join ';') -d $classRoot $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
$sourceRoot = Join-Path $projectRoot 'src/main/java'
Get-ChildItem $sourceRoot -Recurse -File | Where-Object { $_.Extension -in '.png', '.properties' } | ForEach-Object {
    $relative = $_.FullName.Substring($sourceRoot.Length + 1)
    $destination = Join-Path $classRoot $relative
    New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}
# Eclipse's checked-in .classpath runs target/classes. Refresh it from the same
# complete compilation as app.jar, including resources and newly added helpers.
# Replace the core namespace output so moved classes cannot linger in Eclipse.
foreach ($legacyRelative in @('target/classes/jario64', 'target/test-classes/jario64', 'target/classes/kerneln64', 'target/test-classes/kerneln64', 'target/classes/gln64j', 'target/classes/d3d11', 'target/classes/kernel64gl', 'target/test-classes/kernel64gl', 'target/classes/aziaudio', 'target/classes/jario64plus', 'target/classes/debug', 'target/test-classes/gln64j', 'target/test-classes/d3d11', 'target/test-classes/aziaudio')) {
    $legacyOutput = [IO.Path]::GetFullPath((Join-Path $projectRoot $legacyRelative))
    $targetBoundary = [IO.Path]::GetFullPath((Join-Path $projectRoot 'target')) + [IO.Path]::DirectorySeparatorChar
    if (-not $legacyOutput.StartsWith($targetBoundary, [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid legacy class output' }
    if (Test-Path -LiteralPath $legacyOutput) {
        $resolvedLegacy = (Resolve-Path -LiteralPath $legacyOutput).Path
        if (-not $resolvedLegacy.Equals($legacyOutput, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected legacy class output target' }
        Remove-Item -LiteralPath $resolvedLegacy -Recurse -Force
    }
}
$eclipseClassRoot = Join-Path $projectRoot 'target/classes'
New-Item -ItemType Directory -Force -Path $eclipseClassRoot | Out-Null
Get-ChildItem $classRoot -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($classRoot.Length + 1)
    $destination = Join-Path $eclipseClassRoot $relative
    New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}
Write-Output "Refreshed Eclipse output: $eclipseClassRoot"
$testSourceRoot = Join-Path $projectRoot 'src/test/java'
$testSources = @()
if (Test-Path -LiteralPath $testSourceRoot) {
    $testSources = @(Get-ChildItem $testSourceRoot -Recurse -Filter '*.java' | ForEach-Object FullName)
}
if ($testSources.Count -gt 0) {
    $testClassRoot = Join-Path $projectRoot 'target/test-classes'
    New-Item -ItemType Directory -Force -Path $testClassRoot | Out-Null
    & (Join-Path $Jdk 'bin/javac.exe') --release 21 -encoding UTF-8 -cp ((@($classRoot) + $libraries) -join ';') -d $testClassRoot $testSources
    if ($LASTEXITCODE -ne 0) { throw 'Eclipse check compilation failed' }
}
& (Join-Path $Jdk 'bin/jar.exe') "-J-Djava.io.tmpdir=$tempRoot" --create --file (Join-Path $outputRoot 'app.jar') --main-class kerneln64.Main -C $classRoot .
if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed' }
foreach ($library in $libraries) { Copy-Item -LiteralPath $library -Destination (Join-Path $outputRoot 'lib') -Force }
foreach ($native in @('gluegen-rt.dll', 'jogl.dll', 'jogl_awt.dll', 'jogl_cg.dll', 'KernelN64D3D11.dll')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "KernelN64winportable/$native") -Destination (Join-Path $outputRoot 'lib') -Force
}
$runtime = Join-Path $outputRoot 'runtime'
if (-not (Test-Path -LiteralPath (Join-Path $runtime 'bin/javaw.exe'))) {
    & (Join-Path $Jdk 'bin/jlink.exe') --add-modules java.base,java.desktop,java.logging,java.management,jdk.unsupported --strip-debug --no-header-files --no-man-pages --output $runtime
    if ($LASTEXITCODE -ne 0) { throw 'Bundled Java runtime creation failed' }
}
$compiler = Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
& $compiler /nologo /target:winexe /platform:x64 /reference:System.Windows.Forms.dll "/win32icon:$(Join-Path $projectRoot 'assets/kerneln64.ico')" "/out:$outputRoot/KernelN64.exe" (Join-Path $PSScriptRoot 'Launcher.cs')
if ($LASTEXITCODE -ne 0) { throw 'Launcher compilation failed' }
Copy-Item -LiteralPath (Join-Path $outputRoot 'KernelN64.exe') -Destination (Join-Path $projectRoot 'KernelN64.exe') -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'README.md') -Destination $outputRoot -Force
foreach ($notice in @('COPYING', 'THIRD-PARTY-NOTICES.md')) {
    $noticePath = Join-Path $projectRoot $notice
    if (Test-Path -LiteralPath $noticePath) { Copy-Item -LiteralPath $noticePath -Destination $outputRoot -Force }
}
$licenseSource = Join-Path $projectRoot 'licenses'
if (Test-Path -LiteralPath $licenseSource) {
    $licenseOutput = Join-Path $outputRoot 'licenses'
    New-Item -ItemType Directory -Force -Path $licenseOutput | Out-Null
    Get-ChildItem -LiteralPath $licenseSource -File | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $licenseOutput -Force
    }
}
$firmwareGuide = Join-Path $projectRoot 'docs/GameShark-firmware.md'
if (Test-Path -LiteralPath $firmwareGuide) {
    $docsOutput = Join-Path $outputRoot 'docs'
    New-Item -ItemType Directory -Force -Path $docsOutput | Out-Null
    Copy-Item -LiteralPath $firmwareGuide -Destination $docsOutput -Force
}
$decompGuide = Join-Path $projectRoot 'docs/SM64-decomp-cheats.md'
if (Test-Path -LiteralPath $decompGuide) {
    $docsOutput = Join-Path $outputRoot 'docs'
    New-Item -ItemType Directory -Force -Path $docsOutput | Out-Null
    Copy-Item -LiteralPath $decompGuide -Destination $docsOutput -Force
}
$layoutGuide = Join-Path $projectRoot 'docs/Source-layout.md'
if (Test-Path -LiteralPath $layoutGuide) {
    $docsOutput = Join-Path $outputRoot 'docs'
    New-Item -ItemType Directory -Force -Path $docsOutput | Out-Null
    Copy-Item -LiteralPath $layoutGuide -Destination $docsOutput -Force
}
$codeSource = Join-Path $projectRoot 'codes'
if (Test-Path -LiteralPath $codeSource) {
    $codeOutput = Join-Path $outputRoot 'codes'
    New-Item -ItemType Directory -Force -Path $codeOutput | Out-Null
    Get-ChildItem -LiteralPath $codeSource -File | Where-Object { $_.Name -like '*.cheats.txt' -or $_.Extension -eq '.md' } | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $codeOutput -Force
    }
}
Write-Output "Built: $outputRoot/KernelN64.exe"
Write-Output "Project launcher: $projectRoot/KernelN64.exe"

foreach ($guide in @('Lua-scripting.md', 'Kernel64GL.md', 'Controller-settings.md')) {
    New-Item -ItemType Directory -Force -Path (Join-Path $outputRoot 'docs') | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot "docs/$guide") -Destination (Join-Path $outputRoot 'docs') -Force
}
$luaOutput = Join-Path $outputRoot 'lua/examples'
New-Item -ItemType Directory -Force -Path $luaOutput | Out-Null
foreach ($example in @('frame-watch.lua','input-pulse.lua','sm64-audio-wobble.lua','sm64-audio-broken-scale.lua','sm64-music-tempo.lua','sm64-gfx-squash.lua','sm64-blj-assist.lua')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "lua/examples/$example") -Destination $luaOutput -Force
}
