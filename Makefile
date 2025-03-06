ANDROID_HOME := $(HOME)/Android/Sdk
ANDROID_NDK := $(ANDROID_HOME)/ndk/25.2.9519653
PLATFORM := $(ANDROID_HOME)/platforms/android-34
BUILD_TOOLS := $(ANDROID_HOME)/build-tools/34.0.0
GO := go
GRADLE := ./gradlew

export ANDROID_HOME

all: build-go build-apk

build-go:
	cd app/src/main/go && gomobile bind -o ../../../libs/smsmatrix.aar -target=android -androidapi 23 .

build-apk:
	$(GRADLE) assembleDebug

build: build-go build-apk

install: build
	adb install app/build/outputs/apk/debug/app-debug.apk

clean:
	$(GRADLE) clean
