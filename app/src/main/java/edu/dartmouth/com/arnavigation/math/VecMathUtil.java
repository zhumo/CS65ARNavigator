// taken from javax.vecmath

package edu.dartmouth.com.arnavigation.math;

class VecMathUtil {
    static long floatToIntBits(float f) {
        return f == 0.0F ? 0L : doubleToLongBitsImpl((double)f);
    }

    static long doubleToLongBits(double d) {
        return d == 0.0D ? 0L : doubleToLongBitsImpl(d);
    }

    private static long doubleToLongBitsImpl(double v) {
        if (Double.isNaN(v)) {
            return 9221120237041090560L;
        } else {
            long sign = v < 0.0D ? -9223372036854775808L : 0L;
            long exponent = 0L;
            double absV = Math.abs(v);
            if (Double.isInfinite(v)) {
                exponent = 9218868437227405312L;
            } else if (absV == 0.0D) {
                exponent = 0L;
            } else {
                int guess = (int)Math.floor(Math.log(absV) / Math.log(2.0D));
                guess = Math.max(-1023, Math.min(guess, 1023));
                double exp = Math.pow(2.0D, (double)guess);

                for(absV /= exp; absV > 2.0D; absV /= 2.0D) {
                    ++guess;
                }

                while(absV < 1.0D && guess > 1024) {
                    --guess;
                    absV *= 2.0D;
                }

                exponent = (long)guess + 1023L << 52;
            }

            if (exponent <= 0L) {
                absV /= 2.0D;
            }

            long mantissa = (long)(absV % 1.0D * Math.pow(2.0D, 52.0D));
            return sign | exponent | mantissa & 4503599627370495L;
        }
    }

    private VecMathUtil() {
    }
}
