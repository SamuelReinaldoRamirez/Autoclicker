@echo off
"C:\\Users\\samue\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HC:\\Users\\samue\\AndroidStudioProjects\\AutoclickerFusionBuild\\opencv\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=33" ^
  "-DANDROID_PLATFORM=android-33" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\Users\\samue\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\samue\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\samue\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\samue\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=C:\\Users\\samue\\AndroidStudioProjects\\AutoclickerFusionBuild\\opencv\\build\\intermediates\\cxx\\Debug\\5o461l3j\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=C:\\Users\\samue\\AndroidStudioProjects\\AutoclickerFusionBuild\\opencv\\build\\intermediates\\cxx\\Debug\\5o461l3j\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BC:\\Users\\samue\\AndroidStudioProjects\\AutoclickerFusionBuild\\opencv\\.cxx\\Debug\\5o461l3j\\x86" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
