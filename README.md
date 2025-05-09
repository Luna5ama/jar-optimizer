# JarOptimizer
Simple jar optimizing tool that removes unused classes.

## Command line arguments
| Argument                | Value            | Effect                             |
|-------------------------|------------------|------------------------------------|
| `-i` / `-in` / `-input` | directory        | Specifies the input file.          |
| `-o` / `-out` / `-out`  | directory        | Specifies the output file.         |
| `-k` or `-keep`         | package or class | Specifies classes/packages to keep |

## Gradle Plugin Example Usage
```kotlin
val optimizedSomeJar = jarOptimizer.register(someJarTask, "package.to.keep")

// Optional, adding the optimized jar to project artifacts so it is included in gradle build
artifacts {
    archives(optimizedSomeJar)
}
```