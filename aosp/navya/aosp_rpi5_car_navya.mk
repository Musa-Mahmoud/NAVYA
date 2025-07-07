PRODUCT_PACKAGES += \
	libgpiod \
	libgpiohalrpi5 \
	tinycap \
	libswitch3hal \
	libads1115 \
	neopixelhal \
	MutliDisplayRpiOverlay \
	AndroidRpiFrameWorkMultiDisplay
PRODUCT_COPY_FILES += \
	vendor/navya/libs/init.navyahw.rc:root/init.navyahw.rc \
	vendor/navya/input_screen/input-port-associations.xml:$(TARGET_COPY_OUT_VENDOR)/etc/input-port-associations.xml
