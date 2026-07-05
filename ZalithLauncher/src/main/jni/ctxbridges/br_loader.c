//
// Created by Vera-Firefly on 28.01.2025.
//
#include <string.h>
#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include "br_loader.h"
#include "egl_loader.h"
#include "osmesa_loader.h"

__eglMustCastToProperFunctionPointerType (*eglGetProcAddress_p) (const char *procname);
void* (*OSMesaGetProcAddress_p)(const char* funcName);

void* load_symbol(void* handle, const char* symbol_name) {
    void* symbol = dlsym(handle, symbol_name);
    if (!symbol)
        fprintf(stderr, "Error[Load Symbol]: Failed to load symbol '%s': %s\n", symbol_name, dlerror());

    return symbol;
}

// ---------------------------------------------------------------------------
// MobileGlues compatibility stubs
//
// MobileGlues is a GLES 3.x-based OpenGL translator. GLES does not provide
// the OpenGL 1.x fixed-function pipeline, so legacy fog functions such as
// glFogfv are not exported by libMobileGlues.so. When LWJGL3 initialises its
// GLCapabilities it resolves every expected function pointer; a null pointer
// for any of these causes a NullPointerException (Checks.java) the first time
// Minecraft calls the function (e.g. during fog rendering in 1.16.5).
//
// The stubs below are no-ops: fog is simply unsupported on GLES paths, so
// silently ignoring the calls is the correct behaviour (modern versions of
// Minecraft already skip fog on unsupported renderers).
// ---------------------------------------------------------------------------

static void mg_stub_glFogf(int pname, float param) {}
static void mg_stub_glFogi(int pname, int param) {}
static void mg_stub_glFogfv(int pname, const float *params) {}
static void mg_stub_glFogiv(int pname, const int *params) {}
static void mg_stub_glFogCoordf(float coord) {}
static void mg_stub_glFogCoordd(double coord) {}
static void mg_stub_glFogCoordfv(const float *coord) {}
static void mg_stub_glFogCoorddv(const double *coord) {}
static void mg_stub_glFogCoordPointer(int type, int stride, const void *pointer) {}

typedef struct {
    const char* name;
    void*       func;
} MgStubEntry;

static MgStubEntry s_mobileglues_stubs[] = {
    { "glFogf",             (void*)mg_stub_glFogf            },
    { "glFogi",             (void*)mg_stub_glFogi            },
    { "glFogfv",            (void*)mg_stub_glFogfv           },
    { "glFogiv",            (void*)mg_stub_glFogiv           },
    { "glFogCoordf",        (void*)mg_stub_glFogCoordf       },
    { "glFogCoordd",        (void*)mg_stub_glFogCoordd       },
    { "glFogCoordfv",       (void*)mg_stub_glFogCoordfv      },
    { "glFogCoorddv",       (void*)mg_stub_glFogCoorddv      },
    { "glFogCoordPointer",  (void*)mg_stub_glFogCoordPointer },
    { NULL, NULL }
};

static void* mobileglues_find_stub(const char* name) {
    for (int i = 0; s_mobileglues_stubs[i].name != NULL; i++) {
        if (strcmp(s_mobileglues_stubs[i].name, name) == 0) {
            return s_mobileglues_stubs[i].func;
        }
    }
    return NULL;
}

// ---------------------------------------------------------------------------

void* OSMGetProcAddress(void* handle, const char* symbol_name) {
    OSMesaGetProcAddress_p = load_symbol(handle, "OSMesaGetProcAddress");
    if (OSMesaGetProcAddress_p)
    {
        void* symbol = OSMesaGetProcAddress_p(symbol_name);
        if (symbol)
        {
            return symbol;
        }
        fprintf(stderr, "Error[OSM Loader]: 'OSMesaGetProcAddress' could not find symbol '%s'.\n", symbol_name);
    }
    return load_symbol(handle, symbol_name);
}

void* GLGetProcAddress(void* handle, const char* symbol_name) {
    eglGetProcAddress_p = load_symbol(handle, "eglGetProcAddress");
    if(eglGetProcAddress_p)
    {
        void* symbol = (void*) eglGetProcAddress_p(symbol_name);
        if (symbol)
        {
            return symbol;
        }
        fprintf(stderr, "Error[GL Loader]: 'eglGetProcAddress' could not find symbol '%s'.\n", symbol_name);
    }

    void* symbol = dlsym(handle, symbol_name);
    if (symbol) return symbol;

    // For MobileGlues: return no-op stubs for fixed-function GL 1.x fog symbols
    // that libMobileGlues.so does not export. This prevents LWJGL3's Checks.check()
    // from throwing a NullPointerException when Minecraft calls these functions.
    const char* renderer = getenv("POJAV_RENDERER");
    if (renderer != NULL && strcmp(renderer, "mobileglues") == 0) {
        void* stub = mobileglues_find_stub(symbol_name);
        if (stub != NULL) {
            fprintf(stderr, "MobileGlues: providing no-op stub for missing symbol '%s'\n", symbol_name);
            return stub;
        }
    }

    fprintf(stderr, "Error[GL Loader]: Failed to load symbol '%s': %s\n", symbol_name, dlerror());
    return NULL;
}
