//
// Created by maks on 18.10.2023.
//
#include <malloc.h>
#include <stdlib.h>
#include <string.h>
#include <environ/environ.h>
#include <android/log.h>
#include "osm_bridge.h"

static const char* g_LogTag = "GLBridge";
static __thread osm_render_window_t* currentBundle;
// a tiny buffer for rendering when there's nowhere t render
static char no_render_buffer[4];
static bool hasSetNoRendererBuffer = false;

// Its not in a .h file because it is not supposed to be used outsife of this file.
void setNativeWindowSwapInterval(struct ANativeWindow* nativeWindow, int swapInterval);

bool osm_init() {
    dlsym_OSMesa();
    return true; // no more specific initialization required
}

osm_render_window_t* osm_get_current() {
    return currentBundle;
}

osm_render_window_t* osm_init_context(osm_render_window_t* share) {
    osm_render_window_t* render_window = malloc(sizeof(osm_render_window_t));
    if(render_window == NULL) return NULL;
    memset(render_window, 0, sizeof(osm_render_window_t));
    OSMesaContext osmesa_share = NULL;
    if(share != NULL) osmesa_share = share->context;

    OSMesaContext context = NULL;
    const char* pojavRenderer = getenv("POJAV_RENDERER");
    bool needsCoreProfile = pojavRenderer != NULL && !strcmp(pojavRenderer, "vulkan_zink");

    if (needsCoreProfile && OSMesaCreateContextAttribs_p != NULL) {
        // Zink only implements the OpenGL core profile. The legacy OSMesaCreateContext()
        // API always hands back a compatibility-profile context capped at whatever the
        // driver's default version is, which Zink cannot satisfy - it either fails to
        // create a context at all, or creates one that doesn't match the GL 4.6 core
        // context LWJGL/Minecraft subsequently expects, both of which surface later as
        // "There is no OpenGL context current in the current thread" once Minecraft
        // calls GL.createCapabilities(). Explicitly request a core-profile 4.6 context
        // so Zink can actually initialize.
        const int attribs[] = {
            OSMESA_PROFILE, OSMESA_CORE_PROFILE,
            OSMESA_CONTEXT_MAJOR_VERSION, 4,
            OSMESA_CONTEXT_MINOR_VERSION, 6,
            OSMESA_DEPTH_BITS, 24,
            OSMESA_STENCIL_BITS, 8,
            0
        };
        context = OSMesaCreateContextAttribs_p(attribs, osmesa_share);
        if (context == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                                "OSMesaCreateContextAttribs_p() failed to create a core-profile "
                                "GL 4.6 context for Vulkan Zink. This device/driver likely does "
                                "not support the Vulkan+Zink core-profile requirements; try a "
                                "different renderer.");
        }
    } else {
        context = OSMesaCreateContext_p(GL_RGBA, osmesa_share);
        if (context == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                                "OSMesaCreateContext_p() failed to create an OSMesa context "
                                "(renderer=%s)", pojavRenderer != NULL ? pojavRenderer : "unknown");
        }
    }

    if(context == NULL) {
        free(render_window);
        return NULL;
    }
    render_window->context = context;
    return render_window;
}

void osm_set_no_render_buffer(ANativeWindow_Buffer* buffer) {
    buffer->bits = &no_render_buffer;
    buffer->width = 1;
    buffer->height = 1;
    buffer->stride = 0;
}

void osm_swap_surfaces(osm_render_window_t* bundle) {
    if(bundle->nativeSurface != NULL && bundle->newNativeSurface != bundle->nativeSurface) {
        if(!bundle->disable_rendering) {
            __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Unlocking for cleanup...");
            ANativeWindow_unlockAndPost(bundle->nativeSurface);
        }
        ANativeWindow_release(bundle->nativeSurface);
    }
    if(bundle->newNativeSurface != NULL) {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag, "Switching to new native surface");
        bundle->nativeSurface = bundle->newNativeSurface;
        bundle->newNativeSurface = NULL;
        ANativeWindow_acquire(bundle->nativeSurface);
        ANativeWindow_setBuffersGeometry(bundle->nativeSurface, 0, 0, WINDOW_FORMAT_RGBX_8888);
        bundle->disable_rendering = false;
        return;
    }else {
        __android_log_print(ANDROID_LOG_ERROR, g_LogTag,
                            "No new native surface, switching to dummy framebuffer");
        bundle->nativeSurface = NULL;
        osm_set_no_render_buffer(&bundle->buffer);
        bundle->disable_rendering = true;
    }

}

