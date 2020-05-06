package com.workday.torque.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import com.workday.torque.Args
import com.workday.torque.Torque
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

private const val TORQUE_EXTENSION_NAME = "torque"
private const val TORQUE_TASK_NAME = "torque"
private const val RUN_ALL_TASK_NAME = "RunAll"
private const val RUN_LIBRARY_TASK_NAME = "RunLibrary"
private const val SETUP_TASK_NAME = "Setup"
private const val EXTRA_PROPERTY_TEST_APK_PATH = "testApkPath"
private const val ANDROID_TEST_SUFFIX = "AndroidTest"

class TorquePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidTestedExtension =
                requireNotNull(project.extensions.getByType(TestedExtension::class.java)) {
                    "Failed to find android extension"
                }

        val torqueExtension = project.extensions.create(TORQUE_EXTENSION_NAME, TorqueConfigurationExtension::class.java)
        createSetupTask(project, androidTestedExtension, torqueExtension)

        if (androidTestedExtension is AppExtension) {
            createRunAllTask(project, androidTestedExtension, torqueExtension)
        } else if (androidTestedExtension is LibraryExtension) {
            createRunLibraryTask(project, torqueExtension)
        }
    }

    private fun createSetupTask(project: Project, androidExtension: TestedExtension, torqueExtension: TorqueConfigurationExtension) {
        project.task(TORQUE_TASK_NAME + SETUP_TASK_NAME) { task ->
            task.doLast {
                androidExtension.testVariants
                        .filterVariantOutputFile(torqueExtension.variantName + ANDROID_TEST_SUFFIX)
                        .run { task.extensions.extraProperties.set(EXTRA_PROPERTY_TEST_APK_PATH, path) }
            }
            task.dependsOn("assemble${torqueExtension.variantName}$ANDROID_TEST_SUFFIX")
        }
    }

    private fun Set<BaseVariant>.filterVariantOutputFile(variantName: String): File {
        return single { variant -> variant.name.capitalize() == variantName }
                .outputs.single().outputFile
    }

    private fun createRunAllTask(project: Project,
                                 appExtension: AppExtension,
                                 torqueExtension: TorqueConfigurationExtension
    ) {
        val torqueArgs = torqueExtension.args
        project.tasks.register(TORQUE_TASK_NAME + RUN_ALL_TASK_NAME, TorqueRunTask::class.java) { task ->
            val torqueSetupTasks = project.rootProject.getTasksByName(TORQUE_TASK_NAME + SETUP_TASK_NAME, true)
            task.doLast {
                torqueArgs.testApkPaths = torqueSetupTasks.map { torqueConfigureTask ->
                    torqueConfigureTask.extensions.extraProperties.get(EXTRA_PROPERTY_TEST_APK_PATH) as String
                }
                torqueArgs.appApkPath = appExtension.applicationVariants
                        .filterVariantOutputFile(torqueExtension.variantName).path

                configureTaskOptionsAndRun(torqueArgs, task)
            }
            task.dependsOn(listOf(torqueSetupTasks, "assemble${torqueExtension.variantName}"))
        }
    }

    private fun createRunLibraryTask(project: Project, torqueExtension: TorqueConfigurationExtension) {
        val torqueArgs = torqueExtension.args
        project.tasks.register(TORQUE_TASK_NAME + RUN_LIBRARY_TASK_NAME, TorqueRunTask::class.java) { task ->
            val torqueSetupTask = project.getTasksByName(TORQUE_TASK_NAME + SETUP_TASK_NAME, true).single()
            task.doLast {
                val testApk = torqueSetupTask.extensions.extraProperties.get(EXTRA_PROPERTY_TEST_APK_PATH) as String
                torqueArgs.testApkPaths = listOf(testApk)

                configureTaskOptionsAndRun(torqueArgs, task)
            }
            task.dependsOn(torqueSetupTask)
        }
    }

    private fun configureTaskOptionsAndRun(torqueArgs: Args, task: TorqueRunTask) {
        torqueArgs.configureTaskOptions(task)
        println("Starting Torque run with args: $torqueArgs")
        Torque(torqueArgs).run()
    }

    private fun Args.configureTaskOptions(task: TorqueRunTask) {
        if (task.annotations.isNotEmpty()) {
            annotations = task.annotations
        }

        if (task.notAnnotations.isNotEmpty()) {
            notAnnotations = task.notAnnotations
        }

        if (task.testClassRegexes.isNotEmpty()) {
            testClassRegexes = task.testClassRegexes
        }
    }
}
