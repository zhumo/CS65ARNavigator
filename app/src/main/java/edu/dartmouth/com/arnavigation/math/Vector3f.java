// Taken from javax.vecmath

package edu.dartmouth.com.arnavigation.math;

import java.io.Serializable;

public class Vector3f extends Tuple3f implements Serializable {
    public Vector3f(float x, float y, float z) {
        super(x, y, z);
    }

    public Vector3f(float[] v) {
        super(v);
    }

    public Vector3f(Vector3f v1) {
        super(v1);
    }

    public Vector3f(Vector3d v1) {
        super(v1);
    }

    public Vector3f(Tuple3d t1) {
        super(t1);
    }

    public Vector3f(Tuple3f t1) {
        super(t1);
    }

    public Vector3f() {
    }

    public final float lengthSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public final float length() {
        return (float)Math.sqrt((double)this.lengthSquared());
    }

    public final void cross(Vector3f v1, Vector3f v2) {
        this.set(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x);
    }

    public final float dot(Vector3f v1) {
        return this.x * v1.x + this.y * v1.y + this.z * v1.z;
    }

    public final void normalize(Vector3f v1) {
        this.set(v1);
        this.normalize();
    }

    public final void normalize() {
        double d = (double)this.length();
        this.x = (float)((double)this.x / d);
        this.y = (float)((double)this.y / d);
        this.z = (float)((double)this.z / d);
    }

    public final float angle(Vector3f v1) {
        double xx = (double)(this.y * v1.z - this.z * v1.y);
        double yy = (double)(this.z * v1.x - this.x * v1.z);
        double zz = (double)(this.x * v1.y - this.y * v1.x);
        double cross = Math.sqrt(xx * xx + yy * yy + zz * zz);
        return (float)Math.abs(Math.atan2(cross, (double)this.dot(v1)));
    }
}
