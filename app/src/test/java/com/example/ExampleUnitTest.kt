package com.example

import com.example.ui.screens.formatFileSize
import com.example.ui.screens.validateTargetSize
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PDF and photo compression utilities.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testValidateTargetSize_validInputs() {
    // 50 KB
    val (b50kb, err50kb) = validateTargetSize("50", "KB")
    assertNull(err50kb)
    assertEquals(50 * 1024L, b50kb)

    // 100 KB
    val (b100kb, err100kb) = validateTargetSize("100", "KB")
    assertNull(err100kb)
    assertEquals(100 * 1024L, b100kb)

    // 250 KB
    val (b250kb, err250kb) = validateTargetSize("250", "KB")
    assertNull(err250kb)
    assertEquals(250 * 1024L, b250kb)

    // 500 KB
    val (b500kb, err500kb) = validateTargetSize("500", "KB")
    assertNull(err500kb)
    assertEquals(500 * 1024L, b500kb)

    // 1 MB
    val (b1mb, err1mb) = validateTargetSize("1", "MB")
    assertNull(err1mb)
    assertEquals(1024L * 1024L, b1mb)

    // 2 MB
    val (b2mb, err2mb) = validateTargetSize("2", "MB")
    assertNull(err2mb)
    assertEquals(2L * 1024L * 1024L, b2mb)

    // 5 MB
    val (b5mb, err5mb) = validateTargetSize("5", "MB")
    assertNull(err5mb)
    assertEquals(5L * 1024L * 1024L, b5mb)

    // 10 MB
    val (b10mb, err10mb) = validateTargetSize("10", "MB")
    assertNull(err10mb)
    assertEquals(10L * 1024L * 1024L, b10mb)

    // Decimal 1.5 MB
    val (b1_5mb, err1_5mb) = validateTargetSize("1.5", "MB")
    assertNull(err1_5mb)
    assertEquals((1.5 * 1024 * 1024).toLong(), b1_5mb)
  }

  @Test
  fun testValidateTargetSize_invalidInputs() {
    // Empty
    val (bEmpty, errEmpty) = validateTargetSize("", "KB")
    assertNull(bEmpty)
    assertNotNull(errEmpty)

    // Non-numeric
    val (bNonNum, errNonNum) = validateTargetSize("abc", "KB")
    assertNull(bNonNum)
    assertNotNull(errNonNum)

    // Zero
    val (bZero, errZero) = validateTargetSize("0", "KB")
    assertNull(bZero)
    assertNotNull(errZero)

    // Negative
    val (bNeg, errNeg) = validateTargetSize("-50", "KB")
    assertNull(bNeg)
    assertNotNull(errNeg)

    // Extremely large (> 100 MB)
    val (bHuge, errHuge) = validateTargetSize("200", "MB")
    assertNull(bHuge)
    assertNotNull(errHuge)

    // Extremely small (< 5 KB)
    val (bTiny, errTiny) = validateTargetSize("2", "KB")
    assertNull(bTiny)
    assertNotNull(errTiny)
  }

  @Test
  fun testFormatFileSize() {
    assertEquals("0 B", formatFileSize(0))
    assertEquals("500 B", formatFileSize(500))
    assertEquals("50.0 KB", formatFileSize(50 * 1024))
    assertEquals("1.0 MB", formatFileSize(1024 * 1024))
  }
}
