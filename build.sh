#!/bin/bash

set -e

gradleTarget=assembleDebug
target="apk/debug"
file=app-debug
tag=debug

if [ "$1" == "release" ];then
    gradleTarget=assembleRelease
    target="apk/release"
    file=app-release-unsigned
    tag=release
elif [ "$1" == "universal" ];then
    gradleTarget=packageReleaseUniversalApk
    target="apk_from_bundle/release"
    file=app-release-universal-unsigned
    tag=universal
fi
JAVA_HOME=/opt/android-studio/jbr/ ./gradlew $gradleTarget ${@:2}

echo "Signing...."
./apksigner/apksigner -J-enable-native-access=ALL-UNNAMED sign --in ./app/build/outputs/$target/${file}.apk --out photos_signed_$tag.apk --key keys/releasekey.pk8 --cert keys/releasekey.x509.pem
echo "Signed!"
