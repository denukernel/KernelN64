# Third-party notices

KernelN64 contains inherited code and dependencies with their own notices. Copyright and license headers in source files are preserved.

* **Jario64 / Jason LaDere and contributors:** GPL version 3 or later notices appear in the Java sources. The complete GPL v3 text is in `COPYING`.
* **Project64 / zilmar and Jabo:** some inherited files also contain the original Project64 notice, including noncommercial-use terms. A verbatim copy is in `licenses/Project64-notice.txt`. These inherited notices require review before choosing a repository-wide license; this document does not replace or resolve them.
* **Azimer audio:** inherited attribution remains in the audio HLE sources.
* **JOGL 1.1.1:** Sun Microsystems and other upstream contributors. Complete upstream release notices are in `licenses/JOGL-1.1.1-LICENSE.txt`. Source: https://github.com/sgothel/jogl/blob/1.1.1/LICENSE.txt
* **Legacy GlueGen runtime:** Sun Microsystems. The upstream 1.0b06 license is included in `licenses/GlueGen-LICENSE.txt`; the bundled legacy JAR does not identify a release version in its manifest. Source: https://github.com/sgothel/gluegen/blob/1.0b06/LICENSE.txt
* **Bundled Java runtime:** portable archives preserve the runtime's complete `legal/` directory and `release` metadata. The current local runtime was generated from Oracle JDK 24.0.2; its terms are in `runtime/legal/java.base/LICENSE`. A rebuild uses the JDK selected by the builder. Review that JDK's redistribution terms when distributing a runtime.

No Nintendo game images, GameShark firmware images, game assets, or matching SM64 ELF/map files are included in the release preparation.
