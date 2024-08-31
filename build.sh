#!/bin/bash

set -e

gradleTarget=assembleDebug
target=debug
file=app-debug
if [ "$1" == "release" ];then
    gradleTarget=assembleRelease
    target=release
    file=app-release-unsigned
fi
JAVA_HOME=/opt/android-studio/jbr/ ./gradlew $gradleTarget ${@:2}

echo "Signing...."
LD_LIBRARY_PATH=./signapk/ java -jar signapk/signapk.jar keys/releasekey.x509.pem keys/releasekey.pk8 ./app/build/outputs/apk/$target/${file}.apk photos_signed_$target.apk
echo "Signed!"
