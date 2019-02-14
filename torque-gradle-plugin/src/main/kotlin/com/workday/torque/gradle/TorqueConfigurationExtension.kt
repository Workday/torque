package com.workday.torque.gradle

import com.workday.torque.Args
import org.gradle.api.Action
import org.gradle.api.Project

open class TorqueConfigurationExtension(var variantName: String = "", val args: Args = Args()) {
    constructor(project: Project): this()

    fun args(action: Action<Args>) {
        action.execute(args)
    }
}
