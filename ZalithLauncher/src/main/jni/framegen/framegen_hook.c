#include "framegen_hook.h"

#include <dlfcn.h>
#include <stdbool.h>
#include <string.h>
#include <android/log.h>
#include <EGL/egl.h>

#define LOG_TAG "FrameGen"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_enabled = false;
static bool g_ready = false;
static GLuint g_captureFBO = 0;
static GLuint g_captureTex = 0;
static int g_winW = 0;
static int g_winH = 0;

/* Resolved GL function pointers */
static void (*f_glGenFramebuffers)(GLsizei, GLuint*);
static void (*f_glDeleteFramebuffers)(GLsizei, const GLuint*);
static void (*f_glBindFramebuffer)(GLenum, GLuint);
static void (*f_glFramebufferTexture2D)(GLenum, GLenum, GLenum, GLuint, GLint);
static GLenum (*f_glCheckFramebufferStatus)(GLenum);
static void (*f_glGenTextures)(GLsizei, GLuint*);
static void (*f_glDeleteTextures)(GLsizei, const GLuint*);
static void (*f_glBindTexture)(GLenum, GLuint);
static void (*f_glTexImage2D)(GLenum, GLint, GLint, GLsizei, GLsizei, GLint, GLenum, GLenum, const void*);
static void (*f_glTexParameteri)(GLenum, GLenum, GLint);
static void (*f_glBlitFramebuffer)(GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLbitfield, GLenum);
static void (*f_glGetIntegerv)(GLenum, GLint*);

static int resolve_gl(void) {
    if (f_glGenFramebuffers) return 1;

    void* lib = dlopen("libGLESv3.so", RTLD_LAZY | RTLD_NOLOAD);
    if (!lib) lib = dlopen("libGLESv2.so", RTLD_LAZY | RTLD_NOLOAD);
    if (!lib) { LOGE("No GLES lib"); return -1; }

    f_glGenFramebuffers      = dlsym(lib, "glGenFramebuffers");
    f_glDeleteFramebuffers   = dlsym(lib, "glDeleteFramebuffers");
    f_glBindFramebuffer      = dlsym(lib, "glBindFramebuffer");
    f_glFramebufferTexture2D = dlsym(lib, "glFramebufferTexture2D");
    f_glCheckFramebufferStatus = dlsym(lib, "glCheckFramebufferStatus");
    f_glGenTextures          = dlsym(lib, "glGenTextures");
    f_glDeleteTextures       = dlsym(lib, "glDeleteTextures");
    f_glBindTexture          = dlsym(lib, "glBindTexture");
    f_glTexImage2D           = dlsym(lib, "glTexImage2D");
    f_glTexParameteri        = dlsym(lib, "glTexParameteri");
    f_glBlitFramebuffer      = dlsym(lib, "glBlitFramebuffer");
    f_glGetIntegerv          = dlsym(lib, "glGetIntegerv");

    if (!f_glGenFramebuffers || !f_glBlitFramebuffer) {
        LOGE("Missing critical GL functions");
        return -1;
    }
    LOGD("Resolved GLES functions");
    return 1;
}

void framegen_set_enabled(bool enabled) {
    g_enabled = enabled;
    if (!enabled) {
        if (g_captureFBO && f_glDeleteFramebuffers) f_glDeleteFramebuffers(1, &g_captureFBO);
        if (g_captureTex && f_glDeleteTextures) f_glDeleteTextures(1, &g_captureTex);
        g_captureFBO = 0;
        g_captureTex = 0;
        g_ready = false;
    }
    LOGD("FrameGen %s", enabled ? "ON" : "OFF");
}

bool framegen_is_enabled(void) {
    return g_enabled;
}

static int ensure(int w, int h) {
    if (resolve_gl() < 0) return -1;
    if (g_ready && g_winW == w && g_winH == h) return 1;

    if (g_captureFBO) f_glDeleteFramebuffers(1, &g_captureFBO);
    if (g_captureTex) f_glDeleteTextures(1, &g_captureTex);
    g_captureFBO = 0;
    g_captureTex = 0;
    g_ready = false;

    f_glGenTextures(1, &g_captureTex);
    f_glBindTexture(GL_TEXTURE_2D, g_captureTex);
    f_glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
    f_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    f_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    f_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    f_glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    f_glGenFramebuffers(1, &g_captureFBO);
    f_glBindFramebuffer(GL_FRAMEBUFFER, g_captureFBO);
    f_glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, g_captureTex, 0);

    GLenum status = f_glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        LOGE("FBO incomplete: 0x%x", status);
        f_glDeleteFramebuffers(1, &g_captureFBO);
        f_glDeleteTextures(1, &g_captureTex);
        g_captureFBO = 0;
        g_captureTex = 0;
        return -1;
    }

    g_winW = w;
    g_winH = h;
    g_ready = true;
    LOGD("FBO ready %dx%d", w, h);
    return 1;
}

void framegen_capture(EGLDisplay dpy, EGLSurface surface) {
    if (!g_enabled) return;
    EGLint w, h;
    if (!eglQuerySurface(dpy, surface, EGL_WIDTH, &w) || !eglQuerySurface(dpy, surface, EGL_HEIGHT, &h))
        return;
    if (w <= 0 || h <= 0) return;
    if (ensure(w, h) < 0) return;

    GLint prevRead = 0, prevDraw = 0;
    f_glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, &prevRead);
    f_glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, &prevDraw);

    f_glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
    f_glBindFramebuffer(GL_DRAW_FRAMEBUFFER, g_captureFBO);
    f_glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);

    f_glBindFramebuffer(GL_READ_FRAMEBUFFER, prevRead);
    f_glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDraw);
}

void framegen_double(EGLDisplay dpy, EGLSurface surface) {
    if (!g_enabled || !g_ready) return;

    GLint prevRead = 0, prevDraw = 0;
    f_glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, &prevRead);
    f_glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, &prevDraw);

    f_glBindFramebuffer(GL_READ_FRAMEBUFFER, g_captureFBO);
    f_glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    f_glBlitFramebuffer(0, 0, g_winW, g_winH, 0, 0, g_winW, g_winH, GL_COLOR_BUFFER_BIT, GL_NEAREST);

    f_glBindFramebuffer(GL_READ_FRAMEBUFFER, prevRead);
    f_glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDraw);

    if (!eglSwapBuffers(dpy, surface)) {
        LOGD("Double swap failed (surface may have died)");
    }
}
