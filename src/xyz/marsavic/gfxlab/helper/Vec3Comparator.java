package xyz.marsavic.gfxlab.helper;

import xyz.marsavic.gfxlab.Vec3;

import java.util.Comparator;

public class Vec3Comparator implements Comparator<Vec3> {
    @Override
    public int compare(Vec3 o1, Vec3 o2) {
        if (o1.x() != o2.x()) {
            return Double.compare(o1.x(), o2.x());
        }
        if (o1.y() != o2.y()) {
            return Double.compare(o1.y(), o2.y());
        }
        if (o1.z() != o2.z()) {
            return Double.compare(o1.z(), o2.z());
        }
        return 0;
    }
}
