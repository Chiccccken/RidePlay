LOCAL_PATH := $(call my-dir)

# Keep __FILE__ diagnostics and debug paths independent of the build machine.
RIDEPLAY_NATIVE_PATH_FLAGS := -ffile-prefix-map=$(LOCAL_PATH)=jni

include $(CLEAR_VARS)
LOCAL_MODULE := xcertplay_i2c
LOCAL_SRC_FILES := linux_i2c_jni.c
LOCAL_CFLAGS := $(RIDEPLAY_NATIVE_PATH_FLAGS)
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := local_hotspot_radio
LOCAL_SRC_FILES := local_hotspot_radio.c
LOCAL_CFLAGS := -Wall -Wextra -Werror $(RIDEPLAY_NATIVE_PATH_FLAGS)
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := speex_echo
LOCAL_SRC_FILES := speex_echo_jni.c \
    speexdsp/mdf.c speexdsp/preprocess.c speexdsp/fftwrap.c \
    speexdsp/kiss_fft.c speexdsp/kiss_fftr.c speexdsp/filterbank.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/speexdsp $(LOCAL_PATH)/speexdsp/include
LOCAL_CFLAGS := -O2 -DFLOATING_POINT -DUSE_KISS_FFT -DEXPORT= -Wno-unused-parameter $(RIDEPLAY_NATIVE_PATH_FLAGS)
LOCAL_LDLIBS := -lm
include $(BUILD_SHARED_LIBRARY)
