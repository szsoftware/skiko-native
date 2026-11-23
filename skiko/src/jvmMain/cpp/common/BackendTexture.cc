#include <iostream>
#include <jni.h>
#include "ganesh/GrBackendSurface.h"
#include "SkData.h"
#include "SkImage.h"
#include <ganesh/gl/GrGLBackendSurface.h>
#include "ganesh/gl/GrGLBackendSurface.h"
#include "include/gpu/ganesh/SkImageGanesh.h"
#include "ganesh/GrDirectContext.h"
#include "ganesh/gl/GrGLDirectContext.h"
#ifdef SK_METAL
#include "ganesh/mtl/GrMtlBackendSurface.h"
#include "ganesh/mtl/GrMtlTypes.h"
#endif


static void deleteBackendTexture(GrBackendTexture* rt) {
    // std::cout << "Deleting [GrBackendTexture " << rt << "]" << std::endl;
    delete rt;
}

extern "C" JNIEXPORT jlong JNICALL Java_org_jetbrains_skia_BackendTextureKt_BackendTexture_1nGetFinalizer
  (JNIEnv* env, jclass jclass) {
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(&deleteBackendTexture));
}

extern "C" JNIEXPORT jlong JNICALL Java_org_jetbrains_skia_BackendTextureKt__1nMakeGL
  (JNIEnv* env, jclass jclass, jint width, jint height, jboolean isMipmapped, jint textureId, jint target, jint format) {
    GrGLTextureInfo textureInfo;
    textureInfo.fID = static_cast<GrGLuint>(textureId);
    textureInfo.fTarget = static_cast<GrGLenum>(target);
    textureInfo.fFormat = static_cast<GrGLenum>(format);

    GrBackendTexture obj = GrBackendTextures::MakeGL(
        width,
        height,
        isMipmapped ? skgpu::Mipmapped::kYes : skgpu::Mipmapped::kNo,
        textureInfo
    );

    GrBackendTexture* instance = new GrBackendTexture(obj);
    return reinterpret_cast<jlong>(instance);
}

extern "C" JNIEXPORT void JNICALL Java_org_jetbrains_skia_BackendTextureKt__1nGLTextureParametersModified
  (JNIEnv* env, jclass jclass, jlong backendTexturePtr) {
    GrBackendTexture* backendTexture = reinterpret_cast<GrBackendTexture*>(static_cast<uintptr_t>(backendTexturePtr));
    GrBackendTextures::GLTextureParametersModified(backendTexture);
}

extern "C" JNIEXPORT jlong JNICALL Java_org_jetbrains_skia_BackendTextureKt__1nMakeMetal
  (JNIEnv* env, jclass jclass, jint width, jint height, jboolean isMipmapped, jlong texturePtr) {
#ifdef SK_METAL
    GrMtlTextureInfo textureInfo;
    textureInfo.fTexture.retain(reinterpret_cast<GrMTLHandle>(static_cast<uintptr_t>(texturePtr)));

    GrBackendTexture obj = GrBackendTextures::MakeMtl(
        width,
        height,
        isMipmapped ? skgpu::Mipmapped::kYes : skgpu::Mipmapped::kNo,
        textureInfo
    );
    if (!obj.isValid()) {
        return 0;
    }
    return reinterpret_cast<jlong>(new GrBackendTexture(obj));
#else
    return 0;
#endif
}
