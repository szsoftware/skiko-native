package org.jetbrains.skia

import org.jetbrains.skia.impl.*
import org.jetbrains.skia.impl.Library.Companion.staticLoad
import org.jetbrains.skia.impl.Native.Companion.NullPointer
import org.jetbrains.skiko.RenderException

class BackendTexture internal constructor(ptr: NativePointer) : Managed(ptr, _FinalizerHolder.PTR) {
    companion object {
        /**
         * Creates BackendTexture from GL texture.
         *
         * @param width - width of the [BackendTexture] to be created
         * @param height - height of the [BackendTexture] to be created
         * @param isMipmapped - if the passed [textureId] has a GL mipmap, this should be true, otherwise false
         * @param textureId - GL id of the texture to use
         * @param textureTarget - GL enum, must be valid texture target for 2D, e.g. GL_TEXTURE_2D
         * @param textureFormat - GL enum, must be valid color format, e.g. GL_RGBA or GL_BGRA_INTEGER
         *
         * @see glTextureParametersModified
         */
        fun makeGL(
            width: Int,
            height: Int,
            isMipmapped: Boolean,
            textureId: Int,
            textureTarget: Int,
            textureFormat: Int
        ): BackendTexture {
            Stats.onNativeCall()
            val ptr = _nMakeGL(
                width,
                height,
                isMipmapped,
                textureId,
                textureTarget,
                textureFormat
            )
            return BackendTexture(ptr)
        }

        /**
         * Creates BackendTexture from a Metal texture.
         *
         * The texture is retained by the returned object and by every Skia image made from it, so
         * the caller may release its own reference while Skia still uses the texture.
         *
         * @param width - width of the [BackendTexture] to be created
         * @param height - height of the [BackendTexture] to be created
         * @param texturePtr - `id<MTLTexture>` to wrap; must not be zero
         * @param isMipmapped - whether [texturePtr] has a full mipmap chain
         *
         * @throws RenderException if Skia was built without Metal or rejects the texture
         */
        fun makeMetal(
            width: Int,
            height: Int,
            texturePtr: NativePointer,
            isMipmapped: Boolean = false,
        ): BackendTexture {
            require(texturePtr != NullPointer) { "texturePtr must not be null" }
            Stats.onNativeCall()
            val ptr = _nMakeMetal(width, height, isMipmapped, texturePtr)
            if (ptr == NullPointer) throw RenderException("Can't create Metal BackendTexture")
            return BackendTexture(ptr)
        }

        init {
            staticLoad()
        }
    }

    /**
     * Call this to indicate to DirectContext that the texture parameters have been modified in the GL context externally
     */
    fun glTextureParametersModified() {
        return try {
            Stats.onNativeCall()
            _nGLTextureParametersModified(getPtr(this))
        } finally {
            reachabilityBarrier(this)
        }
    }

    private object _FinalizerHolder {
        val PTR = BackendTexture_nGetFinalizer()
    }
}

@ExternalSymbolName("org_jetbrains_skia_BackendTexture__1nGetFinalizer")
// No ForceNativeThreadState: returns the function that frees the object, without calling it, so it is short and cannot
// block.
// Native call path: deleteBackendTexture
private external fun BackendTexture_nGetFinalizer(): NativePointer

@ExternalSymbolName("org_jetbrains_skia_BackendTexture__1nMakeGL")
// Needs ForceNativeThreadState: wraps a GL texture ID from the backend in a texture object.
// Native call path: GrBackendTextures::MakeGL
@ForceNativeThreadState
private external fun _nMakeGL(
    width: Int,
    height: Int,
    isMipmapped: Boolean,
    textureId: Int,
    target: Int,
    format: Int
): NativePointer

@ExternalSymbolName("org_jetbrains_skia_BackendTexture__1nMakeMetal")
// Needs ForceNativeThreadState: retains a Metal texture through CoreFoundation and reads its format through
// Objective-C.
// Native call path: GrBackendTextures::MakeMtl
@ForceNativeThreadState
private external fun _nMakeMetal(
    width: Int,
    height: Int,
    isMipmapped: Boolean,
    texturePtr: NativePointer
): NativePointer

@ExternalSymbolName("org_jetbrains_skia_BackendTexture__1nGLTextureParametersModified")
// No ForceNativeThreadState: only resets cached texture parameter fields, which are plain stored values, so it is short
// and cannot block.
// Native call path: GLTextureParametersModified -> invalidate
private external fun _nGLTextureParametersModified(backendTexturePtr: NativePointer)
