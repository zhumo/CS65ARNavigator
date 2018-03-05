// Taken from javax.vecmath

package edu.dartmouth.com.arnavigation.math;

import java.io.Serializable;

public abstract class Tuple3d implements Serializable {
    public double x;
    public double y;
    public double z;

    public Tuple3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Tuple3d(double[] t) {
        this.x = t[0];
        this.y = t[1];
        this.z = t[2];
    }

    public Tuple3d(Tuple3d t1) {
        this.x = t1.x;
        this.y = t1.y;
        this.z = t1.z;
    }

    public Tuple3d(Tuple3f t1) {
        this.x = (double)t1.x;
        this.y = (double)t1.y;
        this.z = (double)t1.z;
    }

    public Tuple3d() {
        this.x = 0.0D;
        this.y = 0.0D;
        this.z = 0.0D;
    }

    public final void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public final void set(double[] t) {
        this.x = t[0];
        this.y = t[1];
        this.z = t[2];
    }

    public final void set(Tuple3d t1) {
        this.x = t1.x;
        this.y = t1.y;
        this.z = t1.z;
    }

    public final void set(Tuple3f t1) {
        this.x = (double)t1.x;
        this.y = (double)t1.y;
        this.z = (double)t1.z;
    }

    public final void get(double[] t) {
        t[0] = this.x;
        t[1] = this.y;
        t[2] = this.z;
    }

    public final void get(Tuple3d t) {
        t.x = this.x;
        t.y = this.y;
        t.z = this.z;
    }

    public final void add(Tuple3d t1, Tuple3d t2) {
        this.x = t1.x + t2.x;
        this.y = t1.y + t2.y;
        this.z = t1.z + t2.z;
    }

    public final void add(Tuple3d t1) {
        this.x += t1.x;
        this.y += t1.y;
        this.z += t1.z;
    }

    public final void sub(Tuple3d t1, Tuple3d t2) {
        this.x = t1.x - t2.x;
        this.y = t1.y - t2.y;
        this.z = t1.z - t2.z;
    }

    public final void sub(Tuple3d t1) {
        this.x -= t1.x;
        this.y -= t1.y;
        this.z -= t1.z;
    }

    public final void negate(Tuple3d t1) {
        this.x = -t1.x;
        this.y = -t1.y;
        this.z = -t1.z;
    }

    public final void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
    }

    public final void scale(double s, Tuple3d t1) {
        this.x = s * t1.x;
        this.y = s * t1.y;
        this.z = s * t1.z;
    }

    public final void scale(double s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
    }

    public final void scaleAdd(double s, Tuple3d t1, Tuple3d t2) {
        this.x = s * t1.x + t2.x;
        this.y = s * t1.y + t2.y;
        this.z = s * t1.z + t2.z;
    }

    public final void scaleAdd(double s, Tuple3d t1) {
        this.x = s * this.x + t1.x;
        this.y = s * this.y + t1.y;
        this.z = s * this.z + t1.z;
    }

    public int hashCode() {
        long xbits = VecMathUtil.doubleToLongBits(this.x);
        long ybits = VecMathUtil.doubleToLongBits(this.y);
        long zbits = VecMathUtil.doubleToLongBits(this.z);
        return (int)(xbits ^ xbits >> 32 ^ ybits ^ ybits >> 32 ^ zbits ^ zbits >> 32);
    }

    public boolean equals(Tuple3d t1) {
        return t1 != null && this.x == t1.x && this.y == t1.y && this.z == t1.z;
    }

    public boolean epsilonEquals(Tuple3d t1, double epsilon) {
        return Math.abs(t1.x - this.x) <= epsilon && Math.abs(t1.y - this.y) <= epsilon && Math.abs(t1.z - this.z) <= epsilon;
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    public final void absolute(Tuple3d t) {
        this.set(t);
        this.absolute();
    }

    public final void absolute() {
        if (this.x < 0.0D) {
            this.x = -this.x;
        }

        if (this.y < 0.0D) {
            this.y = -this.y;
        }

        if (this.z < 0.0D) {
            this.z = -this.z;
        }

    }

    /** @deprecated */
    public final void clamp(float min, float max) {
        this.clampMin(min);
        this.clampMax(max);
    }

    /** @deprecated */
    public final void clamp(float min, float max, Tuple3d t) {
        this.set(t);
        this.clamp(min, max);
    }

    /** @deprecated */
    public final void clampMin(float min) {
        if (this.x < (double)min) {
            this.x = (double)min;
        }

        if (this.y < (double)min) {
            this.y = (double)min;
        }

        if (this.z < (double)min) {
            this.z = (double)min;
        }

    }

    /** @deprecated */
    public final void clampMin(float min, Tuple3d t) {
        this.set(t);
        this.clampMin(min);
    }

    /** @deprecated */
    public final void clampMax(float max, Tuple3d t) {
        this.set(t);
        this.clampMax(max);
    }

    /** @deprecated */
    public final void clampMax(float max) {
        if (this.x > (double)max) {
            this.x = (double)max;
        }

        if (this.y > (double)max) {
            this.y = (double)max;
        }

        if (this.z > (double)max) {
            this.z = (double)max;
        }

    }

    public final void clamp(double min, double max) {
        this.clampMin(min);
        this.clampMax(max);
    }

    public final void clamp(double min, double max, Tuple3d t) {
        this.set(t);
        this.clamp(min, max);
    }

    public final void clampMin(double min) {
        if (this.x < min) {
            this.x = min;
        }

        if (this.y < min) {
            this.y = min;
        }

        if (this.z < min) {
            this.z = min;
        }

    }

    public final void clampMin(double min, Tuple3d t) {
        this.set(t);
        this.clampMin(min);
    }

    public final void clampMax(double max, Tuple3d t) {
        this.set(t);
        this.clampMax(max);
    }

    public final void clampMax(double max) {
        if (this.x > max) {
            this.x = max;
        }

        if (this.y > max) {
            this.y = max;
        }

        if (this.z > max) {
            this.z = max;
        }

    }

    /** @deprecated */
    public final void interpolate(Tuple3d t1, Tuple3d t2, float alpha) {
        this.set(t1);
        this.interpolate(t2, alpha);
    }

    /** @deprecated */
    public final void interpolate(Tuple3d t1, float alpha) {
        float beta = 1.0F - alpha;
        this.x = (double)beta * this.x + (double)alpha * t1.x;
        this.y = (double)beta * this.y + (double)alpha * t1.y;
        this.z = (double)beta * this.z + (double)alpha * t1.z;
    }

    public final void interpolate(Tuple3d t1, Tuple3d t2, double alpha) {
        this.set(t1);
        this.interpolate(t2, alpha);
    }

    public final void interpolate(Tuple3d t1, double alpha) {
        double beta = 1.0D - alpha;
        this.x = beta * this.x + alpha * t1.x;
        this.y = beta * this.y + alpha * t1.y;
        this.z = beta * this.z + alpha * t1.z;
    }
}
