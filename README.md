## Torque — Reactive Android instrumentation test orchestrator with multi-library-modules-testing and test pooling/grouping support.

Torque is a test orchestrator that aims to support the following features:
* Support running [Android Library Module](https://developer.android.com/studio/projects/android-library) test Apks. 
* Parallel test execution on multiple emulators/devices with **pooled** test chunking support.
* Test grouping to reduce overhead between every `am instrument` call
* Logcat output capturing per test and for whole test run as well.
* Report generation.
* JUnit4 report generation. 

The library is based on [gojuno/composer][composer], hats off to the gojuno team for their great work, see below for details.


![Demo](demo/composer.gif)

### Table of Contents

- [Why we've decided to replace shazam/fork and fork gojuno/composer](#why-weve-decided-to-replace-shazamfork-and-fork-gojunocomposer)
- [Chunks and Pool](#chunks-and-pool)
- [Usage](#usage)
- [How to build](#how-to-build)
- [License](#license)

### Why we've decided to replace shazam/fork and fork gojuno/composer

**Problem 1:** To solve the issues of separating concerns, cross-team collaboration and decrease our incremental build times. 
We are in the process of breaking up our gigantic monolithic app into smaller modules. The Library modules would need to 
contain their own instrumentation tests, so that different module teams may write their own tests for their own independent modules.
[shazam/fork][fork], the library we were using, does not support library module test apks, as well as all other libraries that we've looked at.
Android Gradle Plugin provided some functionality, but it was hard to customize and adjust.

**Solution:** We modified [gojuno/composer][composer] to support multiple module-test-apks, and added [shazam/fork][fork]'s 
pooling functionality to it.  

**Problem 2:** Opening up PRs against either of the libraries would have resulted in a much slower velocity for getting everything working as we needed.

**Solution:** We based off of [gojuno/composer][composer] as it dealt with a lot of details on reporting and running test commands, and we built the logic we needed on top of it.

## Chunks and Pool

Torque utilizes a concept called "Chunks". Similar to "Shards",
each chunk represents a group of tests, but what's different from shards
is that a chunk can be of any defined size (see [Usage](#usage)),
so that we can have more chunks than devices available.
This allows Torque to keep all devices busy until there's no more chunks
left in the test chunk pool.
Every chunk requires ~1 seconds of overhead (an adb call) to start the test app process for testing.
If you have thousands of tests or more, the overhead would be significant.
The *more* tests in a chunk the *less* overhead overall, but the test
app process is more prone to crashing if any of the tests didn't do teardown correctly.
You'd also need to have at least 2 times more chunks than the number of
devices available to leverage the pooling.


## Usage

Torque is shipped as jar for now, you can run it directly (you need JVM 1.8+): `java -jar torque-latest-version.jar [options]`.
or
With the Gradle plugin use `torque-gradle-plugin-latest-version.jar`.


#### Supported options

See [Args.kt](torque/src/main/kotlin/com/workday/torque/Args.kt)

##### Example (Run directly)

Simplest :
```console
java -jar torque-latest-version.jar \
--test-apks myLibrary1-androidTest.apk myLibrary2-androidTest.apk
```

With arguments :
```console
java -jar torque-latest-version.jar \
--test-apks myLibrary1-androidTest.apk myLibrary2-androidTest.apk \
--annotations MediumTest \
--chunk-size 8 \
--output-directory artifacts/torque-output
```

##### Example (Gradle plugin)
In the project's `build.gradle`
```
dependencies {
    implementation files('../torque-gradle-plugin.jar')
    implementation files('../torque.jar')
}
```
In App and Library projects that will be tested and would provide test apks.
```
apply plugin: 'com.workday.torque'
torque {
    variantName "ProdDebug"    // Required
    args {
        outputDirectory = "torque-output/"
        resultFilePath = "torque-result.txt"
        chunkSize = 1
        chunkTimeoutSeconds = 150
        ...
    }
}
```
`variantName` is the `<product-flavor><Build-Type>`, for example: demoDebug, fullRelease

`args` is a dsl for applying any optional params from [Args.kt](torque/src/main/kotlin/com/workday/torque/Args.kt), except the apk paths, which are parsed by the plugin, and only followed when the `torqueRun_` task is ran on that project (see below).

When the plugin is applied on an App module, it will create a `torqueRunAll` task which runs tests on all modules (including the App) that have the plugin applied.

When the plugin is applied on a Library module, it will create a `torqueRunLibrary` task which runs tests on just that Library module.

### How to build

#### Build Torque

Environment variable `ANDROID_HOME` must be set.

```console
./gradlew build
```

## License

```
Copyright 2018 Workday, Inc.
Copyright 2017 Juno, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[composer]: https://github.com/gojuno/composer
[fork]: https://github.com/shazam/fork
[test sharding]: https://developer.android.com/training/testing/junit-runner.html#sharding-tests
