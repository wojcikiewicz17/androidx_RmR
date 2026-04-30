# Performance Evidence Matrix (AndroidX_RmR)

| Feature | File | Código real | Teste | Benchmark | Status |
|---|---|---|---|---|---|
| SIMD level detection via native JNI | `rmr/rmr-core/src/main/java/androidx/rmr/core/RmRHardware.java` + `rmr/rmr-core/src/main/cpp/rmr_matrix_native.cpp` | `RmRHardware.nativeGetSimdLevel()` + compile-time macro mapping | `rmr/rmr-core/src/androidTest/java/androidx/rmr/core/RmRHardwareTest.java` | `RmRMatrixOpsBenchmarkTest` logs ABI/SIMD | implemented |
| ARM NEON compile-time support (AArch64 + ARMv7) | `rmr/rmr-core/src/main/cpp/rmr_matrix_native.cpp` + `CMakeLists.txt` | `#if defined(__aarch64__) || defined(__ARM_NEON) || defined(__ARM_NEON__)` | Indirect by JNI load/tests | Requires device ABI run | implemented |
| Native matrix multiply (JNI) | `rmr/rmr-core/src/main/java/androidx/rmr/core/RmRMatrixOps.java` + `rmr_matrix_native.cpp` | `multiplyNative(...)` | `RmRMatrixOpsTest` | `RmRMatrixOpsBenchmarkTest` | implemented |
| Cache blocking in scalar multiply | `rmr/rmr-core/src/main/java/androidx/rmr/core/RmRMatrixOps.java` | `multiplyBlocked(...)` with block size threshold | `RmRMatrixOpsTest` | `RmRMatrixOpsBenchmarkTest` | implemented |
| Buffer reuse / no new alloc hot-path variant | `RmRMatrixOps.multiplyInto`, `transposeInto`, `applyInto` | `Into` APIs writing to caller-provided output | `RmRMatrixOpsTest` + benchmark smoke | `RmRMatrixOpsBenchmarkTest` (`multiplyInto`, `transposeInto`) | implemented |
| Thread-local / matrix pool | `rmr/rmr-core/src/main/java/androidx/rmr/core/DoubleArrayPool.java` | Reusable buffers for matrix allocation | `RmRMatrixTest` coverage for lifecycle flows | Not yet isolated | implemented |
| HDR support in Camera API | `camera/camera-core/src/main/java/androidx/camera/core/DynamicRange.java` | API-level dynamic range definitions only | Existing AndroidX tests (outside RmR) | N/A | documented |
| Claim: "HDR em qualquer app" | README/docs | No universal runtime proof in this repo | N/A | N/A | unproven |
| GC/allocation/latency metrics with p50/p95 | `rmr/rmr-core/src/androidTest/java/androidx/rmr/core/RmRMatrixOpsBenchmarkTest.java` | Captures avg/p50/p95 + avg allocation delta | Android instrumentation test | Yes (reproducible harness) | implemented |
| Published comparative gains (3x/5x/50x) | docs | No reproducible numbers committed yet | N/A | Pending hardware run exports | unproven |
