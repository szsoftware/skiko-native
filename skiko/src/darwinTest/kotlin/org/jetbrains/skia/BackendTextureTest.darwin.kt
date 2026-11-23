package org.jetbrains.skia

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.objcPtr
import org.jetbrains.skia.impl.Native.Companion.NullPointer
import org.jetbrains.skia.impl.use
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.Metal.MTLPixelFormatRGBA8Unorm
import platform.Metal.MTLTextureDescriptor
import platform.Metal.MTLTextureUsageRenderTarget
import platform.Metal.MTLTextureUsageShaderRead
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class BackendTextureDarwinTest {
    @Test
    fun metalTextureIsBorrowedWithoutReadback() {
        listOf(
            MTLPixelFormatRGBA8Unorm to ColorType.RGBA_8888,
            MTLPixelFormatBGRA8Unorm to ColorType.BGRA_8888,
        ).forEach { (pixelFormat, colorType) ->
            withTestMetalContext { device, context ->
                val descriptor = MTLTextureDescriptor.texture2DDescriptorWithPixelFormat(
                    pixelFormat, 8uL, 8uL, false
                )
                descriptor.usage = MTLTextureUsageRenderTarget or MTLTextureUsageShaderRead
                val texture = checkNotNull(device.newTextureWithDescriptor(descriptor))

                // An external renderer writes the texture: top half red, bottom half blue.
                BackendRenderTarget.makeMetal(8, 8, texture.objcPtr()).use { target ->
                    val format =
                        if (colorType == ColorType.RGBA_8888) SurfaceColorFormat.RGBA_8888
                        else SurfaceColorFormat.BGRA_8888
                    assertNotNull(
                        Surface.makeFromBackendRenderTarget(
                            context, target, SurfaceOrigin.TOP_LEFT, format, null
                        )
                    ).use { surface ->
                        surface.canvas.clear(Color.BLUE)
                        surface.canvas.drawRect(Rect.makeWH(8f, 4f), Paint().apply { color = Color.RED })
                        context.flushAndSubmit(surface, true)
                    }
                }

                // Compose borrows it and draws it into its own GPU surface.
                BackendTexture.makeMetal(8, 8, texture.objcPtr()).use { backendTexture ->
                    Image.borrowTextureFrom(
                        context,
                        backendTexture,
                        SurfaceOrigin.TOP_LEFT,
                        colorType,
                        ColorAlphaType.PREMUL,
                    ).use { image ->
                        assertEquals(8, image.width)
                        assertEquals(8, image.height)
                        Surface.makeRenderTarget(context, false, ImageInfo.makeN32Premul(8, 8)).use { surface ->
                            surface.canvas.clear(Color.WHITE)
                            surface.canvas.drawImage(image, 0f, 0f)
                            context.flushAndSubmit(surface, true)
                            Bitmap().use { bitmap ->
                                assertTrue(bitmap.allocN32Pixels(8, 8))
                                assertTrue(surface.readPixels(bitmap, 0, 0))
                                assertEquals(Color.RED, bitmap.getColor(4, 1))
                                assertEquals(Color.BLUE, bitmap.getColor(4, 6))
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun nullMetalTextureIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            BackendTexture.makeMetal(8, 8, NullPointer)
        }
    }
}
