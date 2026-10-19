param([string]$Jdk = $env:JAVA_HOME)
$ErrorActionPreference='Stop'
if (-not $Jdk -or -not (Test-Path -LiteralPath (Join-Path $Jdk 'include/jni.h'))) {
    throw 'Set JAVA_HOME to a JDK 21+ installation, or pass -Jdk <JDK directory>.'
}
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$locator = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
if (-not (Test-Path -LiteralPath $locator)) { throw 'Install Visual Studio Build Tools with Desktop development with C++.' }
$visualStudio = & $locator -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $visualStudio) { throw 'Visual C++ x64 build tools were not found.' }
$compilerRoot=Join-Path $visualStudio 'VC/Tools/MSVC'
$compilerVersion=(Get-ChildItem $compilerRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName
$sdkRoot=(Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows Kits\Installed Roots').KitsRoot10
$sdkVersion=(Get-ChildItem "$sdkRoot/Include" -Directory | Sort-Object Name -Descending | Select-Object -First 1).Name
$build=Join-Path $root 'target/d3d11-native'
New-Item -ItemType Directory -Force -Path $build | Out-Null
$includes=@("$compilerVersion/include","$sdkRoot/Include/$sdkVersion/ucrt","$sdkRoot/Include/$sdkVersion/shared","$sdkRoot/Include/$sdkVersion/um","$sdkRoot/Include/$sdkVersion/winrt","$Jdk/include","$Jdk/include/win32")
$libraries=@("$compilerVersion/lib/x64","$sdkRoot/Lib/$sdkVersion/ucrt/x64","$sdkRoot/Lib/$sdkVersion/um/x64","$Jdk/lib")
$output=Join-Path $root 'Jario64winportable/JarioD3D11.dll'
# LINK replaces its import library when JNI exports change. Remove only these
# disposable linker outputs so rebuilding also works under restricted file access.
foreach($name in @('JarioD3D11.lib','JarioD3D11.exp')) {
    $artifact=Join-Path $build $name
    if(Test-Path -LiteralPath $artifact){Remove-Item -LiteralPath $artifact -Force}
}
$arguments=@('/nologo','/LD','/MT','/O2','/EHsc','/std:c++17')+@($includes|ForEach-Object{"/I$_"})+@((Join-Path $PSScriptRoot 'renderer.cpp'),"/Fo$build/renderer.obj",'/link',"/OUT:$output","/IMPLIB:$build/JarioD3D11.lib")+@($libraries|ForEach-Object{"/LIBPATH:$_"})+@('d3d11.lib','dxgi.lib','d3dcompiler.lib','jawt.lib','user32.lib')
& "$compilerVersion/bin/Hostx64/x64/cl.exe" @arguments
if($LASTEXITCODE -ne 0){throw 'Direct3D 11 native build failed'}
Write-Output "Built: $output"
