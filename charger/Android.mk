# Copyright 2011 The Android Open Source Project

ifneq ($(BUILD_TINY_ANDROID),true)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	charger.c


ifeq ($(strip $(BOARD_CHARGER_ENABLE_SUSPEND)),true)
LOCAL_CFLAGS += -DCHARGER_ENABLE_SUSPEND
endif

LOCAL_MODULE := note_charger
LOCAL_MODULE_TAGS := optional
LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)
LOCAL_UNSTRIPPED_PATH := $(TARGET_ROOT_OUT_UNSTRIPPED)

LOCAL_C_INCLUDES := bootable/recovery

LOCAL_STATIC_LIBRARIES := libminui libpixelflinger_static libpng
ifeq ($(strip $(BOARD_CHARGER_ENABLE_SUSPEND)),true)
LOCAL_STATIC_LIBRARIES += libsuspend
endif
LOCAL_STATIC_LIBRARIES += libz libstdc++ libcutils liblog libm libc

ifneq ($(BOARD_BATTERY_SYSFS_PATH),)
LOCAL_CFLAGS += -DBATTERY_SYSFS=\"$(BOARD_BATTERY_SYSFS_PATH)\"
endif

ifneq ($(BOARD_AC_SYSFS_PATH),)
LOCAL_CFLAGS += -DAC_SYSFS=\"$(BOARD_AC_SYSFS_PATH)\"
endif

ifneq ($(BOARD_USB_SYSFS_PATH),)
LOCAL_CFLAGS += -DUSB_SYSFS=\"$(BOARD_USB_SYSFS_PATH)\"
endif

ifeq ($(BOARD_CHARGER_DIM_SCREEN_BRIGHTNESS),true)
LOCAL_CFLAGS += -DDIM_SCREEN=\"$(BOARD_CHARGER_DIM_SCREEN_BRIGHTNESS)\"
endif

ifneq ($(TW_BRIGHTNESS_PATH),)
LOCAL_CFLAGS += -DBRIGHTNESS_PATH=\"$(TW_BRIGHTNESS_PATH)\"
ifeq ($(TW_MAX_BRIGHTNESS),)
LOCAL_CFLAGS += -DMAX_BRIGHTNESS=\"255\"
else
LOCAL_CFLAGS += -DMAX_BRIGHTNESS=\"$(TW_MAX_BRIGHTNESS)\"
endif
endif



include $(BUILD_EXECUTABLE)

define _add-charger-image
include $$(CLEAR_VARS)
LOCAL_MODULE := note_system_core_charger_$(notdir $(1))
LOCAL_MODULE_STEM := $(notdir $(1))
_img_modules += $$(LOCAL_MODULE)
LOCAL_SRC_FILES := $1
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $$(TARGET_ROOT_OUT)/res/images/charger
include $$(BUILD_PREBUILT)
endef

_img_modules :=
_images :=
ifneq ($(BOARD_CHARGER_RES),)
$(foreach _img, $(call find-subdir-subdir-files, ../../../$(BOARD_CHARGER_RES), "*.png"), \
  $(eval $(call _add-charger-image,$(_img))))
else
$(foreach _img, $(call find-subdir-subdir-files, "images", "*.png"), \
  $(eval $(call _add-charger-image,$(_img))))
endif

include $(CLEAR_VARS)
LOCAL_MODULE := note_charger_res_images
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := $(_img_modules)
include $(BUILD_PHONY_PACKAGE)

_add-charger-image :=
_img_modules :=

endif
