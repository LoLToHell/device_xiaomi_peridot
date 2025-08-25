QTI_VIBRATOR_HAL_SERVICE := vendor.qti.hardware.vibrator.service.xiaomi.peridot
PRODUCT_PACKAGES += $(QTI_VIBRATOR_HAL_SERVICE)

PRODUCT_COPY_FILES += \
    vibrator/excluded-input-devices.xml:$(TARGET_COPY_OUT_VENDOR)/etc/excluded-input-devices.xml
