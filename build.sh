#!/bin/bash

set -e



function build() {
	gradleTarget=assembleDebug
	target="debug"
	path="debug"
	tag=debug

	if [ $1 == "release" ];then
	    gradleTarget=assembleRelease
	    target="release-unsigned"
	    path="release"
	    tag=release
	fi

	./gradlew $gradleTarget

	echo
	echo "Signing...."

	if [[ ! -d outputs ]]; then
		mkdir outputs
	fi

	declare -a abis=("arm64-v8a" "armeabi-v7a")

	for abi in ${abis[@]}; do
		unaligned="./app/build/outputs/apk/$path/app-$abi-$target.apk"
		aligned="./app/build/outputs/apk/$path/app-$abi-$target-aligned.apk"
		if [ -f $aligned ]; then rm $aligned; fi
		zipalign -v -f -P 16 4 $unaligned $aligned 1>/dev/null
		apksigner -J-enable-native-access=ALL-UNNAMED sign --in $aligned --out outputs/"photos_signed_${tag}_${abi}.apk" --key keys/releasekey.pk8 --cert keys/releasekey.x509.pem
	done

	echo "Signed!"

	if [ "$2" == "--install" ]; then
		echo
		echo "Installing..."
		abi="arm64-v8a"
		if [ "$3" != "" ]; then
			abi="$3"
		fi

		adb install outputs/"photos_signed_${tag}_${abi}.apk"
	fi
}

function show_help() {
	echo "build.sh [command or option]"
	echo
	echo "Commands:"
	echo "	debug: build the debug version of this app"
	echo "	release: build the release version of this app"
	echo
	echo "Options:"
	echo "	-h | --help: Show this help menu"
	echo "	--install [abi]: install the built application"
	echo "		abi is one of (arm64-v8a, armeabi-v7a). it is arm64-v8a by default"
}

case $1 in
	"debug")
		build "debug" $2
		;;
	"release")
		build "release" $2
		;;
	"optimize")
		adb shell cmd package compile -m speed-profile com.kaii.photos
		;;
	"--help" | "-h" | "")
		show_help
		;;
	*)
		echo "Not a valid command. Use --help to list commands"
		;;
esac
