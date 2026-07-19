#ifndef FPS_LIMIT_H
#define FPS_LIMIT_H

#include <stdbool.h>

void fpslimit_set_limit(int fps);
int fpslimit_get_limit(void);
void fpslimit_throttle(void);

#endif
