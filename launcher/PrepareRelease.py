"""Stage source and portable archives from an explicit allowlist; never copy local emulator data."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import zipfile
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parent.parent


def copy(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)


def tree(source, destination, excluded=()):
    for path in sorted(source.rglob("*")):
        if path.is_file() and path.name not in excluded:
            copy(path, destination / path.relative_to(source))


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def archive(source, destination, prefix):
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as output:
        for path in sorted(source.rglob("*")):
            if path.is_file():
                output.write(path, (Path(prefix) / path.relative_to(source)).as_posix())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True, help="An empty staging folder")
    args = parser.parse_args()
    output = args.output.resolve()
    if output == ROOT or ROOT in output.parents and output.parts[-1] in {"src", "target", "dist"}:
        raise SystemExit("Choose a separate empty release staging folder.")
    if output.exists() and any(output.iterdir()):
        raise SystemExit("Staging folder must be empty; existing files are never replaced.")
    portable_build = ROOT / "dist/KernelN64"
    for required in ["KernelN64.exe", "app.jar", "runtime/bin/javaw.exe"]:
        if not (portable_build / required).is_file():
            raise SystemExit("Build the portable application with launcher/Build.ps1 first.")

    repository = output / "repository"
    downloads = output / "release-assets"
    repository.mkdir(parents=True)
    downloads.mkdir()
    for name in [".project", ".classpath", ".gitignore", "KernelN64.launch", "pom.xml", "COPYING", "THIRD-PARTY-NOTICES.md"]:
        copy(ROOT / name, repository / name)
    for name in ["org.eclipse.jdt.core.prefs", "org.eclipse.m2e.core.prefs"]:
        copy(ROOT / ".settings" / name, repository / ".settings" / name)
    tree(ROOT / "src/main", repository / "src/main", {"Test.java"})
    classpath = ET.parse(repository / ".classpath")
    for entry in list(classpath.getroot()):
        path = entry.get("path", "")
        if entry.get("kind") == "src" and path != "src/main/java":
            classpath.getroot().remove(entry)
    ET.indent(classpath)
    classpath.write(repository / ".classpath", encoding="UTF-8", xml_declaration=True)

    tree(ROOT / "native", repository / "native")
    tree(ROOT / "launcher", repository / "launcher")
    tree(ROOT / "licenses", repository / "licenses")
    tree(ROOT / "codes", repository / "codes")
    for name in ["kerneln64-icon-source.png", "kerneln64.ico"]:
        copy(ROOT / "assets" / name, repository / "assets" / name)
    tree(ROOT / "assets/screenshots", repository / "assets/screenshots")
    for name in ["jogl.jar", "gluegen-rt.jar", "jogl.dll", "jogl_awt.dll", "jogl_cg.dll", "gluegen-rt.dll", "KernelN64D3D11.dll"]:
        copy(ROOT / "KernelN64winportable" / name, repository / "KernelN64winportable" / name)
    copy(ROOT / "docs/Release-README.md", repository / "README.md")
    for name in ["Source-layout.md", "SM64-decomp-cheats.md", "GameShark-firmware.md", "GLN64j-compatibility.md", "Kernel64GL.md", "Release-preparation.md", "Release-README.md", "Portable-README.md"]:
        text = (ROOT / "docs" / name).read_text(encoding="utf-8")
        if name in {"SM64-decomp-cheats.md", "GameShark-firmware.md"}:
            text = text.split("\n## Verification\n")[0]
        if name == "SM64-decomp-cheats.md":
            text = text.split("\n## Verified build and address evidence\n")[0]
            if "Opening the supplied" in text:
                start = text.index("Opening the supplied")
                end = text.index("No changes to the decomp source", start)
                text = text[:start] + "The matching ELF is discovered automatically. " + text[end:]
        if name == "Source-layout.md":
            text = text.replace("Feature checks live under matching test packages, with shared runtime checks in `src/test/java/kerneln64`.",
                                "This prepared source snapshot omits automated testing files and local fixtures.")
        (repository / "docs" / name).parent.mkdir(exist_ok=True)
        (repository / "docs" / name).write_text(text, encoding="utf-8")

    blocked = {".z64", ".n64", ".v64", ".rom", ".j64state", ".elf", ".map", ".eep", ".sra", ".fla", ".mpk", ".log"}
    for path in repository.rglob("*"):
        if path.is_file() and path.suffix.lower() in blocked:
            raise SystemExit("Unexpected private or test artifact in staging: " + str(path.relative_to(repository)))
        if path.is_file() and path.suffix in {".java", ".md", ".xml", ".ps1", ".cs", ".properties", ".prefs", ".launch"}:
            text = path.read_text(encoding="utf-8")
            if "C:\\Users\\Intel" in text or "C:/Users/Intel" in text or "\\\\wsl.localhost" in text:
                raise SystemExit("Personal machine path in staging: " + str(path.relative_to(repository)))

    archive(repository, downloads / "KernelN64-source-preview.zip", "KernelN64")
    with tempfile.TemporaryDirectory(prefix="release-portable-", dir=ROOT / "target") as temporary:
        if not Path(temporary).resolve().is_relative_to((ROOT / "target").resolve()):
            raise RuntimeError("Unexpected temporary directory outside build output")
        portable = Path(temporary) / "KernelN64"
        portable.mkdir()
        for name in ["KernelN64.exe", "app.jar", "COPYING", "THIRD-PARTY-NOTICES.md"]:
            copy(portable_build / name, portable / name)
        for name in ["lib", "runtime", "licenses", "codes"]:
            tree(portable_build / name, portable / name)
        tree(repository / "docs", portable / "docs")
        tree(repository / "assets", portable / "assets")
        copy(ROOT / "docs/Portable-README.md", portable / "README.md")
        archive(portable, downloads / "KernelN64-windows-x64-preview.zip", "KernelN64")

    sums = "".join(digest(path) + "  " + path.name + "\n" for path in sorted(downloads.glob("*.zip")))
    (downloads / "SHA256SUMS.txt").write_text(sums, encoding="ascii")
    manifest = [{"path": path.relative_to(repository).as_posix(), "bytes": path.stat().st_size, "sha256": digest(path)}
                for path in sorted(repository.rglob("*")) if path.is_file()]
    (output / "SOURCE-MANIFEST.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    (output / "START-HERE.md").write_text(
        "# KernelN64 release preparation\n\n"
        "Upload the contents of `repository/` to the GitHub repository. Attach the two ZIPs and SHA256SUMS.txt in `release-assets/` to a release.\n\n"
        "ROMs, saves, personal settings, logs, testing files and build caches are excluded. Nothing has been published.\n\n"
        "Before publishing, review `repository/docs/Release-preparation.md`, especially inherited Project64/GPL notices and the bundled JDK's terms. Choose a final version/tag and test on a clean Windows x64 machine.\n",
        encoding="utf-8")
    print("Prepared", len(manifest), "source files and two release ZIPs in", output)
    print(sums, end="")


if __name__ == "__main__":
    main()
