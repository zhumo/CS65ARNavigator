// Taken from javax.vecmath

package edu.dartmouth.com.arnavigation.math;

import java.io.Serializable;

public class Vector2f extends Tuple2f implements Serializable {
    public Vector2f(float x, float y) {
        super(x, y);
    }

    public Vector2f(float[] v) {
        super(v);
    }

    public Vector2f(Vector2f v1) {
        super(v1);
    }

    public Vector2f(Vector2d v1) {
        super(v1);
    }

    public Vector2f(Tuple2f t1) {
        super(t1);
    }

    public Vector2f(Tuple2d t1) {
        super(t1);
    }

    public Vector2f() {
    }

    public final float dot(Vector2f v1) {
        return this.x * v1.x + this.y * v1.y;
    }

    public final float length() {
        return (float)Math.sqrt((double)(this.x * this.x + this.y * this.y));
    }

    public final float lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public final void normalize() {
        double d = (double)this.length();
        this.x = (float)((double)this.x / d);
        this.y = (float)((double)this.y / d);
    }

    public final void normalize(Vector2f v1) {
        this.set(v1);
        this.normalize();
    }

    public final float angle(Vector2f v1) {
        return (float)Math.abs(Math.atan2((double)(this.x * v1.y - this.y * v1.x), (double)this.dot(v1)));
    }
}
