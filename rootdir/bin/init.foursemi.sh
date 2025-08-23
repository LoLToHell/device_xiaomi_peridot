#!/system/bin/sh

[ $(</sys/kernel/smartpa/smartpa_type) = fs19xx ] && insmod /vendor/lib/modules/fs19xx_dlkm.ko
