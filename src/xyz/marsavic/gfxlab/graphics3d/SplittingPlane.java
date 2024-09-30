package xyz.marsavic.gfxlab.graphics3d;

public class SplittingPlane {
    private final double pos;
    private final Axis axis;

    private SplittingPlane(double pos, Axis axis) {
        this.pos = pos;
        this.axis = axis;
    }

    public static SplittingPlane pa(double pos, Axis axis) { return new SplittingPlane(pos, axis); }

    public double pos() { return pos; }

    public Axis axis() { return axis; }

    @Override
    public String toString() {
        return axis + " " + pos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;

        if (obj.getClass() != this.getClass()) return false;

        final SplittingPlane other = (SplittingPlane) obj;
        if ((this.axis == null) ? (other.axis != null) : !this.axis.equals(other.axis)) return false;

        if (Math.abs(this.pos - other.pos) > 0.0000000001) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.axis() != null ? this.axis().hashCode() : 0);
        hash = 53 * hash + Double.valueOf(this.pos()).hashCode();;
        return hash;
    }
}
