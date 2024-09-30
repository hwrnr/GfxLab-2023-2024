package xyz.marsavic.gfxlab.graphics3d;

import xyz.marsavic.gfxlab.BoxedObjectFactory;
import xyz.marsavic.gfxlab.Vec3;


public class BoundingBox {
    public static final BoxedObjectFactory.CR<BoundingBox> $ = BoundingBox::new;

    private final Vec3 c, r;

    private BoundingBox(Vec3 c, Vec3 r) {
        this.c = c;
        this.r = r;
    }

    public Vec3 c(){
        return c;
    }

    public Vec3 r(){
        return r;
    }

    public Vec3 p() { return c.sub(r); }

    public Vec3 q() {
        return c.add(r);
    }

    public Vec3 d() {
        return r.mul(2);
    }

    public double firstHit(Ray ray, double afterTime, double tFrame) {
        Vec3 tP = p().sub(ray.p()).div(ray.d());
        Vec3 tQ = q().sub(ray.p()).div(ray.d());

        Vec3 t0 = Vec3.min(tP, tQ);
        Vec3 t1 = Vec3.max(tP, tQ);

        int iMax0 = t0.maxIndex();
        int iMin1 = t1.minIndex();

        double max0 = t0.get(iMax0);
        double min1 = t1.get(iMin1);

        if (max0 < min1) {
            if (max0 > afterTime) return max0;
            if (min1 > afterTime) return min1;
        }
        return Double.NaN;
    }

    public double area(){
        return d().x()*d().x() + d().y()*d().y() + d().z()*d().z();
    }

    @Override
    public String toString() {
        return c + ", " + r;
    }
}
