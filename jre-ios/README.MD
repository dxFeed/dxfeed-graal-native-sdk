# How to build JDK lib on macOS?

## 1. Install JDK

```shell
# We use https://www.jenv.be/ to switch between JDKs.
brew install jenv
jenv enable-plugin maven
cd ~
# If you are running macOS on something other than an M1 processor, use JDK for amd64:
# https://github.com/graalvm/labs-openjdk-11/releases/download/jvmci-22.3-b22/labsjdk-ce-11.0.20+8-jvmci-22.3-b22-darwin-amd64.tar.gz
# in the following commands replace "aarch64" with "amd64"
curl -LO https://github.com/graalvm/labs-openjdk-11/releases/download/jvmci-22.3-b22/labsjdk-ce-11.0.20+8-jvmci-22.3-b22-darwin-aarch64.tar.gz
sudo xattr -rd com.apple.quarantine labsjdk-ce-11.0.20+8-jvmci-22.3-b22-darwin-aarch64.tar.gz
tar -xzvf labsjdk-ce-11.0.20+8-jvmci-22.3-b22-darwin-aarch64.tar.gz
mv labsjdk-ce-11.0.20-jvmci-22.3-b22 labsjdk-ce-11.0.20-jvmci-22.3-b22_aarch64
sudo mv labsjdk-ce-11.0.20-jvmci-22.3-b22_aarch64 /Library/Java/JavaVirtualMachines/
ln -s /Library/Java/JavaVirtualMachines/labsjdk-ce-11.0.20-jvmci-22.3-b22_aarch64/Contents/Home ~/.jenv/versions/labsjdk-ce-11.0.20-jvmci-22.3-b22_aarch64
jenv shell labsjdk-ce-11.0.20-jvmci-22.3-b22_aarch64
```

## 2. Build iOS JDK lib

```shell
git clone https://github.com/graalvm/labs-openjdk-11.git
cd labs-openjdk-11
git checkout jvmci-22.3-b22 -b latest
# use the patches in this folder
git apply iOS_build.patch
git apply "fix_build_basing_on_(https___stackoverflow_com_questions_72336864_how-to-compile-openjdk-1.patch"
git apply fix_problems_with_undefined_symbol_Java_com_sun_management_internal_OperatingSystemImpl_in.patch
# If you are running macOS on something other than an M1 processor, run the following command without "arch -x86_64 ".
arch -x86_64 python3 build_labsjdk.py --jvmci-version 22.3-b22 --configure-option=--disable-warnings-as-errors
#--- Build Succeeded ---
xcodebuild -sdk iphoneos -arch arm64 -project jdk-11-ios.xcodeproj -scheme jdk-arm64-release
#** BUILD SUCCEEDED **
lipo -info xcode/jdk-arm64-ios-r.a
#Non-fat file: xcode/jdk-arm64-ios-r.a is architecture: arm64
xcodebuild -sdk iphonesimulator -project jdk-11-ios.xcodeproj -scheme jdk-arm64-release
#** BUILD SUCCEEDED **
lipo -info xcode/jdk-arm64-ios-simulator-r.a
#Architectures in the fat file: xcode/jdk-arm64-ios-simulator-r.a are: arm64 x86_64
```

## 3. Install JDK form previous step

```shell
ln -s ~/labs-openjdk-11/java_home ~/.jenv/versions/labs-openjdk-11_graal
jenv shell labs-openjdk-11_graal
```

## 4. Build iOS graal svm lib

```shell
cd ~
git clone https://github.com/graalvm/mx & export PATH=$PATH:~/mx
git clone git@github.com:oracle/graal.git
cd graal
git checkout graal-22.3.3 -b latest
git apply "Add_XCode_project_to_build_JDK_for_iOS_and_iOS-simulator.patch"
cd substratevm
# If you are running macOS on something other than an M1 processor, run the following command without "arch -x86_64 ".
arch -x86_64 mx build
xcodebuild -sdk iphoneos -arch arm64 -project graal-svm-ios.xcodeproj -scheme graal-svm-arm64-release
lipo -info xcode/graal-svm-arm64-ios-r.a
#Non-fat file: xcode/graal-svm-arm64-ios-r.a is architecture: arm64
xcodebuild -sdk iphonesimulator -project graal-svm-ios.xcodeproj -scheme graal-svm-arm64-release
lipo -info xcode/graal-svm-arm64-ios-simulator-r.a
#Non-fat file: xcode/graal-svm-arm64-ios-simulator-r.a is architecture: arm64
# If you are running macOS on something other than an M1 processor, run the following command without "arch -x86_64 ".
arch -x86_64 xcodebuild -sdk iphonesimulator -arch x86_64 -project graal-svm-ios.xcodeproj -scheme graal-svm-x86-64-release
lipo -info xcode/graal-svm-x86-64-ios-simulator-r.a
#Non-fat file: xcode/graal-svm-x86-64-ios-simulator-r.a is architecture: x86_64
lipo -create xcode/graal-svm-arm64-ios-simulator-r.a xcode/graal-svm-x86-64-ios-simulator-r.a -output xcode/graal-svm-ios-simulator-r.a
lipo -info xcode/graal-svm-ios-simulator-r.a
#Architectures in the fat file: xcode/graal-svm-ios-simulator-r.a are: x86_64 arm64
```
