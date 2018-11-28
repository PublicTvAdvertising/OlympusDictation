APP_PLATFORM := android-21
APP_STL := gnustl_static
APP_CPPFLAGS += -fexceptions
NDK_TOOLCHAIN_VERSION=4.8
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog