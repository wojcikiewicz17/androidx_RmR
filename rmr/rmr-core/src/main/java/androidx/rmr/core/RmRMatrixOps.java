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

package androidx.rmr.core;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRMatrixOps {
    private RmRMatrixOps() {
    }

    private static native void multiplyNative(
            double[] aData,
            int aRows,
            int aCols,
            double[] bData,
            int bRows,
            int bCols,
            double[] resultData);

    @NonNull
    public static RmRMatrix multiply(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        if (left.getCols() != right.getRows()) {
            throw new IllegalArgumentException("Incompatible matrix dimensions.");
        }
        if (RmRHardware.ensureNativeLoaded()) {
            int rows = left.getRows();
            int cols = right.getCols();
            double[] result = new double[rows * cols];
            multiplyNative(
                    left.getDataUnsafe(),
                    rows,
                    left.getCols(),
                    right.getDataUnsafe(),
                    right.getRows(),
                    cols,
                    result);
            return RmRMatrix.wrap(rows, cols, result);
        }
        return multiplyScalar(left, right);
    }

    public static void multiplyInto(
            @NonNull RmRMatrix left,
            @NonNull RmRMatrix right,
            @NonNull RmRMatrix out) {
        if (left.getCols() != right.getRows()) {
            throw new IllegalArgumentException("Incompatible matrix dimensions.");
        }
        if (left.getRows() != out.getRows() || right.getCols() != out.getCols()) {
            throw new IllegalArgumentException("Output matrix has incompatible dimensions.");
        }
        double[] outData = out.getDataUnsafe();
        int outLength = outData.length;
        for (int i = 0; i < outLength; i++) {
            outData[i] = 0.0;
        }
        if (RmRHardware.ensureNativeLoaded()) {
            multiplyNative(
                    left.getDataUnsafe(),
                    left.getRows(),
                    left.getCols(),
                    right.getDataUnsafe(),
                    right.getRows(),
                    right.getCols(),
                    outData);
        } else {
            multiplyScalarInto(left, right, outData);
        }
    }



    public static void transposeInto(@NonNull RmRMatrix input, @NonNull RmRMatrix out) {
        if (input.getRows() != out.getCols() || input.getCols() != out.getRows()) {
            throw new IllegalArgumentException("Output matrix has incompatible dimensions.");
        }
        double[] inData = input.getDataUnsafe();
        double[] outData = out.getDataUnsafe();
        int inRows = input.getRows();
        int inCols = input.getCols();
        for (int row = 0; row < inRows; row++) {
            int inOffset = row * inCols;
            for (int col = 0; col < inCols; col++) {
                outData[col * inRows + row] = inData[inOffset + col];
            }
        }
    }

    public static void applyInto(@NonNull RmRMatrix input, @NonNull RmRMatrix out) {
        input.linearFlipInto(out);
    }

    public static double vectorDot(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        if (left.getCols() != 1 || right.getCols() != 1 || left.getRows() != right.getRows()) {
            throw new IllegalArgumentException("Dot product requires Nx1 vectors with matching dimensions.");
        }
        double[] leftData = left.getDataUnsafe();
        double[] rightData = right.getDataUnsafe();
        double sum = 0.0;
        for (int i = 0; i < leftData.length; i++) {
            sum += leftData[i] * rightData[i];
        }
        return sum;
    }

    @NonNull
    public static RmRMatrix add(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        return left.add(right);
    }

    @NonNull
    public static RmRMatrix linearFlip(@NonNull RmRMatrix matrix) {
        return matrix.linearFlip();
    }

    private static void multiplyScalarInto(
            @NonNull RmRMatrix left,
            @NonNull RmRMatrix right,
            double[] result) {
        int rows = left.getRows();
        int cols = left.getCols();
        int resultCols = right.getCols();
        double[] leftData = left.getDataUnsafe();
        double[] rightData = right.getDataUnsafe();
        if (resultCols == 1) {
            multiplyVector(leftData, rightData, result, rows, cols);
        } else if (shouldBlockForMultiply(rows, cols, resultCols)) {
            multiplyBlocked(leftData, rightData, result, rows, cols, resultCols);
        } else if (shouldTransposeForMultiply(rows, cols, resultCols)) {
            multiplyWithTransposedRight(leftData, rightData, result, rows, cols, resultCols);
        } else {
            multiplyStandard(leftData, rightData, result, rows, cols, resultCols);
        }
    }

    @NonNull
    private static RmRMatrix multiplyScalar(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        int rows = left.getRows();
        int cols = left.getCols();
        int resultCols = right.getCols();
        double[] leftData = left.getDataUnsafe();
        double[] rightData = right.getDataUnsafe();
        double[] result = new double[rows * resultCols];
        if (resultCols == 1) {
            multiplyVector(leftData, rightData, result, rows, cols);
        } else if (shouldBlockForMultiply(rows, cols, resultCols)) {
            multiplyBlocked(leftData, rightData, result, rows, cols, resultCols);
        } else if (shouldTransposeForMultiply(rows, cols, resultCols)) {
            multiplyWithTransposedRight(leftData, rightData, result, rows, cols, resultCols);
        } else {
            multiplyStandard(leftData, rightData, result, rows, cols, resultCols);
        }
        return RmRMatrix.wrap(rows, resultCols, result);
    }

    private static void multiplyStandard(
            double[] leftData,
            double[] rightData,
            double[] result,
            int rows,
            int cols,
            int resultCols) {
        for (int row = 0; row < rows; row++) {
            int rowOffset = row * cols;
            int resultOffset = row * resultCols;
            for (int k = 0; k < cols; k++) {
                double leftValue = leftData[rowOffset + k];
                int rightOffset = k * resultCols;
                for (int col = 0; col < resultCols; col++) {
                    result[resultOffset + col] += leftValue * rightData[rightOffset + col];
                }
            }
        }
    }

    private static void multiplyWithTransposedRight(
            double[] leftData,
            double[] rightData,
            double[] result,
            int rows,
            int cols,
            int resultCols) {
        double[] rightTransposed = new double[resultCols * cols];
        for (int row = 0; row < cols; row++) {
            int rightOffset = row * resultCols;
            for (int col = 0; col < resultCols; col++) {
                rightTransposed[col * cols + row] = rightData[rightOffset + col];
            }
        }
        for (int row = 0; row < rows; row++) {
            int leftOffset = row * cols;
            int resultOffset = row * resultCols;
            for (int col = 0; col < resultCols; col++) {
                int rightOffset = col * cols;
                double sum = 0.0;
                for (int k = 0; k < cols; k++) {
                    sum += leftData[leftOffset + k] * rightTransposed[rightOffset + k];
                }
                result[resultOffset + col] = sum;
            }
        }
    }

    private static void multiplyVector(double[] leftData, double[] rightData, double[] result, int rows, int cols) {
        if (cols == 4) {
            double v0 = rightData[0];
            double v1 = rightData[1];
            double v2 = rightData[2];
            double v3 = rightData[3];
            for (int row = 0; row < rows; row++) {
                int rowOffset = row * 4;
                result[row] = leftData[rowOffset] * v0
                        + leftData[rowOffset + 1] * v1
                        + leftData[rowOffset + 2] * v2
                        + leftData[rowOffset + 3] * v3;
            }
            return;
        }
        for (int row = 0; row < rows; row++) {
            int rowOffset = row * cols;
            double sum = 0.0;
            for (int k = 0; k < cols; k++) {
                sum += leftData[rowOffset + k] * rightData[k];
            }
            result[row] = sum;
        }
    }

    private static void multiplyBlocked(
            double[] leftData,
            double[] rightData,
            double[] result,
            int rows,
            int cols,
            int resultCols) {
        int block = 32;
        for (int row = 0; row < rows; row += block) {
            int rowMax = Math.min(row + block, rows);
            for (int k = 0; k < cols; k += block) {
                int kMax = Math.min(k + block, cols);
                for (int col = 0; col < resultCols; col += block) {
                    int colMax = Math.min(col + block, resultCols);
                    for (int ii = row; ii < rowMax; ii++) {
                        int rowOffset = ii * cols;
                        int resultOffset = ii * resultCols;
                        for (int kk = k; kk < kMax; kk++) {
                            double leftValue = leftData[rowOffset + kk];
                            int rightOffset = kk * resultCols;
                            for (int jj = col; jj < colMax; jj++) {
                                result[resultOffset + jj] += leftValue * rightData[rightOffset + jj];
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean shouldBlockForMultiply(int rows, int cols, int resultCols) {
        long workload = (long) rows * (long) cols * (long) resultCols;
        return rows >= 8 && cols >= 8 && resultCols >= 8 && workload >= 65536L;
    }

    private static boolean shouldTransposeForMultiply(int rows, int cols, int resultCols) {
        long workload = (long) rows * (long) cols * (long) resultCols;
        return workload >= 4096L;
    }
}
