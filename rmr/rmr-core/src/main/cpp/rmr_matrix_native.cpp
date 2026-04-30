/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <cstdint>
#include <cstring>

// Include platform-specific SIMD headers
#if defined(__aarch64__) || defined(__ARM_NEON) || defined(__ARM_NEON__)
#include <arm_neon.h>
#define HAS_NEON 1
#endif

#ifdef __x86_64__
#include <emmintrin.h>  // SSE2
#include <immintrin.h>  // AVX
#define HAS_SSE2 1
#ifdef __AVX__
#define HAS_AVX 1
#endif
#ifdef __AVX2__
#define HAS_AVX2 1
#endif
#endif

extern "C" {

/**
 * Cache-blocking parameters for optimal L1 cache utilization.
 * Typical L1 cache: 32KB-64KB
 */
static constexpr int BLOCK_SIZE = 64;
static constexpr int CACHE_LINE_SIZE = 64;

/**
 * Aligns size to cache line boundary.
 */
static inline int alignToCacheLine(int size) {
    return ((size + CACHE_LINE_SIZE - 1) / CACHE_LINE_SIZE) * CACHE_LINE_SIZE;
}

/**
 * NEON-optimized matrix multiplication for ARM/ARM64.
 * Uses 128-bit SIMD registers to process 2 doubles at a time.
 */
#ifdef HAS_NEON
static void multiplyNEON(const double* __restrict__ aData, int aRows, int aCols,
                        const double* __restrict__ bData, int bRows, int bCols,
                        double* __restrict__ resultData) {
    int m = aRows;
    int k = aCols;
    int n = bCols;
    
    // Clear result
    memset(resultData, 0, m * n * sizeof(double));
    
    // Blocked multiplication with NEON
    for (int ii = 0; ii < m; ii += BLOCK_SIZE) {
        int iEnd = (ii + BLOCK_SIZE < m) ? ii + BLOCK_SIZE : m;
        
        for (int kk = 0; kk < k; kk += BLOCK_SIZE) {
            int kEnd = (kk + BLOCK_SIZE < k) ? kk + BLOCK_SIZE : k;
            
            for (int jj = 0; jj < n; jj += BLOCK_SIZE) {
                int jEnd = (jj + BLOCK_SIZE < n) ? jj + BLOCK_SIZE : n;
                
                // Process block
                for (int i = ii; i < iEnd; i++) {
                    for (int kIdx = kk; kIdx < kEnd; kIdx++) {
                        // Broadcast a[i][k] to all elements of a NEON vector
                        float64x2_t aik = vdupq_n_f64(aData[i * aCols + kIdx]);
                        
                        int j = jj;
                        int bOffset = kIdx * n;
                        int resultOffset = i * n;
                        
                        // Process 2 doubles at a time with NEON
                        for (; j + 2 <= jEnd; j += 2) {
                            float64x2_t b_vec = vld1q_f64(&bData[bOffset + j]);
                            float64x2_t result_vec = vld1q_f64(&resultData[resultOffset + j]);
                            
                            // Multiply and accumulate: result += aik * b
                            result_vec = vfmaq_f64(result_vec, aik, b_vec);
                            
                            vst1q_f64(&resultData[resultOffset + j], result_vec);
                        }
                        
                        // Handle remainder
                        for (; j < jEnd; j++) {
                            resultData[resultOffset + j] += aData[i * aCols + kIdx] * bData[bOffset + j];
                        }
                    }
                }
            }
        }
    }
}
#endif

/**
 * SSE2-optimized matrix multiplication for x86/x86_64.
 * Uses 128-bit SIMD registers to process 2 doubles at a time.
 */
#ifdef HAS_SSE2
static void multiplySSE2(const double* __restrict__ aData, int aRows, int aCols,
                        const double* __restrict__ bData, int bRows, int bCols,
                        double* __restrict__ resultData) {
    int m = aRows;
    int k = aCols;
    int n = bCols;
    
    // Clear result
    memset(resultData, 0, m * n * sizeof(double));
    
    // Blocked multiplication with SSE2
    for (int ii = 0; ii < m; ii += BLOCK_SIZE) {
        int iEnd = (ii + BLOCK_SIZE < m) ? ii + BLOCK_SIZE : m;
        
        for (int kk = 0; kk < k; kk += BLOCK_SIZE) {
            int kEnd = (kk + BLOCK_SIZE < k) ? kk + BLOCK_SIZE : k;
            
            for (int jj = 0; jj < n; jj += BLOCK_SIZE) {
                int jEnd = (jj + BLOCK_SIZE < n) ? jj + BLOCK_SIZE : n;
                
                // Process block
                for (int i = ii; i < iEnd; i++) {
                    for (int kIdx = kk; kIdx < kEnd; kIdx++) {
                        // Broadcast a[i][k] to all elements of an SSE vector
                        __m128d aik = _mm_set1_pd(aData[i * aCols + kIdx]);
                        
                        int j = jj;
                        int bOffset = kIdx * n;
                        int resultOffset = i * n;
                        
                        // Process 2 doubles at a time with SSE2
                        for (; j + 2 <= jEnd; j += 2) {
                            __m128d b_vec = _mm_loadu_pd(&bData[bOffset + j]);
                            __m128d result_vec = _mm_loadu_pd(&resultData[resultOffset + j]);
                            
                            // Multiply and accumulate: result += aik * b
                            result_vec = _mm_add_pd(result_vec, _mm_mul_pd(aik, b_vec));
                            
                            _mm_storeu_pd(&resultData[resultOffset + j], result_vec);
                        }
                        
                        // Handle remainder
                        for (; j < jEnd; j++) {
                            resultData[resultOffset + j] += aData[i * aCols + kIdx] * bData[bOffset + j];
                        }
                    }
                }
            }
        }
    }
}
#endif

/**
 * AVX-optimized matrix multiplication for x86_64.
 * Uses 256-bit SIMD registers to process 4 doubles at a time.
 */
#ifdef HAS_AVX
static void multiplyAVX(const double* __restrict__ aData, int aRows, int aCols,
                       const double* __restrict__ bData, int bRows, int bCols,
                       double* __restrict__ resultData) {
    int m = aRows;
    int k = aCols;
    int n = bCols;
    
    // Clear result
    memset(resultData, 0, m * n * sizeof(double));
    
    // Blocked multiplication with AVX
    for (int ii = 0; ii < m; ii += BLOCK_SIZE) {
        int iEnd = (ii + BLOCK_SIZE < m) ? ii + BLOCK_SIZE : m;
        
        for (int kk = 0; kk < k; kk += BLOCK_SIZE) {
            int kEnd = (kk + BLOCK_SIZE < k) ? kk + BLOCK_SIZE : k;
            
            for (int jj = 0; jj < n; jj += BLOCK_SIZE) {
                int jEnd = (jj + BLOCK_SIZE < n) ? jj + BLOCK_SIZE : n;
                
                // Process block
                for (int i = ii; i < iEnd; i++) {
                    for (int kIdx = kk; kIdx < kEnd; kIdx++) {
                        // Broadcast a[i][k] to all elements of an AVX vector
                        __m256d aik = _mm256_set1_pd(aData[i * aCols + kIdx]);
                        
                        int j = jj;
                        int bOffset = kIdx * n;
                        int resultOffset = i * n;
                        
                        // Process 4 doubles at a time with AVX
                        for (; j + 4 <= jEnd; j += 4) {
                            __m256d b_vec = _mm256_loadu_pd(&bData[bOffset + j]);
                            __m256d result_vec = _mm256_loadu_pd(&resultData[resultOffset + j]);
                            
                            // Multiply and accumulate: result += aik * b
                            result_vec = _mm256_add_pd(result_vec, _mm256_mul_pd(aik, b_vec));
                            
                            _mm256_storeu_pd(&resultData[resultOffset + j], result_vec);
                        }
                        
                        // Handle remainder
                        for (; j < jEnd; j++) {
                            resultData[resultOffset + j] += aData[i * aCols + kIdx] * bData[bOffset + j];
                        }
                    }
                }
            }
        }
    }
}
#endif

/**
 * Fallback scalar implementation (no SIMD).
 * Uses cache blocking for better cache utilization.
 */
static void multiplyScalar(const double* __restrict__ aData, int aRows, int aCols,
                          const double* __restrict__ bData, int bRows, int bCols,
                          double* __restrict__ resultData) {
    int m = aRows;
    int k = aCols;
    int n = bCols;
    
    // Clear result
    memset(resultData, 0, m * n * sizeof(double));
    
    // Blocked multiplication
    for (int ii = 0; ii < m; ii += BLOCK_SIZE) {
        int iEnd = (ii + BLOCK_SIZE < m) ? ii + BLOCK_SIZE : m;
        
        for (int kk = 0; kk < k; kk += BLOCK_SIZE) {
            int kEnd = (kk + BLOCK_SIZE < k) ? kk + BLOCK_SIZE : k;
            
            for (int jj = 0; jj < n; jj += BLOCK_SIZE) {
                int jEnd = (jj + BLOCK_SIZE < n) ? jj + BLOCK_SIZE : n;
                
                // Process block
                for (int i = ii; i < iEnd; i++) {
                    for (int kIdx = kk; kIdx < kEnd; kIdx++) {
                        double aik = aData[i * aCols + kIdx];
                        int bOffset = kIdx * n;
                        int resultOffset = i * n;
                        
                        for (int j = jj; j < jEnd; j++) {
                            resultData[resultOffset + j] += aik * bData[bOffset + j];
                        }
                    }
                }
            }
        }
    }
}

/**
 * JNI entry point for native matrix multiplication.
 * Automatically selects the best SIMD implementation based on compile-time detection.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_core_RmRMatrixOps_multiplyNative(JNIEnv* env, jclass clazz,
                                                   jdoubleArray aData, jint aRows, jint aCols,
                                                   jdoubleArray bData, jint bRows, jint bCols,
                                                   jdoubleArray resultData) {
    // Get array pointers
    jdouble* a = env->GetDoubleArrayElements(aData, nullptr);
    jdouble* b = env->GetDoubleArrayElements(bData, nullptr);
    jdouble* result = env->GetDoubleArrayElements(resultData, nullptr);
    
    if (a == nullptr || b == nullptr || result == nullptr) {
        // Memory allocation failed
        if (a) env->ReleaseDoubleArrayElements(aData, a, JNI_ABORT);
        if (b) env->ReleaseDoubleArrayElements(bData, b, JNI_ABORT);
        if (result) env->ReleaseDoubleArrayElements(resultData, result, JNI_ABORT);
        return;
    }
    
    // Select best implementation based on available SIMD
#ifdef HAS_AVX
    multiplyAVX(a, aRows, aCols, b, bRows, bCols, result);
#elif defined(HAS_SSE2)
    multiplySSE2(a, aRows, aCols, b, bRows, bCols, result);
#elif defined(HAS_NEON)
    multiplyNEON(a, aRows, aCols, b, bRows, bCols, result);
#else
    multiplyScalar(a, aRows, aCols, b, bRows, bCols, result);
#endif
    
    // Release arrays
    env->ReleaseDoubleArrayElements(aData, a, JNI_ABORT);  // Read-only, no need to copy back
    env->ReleaseDoubleArrayElements(bData, b, JNI_ABORT);  // Read-only, no need to copy back
    env->ReleaseDoubleArrayElements(resultData, result, 0); // Write results back
}


JNIEXPORT jint JNICALL
Java_androidx_rmr_core_RmRHardware_nativeGetSimdLevel(JNIEnv* env, jclass clazz) {
    static constexpr jint SIMD_NATIVE_NONE = 0;
    static constexpr jint SIMD_NATIVE_NEON = 1;
    static constexpr jint SIMD_NATIVE_SSE = 2;
    static constexpr jint SIMD_NATIVE_AVX = 3;

#if defined(HAS_NEON)
    return SIMD_NATIVE_NEON;
#elif defined(__x86_64__) || defined(__i386__)
#if defined(__has_builtin)
#if __has_builtin(__builtin_cpu_supports)
    if (__builtin_cpu_supports("avx")) {
        return SIMD_NATIVE_AVX;
    }
#endif
#endif
#if defined(HAS_SSE2)
    return SIMD_NATIVE_SSE;
#else
    return SIMD_NATIVE_NONE;
#endif
#elif defined(HAS_SSE2)
    return SIMD_NATIVE_SSE;
#else
    return SIMD_NATIVE_NONE;
#endif
}

/**
 * JNI_OnLoad - Called when the native library is loaded.
 * Notifies Java side that native support is available.
 */
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    
    // Find the RmRHardware class and set native availability flag
    jclass hardwareClass = env->FindClass("androidx/rmr/core/RmRHardware");
    if (hardwareClass != nullptr) {
        jmethodID setNativeAvailable = env->GetStaticMethodID(
            hardwareClass, "setNativeAvailable", "(Z)V");
        
        if (setNativeAvailable != nullptr) {
            env->CallStaticVoidMethod(hardwareClass, setNativeAvailable, JNI_TRUE);
        }
        
        env->DeleteLocalRef(hardwareClass);
    }
    
    return JNI_VERSION_1_6;
}

} // extern "C"
