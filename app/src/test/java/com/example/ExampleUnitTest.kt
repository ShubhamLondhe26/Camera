package com.example

import com.example.camera.AspectRatioMode
import com.example.camera.CameraMode
import com.example.camera.FlashMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testCameraModes() {
    assertEquals(4, CameraMode.entries.size)
    assertEquals(CameraMode.PHOTO, CameraMode.fromIndex(0))
    assertEquals(CameraMode.PORTRAIT, CameraMode.fromIndex(1))
    assertEquals(CameraMode.NIGHT, CameraMode.fromIndex(2))
    assertEquals(CameraMode.VIDEO, CameraMode.fromIndex(3))

    assertTrue(CameraMode.VIDEO.requiresAudio)
    assertFalse(CameraMode.PHOTO.requiresAudio)
    assertFalse(CameraMode.PORTRAIT.requiresAudio)
    assertFalse(CameraMode.NIGHT.requiresAudio)
  }

  @Test
  fun testFlashCycling() {
    assertEquals(FlashMode.AUTO, FlashMode.OFF.next())
    assertEquals(FlashMode.ON, FlashMode.AUTO.next())
    assertEquals(FlashMode.TORCH, FlashMode.ON.next())
    assertEquals(FlashMode.OFF, FlashMode.TORCH.next())
  }

  @Test
  fun testAspectRatioCycling() {
    assertEquals(AspectRatioMode.RATIO_16_9, AspectRatioMode.RATIO_4_3.next())
    assertEquals(AspectRatioMode.RATIO_1_1, AspectRatioMode.RATIO_16_9.next())
    assertEquals(AspectRatioMode.RATIO_FULL, AspectRatioMode.RATIO_1_1.next())
    assertEquals(AspectRatioMode.RATIO_4_3, AspectRatioMode.RATIO_FULL.next())
  }
}

