package xyz.marsavic.gfxlab.graphics3d.solids;

import xyz.marsavic.gfxlab.graphics3d.BoundingBox;
import xyz.marsavic.gfxlab.graphics3d.Hit;
import xyz.marsavic.gfxlab.graphics3d.Ray;
import xyz.marsavic.gfxlab.graphics3d.Solid;

import java.util.Arrays;
import java.util.Collection;


public class Group extends Solid {

	private final Solid[] solids;
	
	
	private Group(Solid[] solids) {
		this.solids = solids;
	}
	
	public static Group of(Solid... solids) {
		return new Group(solids);
	}
	
	public static Group of(Collection<Solid> solids) {
		return new Group(solids.toArray(Solid[]::new));
	}
	
	
	@Override
	public Hit firstHit(Ray ray, double afterTime, double tFrame) {
		Hit minHit = Nothing.INSTANCE.firstHit(ray, afterTime, tFrame);
		double minT = minHit.t();
		
		for (Solid solid : solids) {
			Hit hit = solid.firstHit(ray, afterTime, tFrame);
			double t = hit.t();
			if (t < minT) {
				minT = t;
				minHit = hit;
			}
		}
		
		return minHit;
	}

	@Override
	public boolean intersects(BoundingBox boundingBox) {
		return Arrays.stream(this.solids).anyMatch(t -> t.intersects(boundingBox)); // Ovo neću prosleđivati Milicinom algoritmu
	}

	@Override
	public boolean hitBetween(Ray ray, double afterTime, double beforeTime, double tFrame) {
		for (Solid solid : solids) {
			if (solid.firstHit(ray, afterTime, tFrame).t() < beforeTime) {
				return true;
			}
		}
		return false;
	}
	
}
