# kotlin-script-editor

A GUI tool that allows users to enter a script, execute it, and see its output side-by-side.

## Building and Running the project

To build the project, execute:

```shell
./gradlew package
```

The executable can be found in `build/compose/binaries/main`.

Note that binaries built on macOS cannot be run directly because they are not signed.
One can right-click the file and click 'Open' to open without the signature check.

## Configuring Kotlin Compiler

The application requires the Kotlin compiler to be pre-installed.
Path to the compiler can be specified using JVM argument `-Dscript.runner.kotlinc=path-to-compiler`.
It uses `kotlinc` from system PATH by default.
