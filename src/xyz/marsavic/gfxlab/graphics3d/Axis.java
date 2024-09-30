package xyz.marsavic.gfxlab.graphics3d;

public enum Axis {
    X, Y, Z;

    private static final Axis[] vals = values();

    public int index() { return this.ordinal(); }

    public Axis next() {
        return vals[(this.ordinal() + 1) % vals.length];
    }
}