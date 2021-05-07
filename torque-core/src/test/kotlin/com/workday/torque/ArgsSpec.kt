package com.workday.torque

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class ArgsSpec : Spek(
{

    val rawArgsWithOnlyRequiredFields = arrayOf(
            "--test-apks", "test-apk-path-1", "test-apk-path-2"
    )

    context("parse args with only required params") {

        val args by memoized { parseArgs(rawArgsWithOnlyRequiredFields) }

        it("parses passes testApkPaths and uses default values for other fields") {
            assertThat(args).isEqualTo(Args(
                    testApkPaths = listOf("test-apk-path-1", "test-apk-path-2"),
                    allowedAnnotations = emptyList(),
                    prohibitedAnnotations = DEFAULT_NOT_ANNOTATIONS,
                    testClassRegexes = emptyList(),
                    appApkPath = "",
                    chunkSize = DEFAULT_CHUNK_SIZE,
                    retriesPerChunk = DEFAULT_MAX_RETRIES_PER_CHUNK,
                    verboseOutput = false,
                    installTimeoutSeconds = DEFAULT_INSTALL_TIMEOUT_SECONDS,
                    retriesInstallPerApk = DEFAULT_MAX_INSTALL_RETRIES_PER_APK,
                    resultFilePath = DEFAULT_RESULT_FILE_PATH,
                    chunkTimeoutSeconds = DEFAULT_PER_CHUNK_TIMEOUT_SECONDS,
                    timeoutMinutes = DEFAULT_TORQUE_TIMEOUT_MINUTES
            ))
        }
    }

    context("parse args with explicitly passed --annotations") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--allowed-annotations", "Annotation1", "Annotation2"))
        }

        it("parses --allowed-annotations correctly") {
            assertThat(args.allowedAnnotations).isEqualTo(listOf("Annotation1", "Annotation2"))
        }
    }

    context("parse args with explicitly passed --prohibited-annotations") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--prohibited-annotations", "NotAnnotation1", "NotAnnotation2"))
        }

        it("parses --prohibited-annotations correctly") {
            assertThat(args.prohibitedAnnotations).isEqualTo(listOf("NotAnnotation1", "NotAnnotation2"))
        }
    }

    context("parse args with passed --class-regex") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--class-regex", "(some).*(regex1)",  "(some).*(regex2)"))
        }

        it("parses --class-regex correctly") {
            assertThat(args.testClassRegexes).isEqualTo(listOf("(some).*(regex1)", "(some).*(regex2)"))
        }
    }

    context("parse args with explicitly passed --app-apk") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--app-apk", "app-apk-path"))
        }

        it("parses --app-apk correctly") {
            assertThat(args.appApkPath).isEqualTo("app-apk-path")
        }
    }

    context("parse args with passed --chunk-size") {

      val args by memoized {
          parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--chunk-size", "5"))
      }

      it("parses --chunk-size correctly") {
          assertThat(args.chunkSize).isEqualTo(5)
      }
    }

    context("parse args with explicitly passed --chunk-max-retries") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--chunk-max-retries", "5"))
        }

        it("parses --chunk-max-retries correctly") {
            assertThat(args.retriesPerChunk).isEqualTo(5)
        }
    }

    context("parse args with passed --chunk-timeout") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--chunk-timeout", "120"))
        }

        it("parses --chunk-timeout correctly") {
            assertThat(args.chunkTimeoutSeconds).isEqualTo(120L)
        }
    }

    context("parse args with explicitly passed --verbose-output") {

        listOf(true, false).forEach { verboseOutput ->

            context("--verbose--output $verboseOutput") {

                val args by memoized {
                    parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--verbose-output", "$verboseOutput"))
                }

                it("parses --verbose-output correctly") {
                    assertThat(args.verboseOutput).isEqualTo(verboseOutput)
                }
            }
        }
    }

    context("parse args with passed --install-timeout") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--install-timeout", "600"))
        }

        it("parses --install-timeout correctly") {
            assertThat(args.installTimeoutSeconds).isEqualTo(600)
        }
    }

    context("parse args with explicitly passed --install-max-retries") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--install-max-retries", "3"))
        }

        it("parses --install-max-retries correctly") {
            assertThat(args.retriesInstallPerApk).isEqualTo(3)
        }
    }

    context("parse args with explicitly passed --result-file") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--result-file", "result-file-path"))
        }

        it("parses --result-file correctly") {
            assertThat(args.resultFilePath).isEqualTo("result-file-path")
        }
    }

    context("parse args with passed --timeout") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--timeout", "90"))
        }

        it("parses --timeout correctly") {
            assertThat(args.timeoutMinutes).isEqualTo(90L)
        }
    }

    context("parse args with explicitly passed --test-files-pull-device-directory") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--test-files-pull-device-directory", "deviceDir"))
        }

        it("parses --test-files-pull-device-directory correctly") {
            assertThat(args.testFilesPullDeviceDirectory).isEqualTo("deviceDir")
        }
    }

    context("parse args with passed --test-files-pull-host-directory") {

        val args by memoized {
            parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--test-files-pull-host-directory", "hostDir"))
        }

        it("parses --test-files-pull-host-directory correctly") {
            assertThat(args.testFilesPullHostDirectory).isEqualTo("hostDir")
        }
    }

    context("parse args with explicitly passed --uninstall-apk-after-test") {

        listOf(true, false).forEach { uninstallApkAfterTest ->

            context("--uninstall-apk-after-test $uninstallApkAfterTest") {

                val args by memoized {
                    parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--uninstall-apk-after-test", "$uninstallApkAfterTest"))
                }

                it("parses --uninstall-apk-after-test correctly") {
                    assertThat(args.uninstallApkAfterTest).isEqualTo(uninstallApkAfterTest)
                }
            }
        }
    }

    context("parse args with explicitly passed --record-failed-tests") {

        listOf(true, false).forEach { recordFailedTests ->

            context("--record-failed-tests $recordFailedTests") {

                val args by memoized {
                    parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--record-failed-tests", "$recordFailedTests"))
                }

                it("parses --record-failed-tests correctly") {
                    assertThat(args.recordFailedTests).isEqualTo(recordFailedTests)
                }
            }
        }
    }

    context("parse args with explicitly passed --test-coverage-enabled") {

        listOf(true, false).forEach { testCoverageEnabled ->

            context("--test-coverage-enabled $testCoverageEnabled") {

                val args by memoized {
                    parseArgs(rawArgsWithOnlyRequiredFields + arrayOf("--test-coverage-enabled", "$testCoverageEnabled"))
                }

                it("parses --test-coverage-enabled correctly") {
                    assertThat(args.testCoverageEnabled).isEqualTo(testCoverageEnabled)
                }
            }
        }
    }
})
