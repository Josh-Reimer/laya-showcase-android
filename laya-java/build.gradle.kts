// Vendored from https://github.com/NandhaKishorM/laya (laya-java module), Apache-2.0.
// Not published to Maven Central upstream, so it is checked in here rather than resolved as a
// dependency. Compiles against the desktop ONNX Runtime jar for its API surface only; the actual
// runtime classes + native libs come from `onnxruntime-android` on the Android app's classpath at
// run time (same `ai.onnxruntime` package, so the two are interchangeable at the JVM level).
plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("com.microsoft.onnxruntime:onnxruntime:1.20.0")
}
