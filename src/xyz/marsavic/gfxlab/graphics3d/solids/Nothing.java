package xyz.marsavic.gfxlab.graphics3d.solids;

import xyz.marsavic.gfxlab.graphics3d.BoundingBox;
import xyz.marsavic.gfxlab.graphics3d.Hit;
import xyz.marsavic.gfxlab.graphics3d.Ray;
import xyz.marsavic.gfxlab.graphics3d.Solid;

public class Nothing extends Solid {

	public static final Nothing INSTANCE = new Nothing();

	@Override
	public Hit firstHit(Ray ray, double afterTime, double tFrame) {
		return Hit.AtInfinity.axisAlignedGoingIn(ray.d());
	}

	//@Override
	public boolean intersects(BoundingBox boundingBox) {
		return false;
	}

}
