# Release preparation

The prepared folder separates a clean source repository from downloadable release assets. It does not publish anything or create a GitHub repository.

## Included

* Main source, source assets, Eclipse configuration, Maven metadata, launcher and native renderer build scripts.
* Only the JOGL/GlueGen JARs and native DLLs used by the current build, plus the locally built Direct3D DLL.
* Source layout and supported feature documentation, copyright/license notices, and documented cheat preset catalogs.
* A Windows x64 portable ZIP with the launcher, application JAR, native libraries and complete Java runtime legal files.

## Excluded

ROMs, firmware images, saves, savestates, screenshots, logs, crash reports, personal configuration and cheat selections, local research/decomp files, `port to java`, build caches, old plugin JARs, unused OpenAL/native-audio DLLs, and testing source/output are excluded. The source package's build script supports the absence of `src/test`.

## Before publishing

1. Review the inherited license notices in `THIRD-PARTY-NOTICES.md` and `licenses/Project64-notice.txt`. Some original files include both GPL notices and Project64 noncommercial-use terms. The preparation preserves these terms and does not claim that a single blanket license resolves them.
2. Choose the final version/tag and update release notes. The staged build is labeled a development preview; its native renderers and firmware support have documented limitations.
3. Upload the contents of `repository/` as the GitHub repository. Keep `release-assets/` outside the source tree; attach its ZIP files and `SHA256SUMS.txt` to a GitHub release.
4. Test the extracted Windows package on another Windows x64 machine. Local checks do not replace a clean-machine compatibility check.
5. Release matching source alongside each binary. Keep dependency and Java runtime notices with downloads.

No credentials, account details, Git remotes, or signing keys are needed for this local preparation.
