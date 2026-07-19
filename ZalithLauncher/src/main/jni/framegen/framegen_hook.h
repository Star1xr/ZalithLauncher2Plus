#ifndef FRAMEGEN_HOOK_H
#define FRAMEGEN_HOOK_H

#include <stdbool.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>

void framegen_set_enabled(bool enabled);
bool framegen_is_enabled(void);

void framegen_capture(EGLDisplay dpy, EGLSurface surface);
void framegen_double(EGLDisplay dpy, EGLSurface surface);

#endif
