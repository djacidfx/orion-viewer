/*
 * __mulodi4: signed 64-bit multiply with overflow detection.
 *
 * clang lowers __builtin_mul_overflow on 64-bit operands (used by libarchive's
 * archive_integer.h) to this compiler-rt call on 32-bit ARM/x86. NDK 17, used
 * for the Android 4.0 build, links libgcc instead of compiler-rt, and libgcc
 * never provided it. Newer NDKs ship it, so the definition is weak and only
 * compiled in for old NDKs (see mupdfModule/jni/CMakeLists.txt).
 *
 * Semantics follow compiler-rt's mulodi4.c: returns the wrapped product and
 * sets *overflow to 1 on overflow, 0 otherwise.
 */
#include <limits.h>

__attribute__((weak))
long long __mulodi4(long long a, long long b, int *overflow)
{
    const int N = (int)(sizeof(long long) * CHAR_BIT);
    const long long MIN = (long long)((unsigned long long)1 << (N - 1));
    const long long MAX = ~MIN;
    long long result = (long long)((unsigned long long)a * (unsigned long long)b);
    long long sa, abs_a, sb, abs_b;

    *overflow = 0;
    if (a == MIN) {
        if (b != 0 && b != 1)
            *overflow = 1;
        return result;
    }
    if (b == MIN) {
        if (a != 0 && a != 1)
            *overflow = 1;
        return result;
    }
    sa = a >> (N - 1);
    abs_a = (a ^ sa) - sa;
    sb = b >> (N - 1);
    abs_b = (b ^ sb) - sb;
    if (abs_a < 2 || abs_b < 2)
        return result;
    if (sa == sb) {
        if (abs_a > MAX / abs_b)
            *overflow = 1;
    } else {
        if (abs_a > MIN / -abs_b)
            *overflow = 1;
    }
    return result;
}
