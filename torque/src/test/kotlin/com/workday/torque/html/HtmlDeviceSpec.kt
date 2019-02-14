package com.workday.torque.html

import com.workday.torque.Device
import com.workday.torque.testFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class HtmlDeviceSpec : Spek({

    context("Device.toHtmlDevice") {

        val device = Device(id = "testDevice1", logcatFile = testFile(), model = "testModel1")

        val htmlDevice = device.toHtmlDevice(testFile().parentFile)

        it("converts Device to HtmlDevice") {
            assertThat(htmlDevice).isEqualTo(HtmlDevice(
                    id = device.id,
                    model = device.model,
                    logcatPath = device.logcatFile.name
            ))
        }
    }
})
