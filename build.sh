#!/bin/bash

set -e

gradleTarget=assembleDebug
target="debug"
path="debug"
tag=debug

if [ "$1" == "release" ];then
    gradleTarget=assembleRelease
    target="release-unsigned"
    path="release"
    tag=release
fi

JAVA_HOME=/opt/android-studio/jbr/ ./gradlew $gradleTarget ${@:2}

echo "Signing...."

if [[ -d outputs ]]; then
	rm -r outputs
fi

mkdir outputs

declare -a abis=("arm64-v8a" "armeabi-v7a")

for abi in ${abis[@]}; do
	./apksigner/apksigner -J-enable-native-access=ALL-UNNAMED sign --in ./app/build/outputs/apk/$path/app-$abi-$target.apk --out outputs/"photos_signed_${tag}_${abi}.apk" --key keys/releasekey.pk8 --cert keys/releasekey.x509.pem
done

echo "Signed!"
