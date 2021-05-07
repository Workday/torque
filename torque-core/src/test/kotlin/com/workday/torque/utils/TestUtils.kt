package com.workday.torque.utils

import com.linkedin.dex.parser.DecodedValue
import com.linkedin.dex.parser.TestAnnotation
import com.linkedin.dex.parser.TestMethod
import com.workday.torque.pooling.TestModule
import org.assertj.core.api.Assertions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val THROWS_EXCEPTION_ANNOTATION = TestAnnotation("dalvik.annotation.Throws", values = mapOf("value" to DecodedValue.DecodedArrayValue(values= arrayOf(DecodedValue.DecodedType(value="Ljava/lang/Exception;")))), inherited = false)
val TEST_ANNOTATION = TestAnnotation(name = "org.junit.Test", values = mapOf(), inherited = false)
val RUNS_WITH_ANDROID_ANNOTATION = TestAnnotation("org.junit.runner.RunWith", values = mapOf("value" to DecodedValue.DecodedType(value="Landroid/support/test/runner/AndroidJUnit4;")), inherited = true)
val MEDIUM_TEST_ANNOTATION = TestAnnotation("android.support.test.filters.MediumTest", values = mapOf(), inherited = false)
val METADATA_ANNOTATION = TestAnnotation("kotlin.Metadata", values = mapOf(), inherited = false)
val FLAKY_TEST_ANNOTATION = TestAnnotation("android.support.test.filters.FlakyTest", values = mapOf(), inherited = false)
val IGNORE_TEST_ANNOTATION = TestAnnotation("org.junit.Ignore", values = mapOf(), inherited = false)

fun createTestMethodsList(size: Int): MutableList<TestMethod> {
    return MutableList(size) { TestMethod("test#somemethod", listOf(TestAnnotation("org.junit.Test", values = mapOf(), inherited = false))) }
}

fun assertTestModules(expectedModules: List<TestModule>, actualTestModule: List<TestModule>) {
    expectedModules.zip(actualTestModule).forEach { (expected, actual) ->
        Assertions.assertThat(expected.testModuleInfo).isEqualTo(actual.testModuleInfo)
        expected.testMethods.zip(actual.testMethods).forEach { (expected, actual) ->
            assertEquals(expected.testName, actual.testName)
            assertAnnotationArrays(expected.annotations, actual.annotations)
        }
    }
}

fun assertAnnotationArrays(expectedAnnotations: List<TestAnnotation>, actualAnnotations: List<TestAnnotation>) {
    expectedAnnotations.zip(actualAnnotations).forEach { (expected, actual) ->
        expected.assertAnnotations(actual)
    }
}

private fun TestAnnotation.assertAnnotations(other: TestAnnotation) {
    assertEquals(name, other.name)
    assertEquals(inherited, other.inherited)
    values.forEach { (key, value) ->
        assertTrue(other.values.containsKey(key))
        val otherValue = other.values.getValue(key)
        value.assertDecodeValue(otherValue)
    }
}

private fun DecodedValue.assertDecodeValue(other: DecodedValue) {
    if(this is DecodedValue.DecodedArrayValue && other is DecodedValue.DecodedArrayValue) {
        assertTrue(values.contentDeepEquals(other.values))
    } else {
        assertEquals(this, other)
    }
}