void osm_release_window() {
    currentBundle->newNativeSurface = NULL;
    osm_swap_surfaces(currentBundle);
    // 清理过期状态，避免下次 swap 重复进入 osm_swap_surfaces 导致回退到空 framebuffer
    if (currentBundle->nativeSurface != NULL && currentBundle->state == STATE_RENDERER_NEW_WINDOW) {
        currentBundle->state = STATE_RENDERER_ALIVE;
    }
}

void osm_apply_current_ll() {
    ANativeWindow_Buffer* buffer = &currentBundle->buffer;
    OSMesaMakeCurrent_p(currentBundle->context, buffer->bits, GL_UNSIGNED_BYTE, buffer->width, buffer->height);
    if(buffer->stride != currentBundle->last_stride)
        OSMesaPixelStore_p(OSMESA_ROW_LENGTH, buffer->stride);
    currentBundle->last_stride = buffer->stride;
}

void osm_make_current(osm_render_window_t* bundle) {
    if(bundle == NULL) {
        //technically this does nothing as its not possible to unbind a context in OSMesa
        OSMesaMakeCurrent_p(NULL, NULL, 0, 0, 0);
        currentBundle = NULL;
        return;
    }
    bool hasSetMainWindow = false;
    currentBundle = bundle;
    if(pojav_environ->mainWindowBundle == NULL) {
        pojav_environ->mainWindowBundle = (basic_render_window_t*) bundle;
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Main window bundle is now %p", pojav_environ->mainWindowBundle);
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
        hasSetMainWindow = true;
    }
    if(bundle->nativeSurface == NULL) {
        //prepare the buffer for our first render!
        osm_swap_surfaces(bundle);
        if(hasSetMainWindow) pojav_environ->mainWindowBundle->state = STATE_RENDERER_ALIVE;
    }
    if (!hasSetNoRendererBuffer)
    {
        osm_set_no_render_buffer(&bundle->buffer);
        hasSetNoRendererBuffer = true;
    }
    osm_apply_current_ll();
    OSMesaPixelStore_p(OSMESA_Y_UP,0);
}

void osm_swap_buffers() {
    if(currentBundle->state == STATE_RENDERER_NEW_WINDOW) {
        osm_swap_surfaces(currentBundle);
        currentBundle->state = STATE_RENDERER_ALIVE;
    }

    if(currentBundle->nativeSurface != NULL && !currentBundle->disable_rendering)
        if(ANativeWindow_lock(currentBundle->nativeSurface, &currentBundle->buffer, NULL) != 0)
            osm_release_window();

    osm_apply_current_ll();
    glFinish_p(); // this will force osmesa to write the last rendered image into the buffer

    if(currentBundle->nativeSurface != NULL && !currentBundle->disable_rendering)
        if(ANativeWindow_unlockAndPost(currentBundle->nativeSurface) != 0)
            osm_release_window();
}

void osm_setup_window() {
    if(pojav_environ->mainWindowBundle != NULL) {
        __android_log_print(ANDROID_LOG_INFO, g_LogTag, "Main window bundle is not NULL, changing state");
        pojav_environ->mainWindowBundle->state = STATE_RENDERER_NEW_WINDOW;
        pojav_environ->mainWindowBundle->newNativeSurface = pojav_environ->pojavWindow;
    }
}

void osm_swap_interval(int swapInterval) {
    if(pojav_environ->mainWindowBundle != NULL && pojav_environ->mainWindowBundle->nativeSurface != NULL) {
        setNativeWindowSwapInterval(pojav_environ->mainWindowBundle->nativeSurface, swapInterval);
    }
}
