# How to build on macOS M1 for iOS or simulator

## 1. Prepare

```shell
# We use https://www.jenv.be/ to switch between JDKs.
brew install jenv
jenv enable-plugin maven
# xcode command line tools
xcode-select --install
# Open JDK 22
cd ~
curl -LO https://github.com/graalvm/labs-openjdk-22/releases/download/22.0.1%2B8-jvmci-b01/labsjdk-ce-22.0.1+8-jvmci-b01-darwin-aarch64.tar.gz
sudo xattr -rd com.apple.quarantine labsjdk-ce-22.0.1+8-jvmci-b01-darwin-aarch64.tar.gz
tar -xzvf labsjdk-ce-22.0.1+8-jvmci-b01-darwin-aarch64.tar.gz
mv labsjdk-ce-22.0.1-jvmci-b01 labsjdk-ce-22.0.1-jvmci-b01_aarch64
sudo mv labsjdk-ce-22.0.1-jvmci-b01_aarch64 /Library/Java/JavaVirtualMachines/
ln -s /Library/Java/JavaVirtualMachines/labsjdk-ce-22.0.1-jvmci-b01_aarch64/Contents/Home ~/.jenv/versions/labsjdk-ce-22.0.1-jvmci-b01_aarch64
```

## 2. Build iOS JDK lib

```shell
jenv shell labsjdk-ce-22.0.1-jvmci-b01_aarch64
git clone https://github.com/graalvm/labs-openjdk-22.git
cd labs-openjdk-22
git checkout tags/22.0.1+8-jvmci-b01 -b latest
git apply patch_and_xcode_projects_for_22_0_1+8-jvmci-b01.patch
#make clean
bash configure
make images graal-builder-image
./build/macosx-aarch64-server-release/images/graal-builder-jdk/bin/java -version
#--- Build Succeeded ---
xcodebuild -sdk iphoneos -arch arm64 -project jdk-11-ios.xcodeproj -scheme jdk-arm64-release
lipo -info xcode/jdk-arm64-ios-r.a
#Non-fat file: xcode/jdk-arm64-ios-r.a is architecture: arm64
xcodebuild -sdk iphonesimulator -project jdk-11-ios.xcodeproj -scheme jdk-arm64-release
lipo -info xcode/jdk-arm64-ios-simulator-r.a
#Architectures in the fat file: xcode/jdk-arm64-ios-simulator-r.a are: arm64 x86_64
mv xcode/jdk-arm64-ios-simulator-r.a xcode/jdk-ios-simulator-r.a
```
Copy the jdk-arm64-ios-simulator-r.a and jdk-ios-simulator-r.a libraries to the /jre-ios folder.

## 3. Install JDK form previous step

```shell
ln -s ~/labs-openjdk-22/build/macosx-aarch64-server-release/images/graal-builder-jdk ~/.jenv/versions/graal-builder-jdk-22
jenv shell graal-builder-jdk-22
```

## 4. Build iOS graal svm lib

```shell
cd ~
git clone https://github.com/graalvm/mx & export PATH=$PATH:~/mx
git clone git@github.com:oracle/graal.git
cd graal
git checkout tags/jdk-22.0.1 -b latest
cd substratevm
mx build
cd ..
git apply "xcode_project_for_GraalVM.patch"
cd substratevm
xcodebuild -sdk iphoneos -project graal-svm-ios.xcodeproj -scheme graal-svm-arm64-release
lipo -info xcode/graal-svm-arm64-ios-r.a
#Non-fat file: xcode/graal-svm-arm64-ios-r.a is architecture: arm64
xcodebuild -sdk iphonesimulator -project graal-svm-ios.xcodeproj -scheme graal-svm-x86-64-release
lipo -info xcode/graal-svm-x86-64-ios-simulator-r.a
#Architectures in the fat file: xcode/graal-svm-x86-64-ios-simulator-r.a are: arm64 x86_64
mv xcode/graal-svm-x86-64-ios-simulator-r.a xcode/graal-svm-ios-simulator-r.a
```

Copy the graal-svm-arm64-ios-r.a and graal-svm-ios-simulator-r.a libraries to the /jre-ios folder.

## 4. Build project macOS (arm64 & x86_64), iOS (arm64) and simulator (both arm64 & x86_64)

```shell
cd ~
# maven
curl -LO https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz
sudo xattr -rd com.apple.quarantine apache-maven-3.8.8-bin.tar.gz
tar -xzvf apache-maven-3.8.8-bin.tar.gz
# add bin to PATH
#ARCH64
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-22.0.1/graalvm-community-jdk-22.0.1_macos-aarch64_bin.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-community-jdk-22.0.1_macos-aarch64_bin.tar.gz
tar -xzvf graalvm-community-jdk-22.0.1_macos-aarch64_bin.tar.gz
sudo mv graalvm-community-openjdk-22.0.1+8.1 /Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22.0.1+8.1
ln -s /Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22.0.1+8.1/Contents/Home ~/.jenv/versions/graalvm-community-openjdk-22.0.1+8.1
jenv global graalvm-community-openjdk-22.0.1+8.1
#AMD64
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-22.0.1/graalvm-community-jdk-22.0.1_macos-x64_bin.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-community-jdk-22.0.1_macos-x64_bin.tar.gz
tar -xzvf graalvm-community-jdk-22.0.1_macos-x64_bin.tar.gz
sudo mv graalvm-community-openjdk-22.0.1+8.1 /Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22.0.1+8.1-amd64
ln -s /Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22.0.1+8.1-amd64/Contents/Home ~/.jenv/versions/graalvm-community-openjdk-22.0.1+8.1-amd64
jenv shell graalvm-community-openjdk-22.0.1+8.1
# macOS
mvn clean package
# iOS
arch -arm64 mvn -DmacIos=true clean package
jenv shell graalvm-community-openjdk-22.0.1+8.1-amd64
# Simulator (without clean because it also use iOS artifacts from previous step)
arch -x86_64 mvn -DmacIosSimulator=true package
# amd64
arch -x86_64 mvn clean package
```