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

./gradlew $gradleTarget ${@:2}

echo "Signing...."

if [[ ! -d outputs ]]; then
	mkdir outputs
fi


declare -a abis=("arm64-v8a" "armeabi-v7a")

for abi in ${abis[@]}; do
	unaligned="./app/build/outputs/apk/$path/app-$abi-$target.apk"
	aligned="./app/build/outputs/apk/$path/app-$abi-$target-aligned.apk"
	if [ -f $aligned ]; then rm $aligned; fi
	zipalign -v -p 4 $unaligned $aligned 1>/dev/null
	apksigner -J-enable-native-access=ALL-UNNAMED sign --in $aligned --out outputs/"photos_signed_${tag}_${abi}.apk" --key keys/releasekey.pk8 --cert keys/releasekey.x509.pem
done

echo "Signed!"
