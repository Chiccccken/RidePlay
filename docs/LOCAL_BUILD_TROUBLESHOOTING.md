# Local build output in synced folders

If a checkout lives in a synced directory such as macOS Documents, keep disposable build
output outside that directory. Opt in with an absolute path (use a separate path per checkout):

```sh
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew \
  -Pdiplay.buildRoot=/private/tmp/diplay-local-build \
  :common:testDebugUnitTest
```

The property relocates every project's `build` directory, including AGP's native intermediates.
The build root is canonicalized so `/tmp` and `/private/tmp` on macOS use one output identity.
Omit it to use the ordinary build locations; CI needs no change. Pass the same property on
subsequent test, lint, assemble and clean commands. Gradle's project cache can also be moved
with `--project-cache-dir /private/tmp/diplay-project-cache` when the checkout itself is synced.
`/private/tmp` is temporary: use an unsynced persistent directory for longer-lived output.

Diagnose before cleaning:

* Run with `--info` to see why tasks are not up to date. `generateDebugResources UP-TO-DATE`
  is an incremental check, not a regeneration. Unexpected inputs such as `ic_carplay 3.png`
  under `build/intermediates/packaged_res` indicate polluted output. Check the source tree
  separately before touching any files.
* `ls -lO` shows macOS `dataless` files. Access can require File Provider materialization.
  Numbered copies and dataless flags are evidence of externally managed output; they are
  not normal Gradle resource names. They do not alone identify which sync client created them.
* NDK r28's `build/core/build-all.mk` includes `$(wildcard .../*.d)`. A conflict copy named
  `local_hotspot_radio.o 3.d` expands into separate names, including `local_hotspot_radio.o`.
  Make then reads the ELF binary as a Makefile and reports NUL bytes and `missing separator`,
  even during `clean`. This does not mean the C source has a syntax error.

Switch to fresh external output instead of relying on native `clean` to repair a polluted
dependency directory. Existing build folders and source modifications are left untouched.
Inspect or archive old generated folders separately; changing `.gitignore` does not exclude
files from a cloud sync service.

To check incrementality without rerunning the test suite, run the following twice without
editing inputs. The second run should report both resource tasks as `UP-TO-DATE`:

```sh
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew \
  -Pdiplay.buildRoot=/private/tmp/diplay-local-build \
  --project-cache-dir /private/tmp/diplay-project-cache \
  :common:packageDebugResources :shared:packageDebugResources --info
```

The backup server's connection-ending race test uses immediate-reset clients (`SO_LINGER=0`)
to avoid macOS graceful-close delays, retaining 1000 iterations with a 30-second timeout.
This affects only those test clients. Other backup
server tests still cover ordinary HTTP delivery, idle clients, and listener shutdown.
