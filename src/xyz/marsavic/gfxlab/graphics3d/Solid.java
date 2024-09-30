package xyz.marsavic.gfxlab.graphics3d;


import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Vec3;

public abstract class Solid {

	protected BoundingBox boundingBox = BoundingBox.$.cr(Vec3.ZERO, Vec3.INFTY);

	/**
	 * Returns the first hit of the ray into the surface of the solid, occurring strictly after the given time.
	 * If the ray misses the Solid, the hit at infinity is returned.
	 * All hits along the same line should alternate between entering and exiting hits, meaning that the sign of the
	 * dot product between the normal at the hit and the line direction should alternate. This should also hold for
	 * the hit at infinity.
	 */
	public abstract Hit firstHit(Ray ray, double afterTime, double tFrame);

	public abstract boolean intersects(BoundingBox boundingBox);

	public BoundingBox boundingBox(){
		return this.boundingBox;
	}

	public Hit firstHit(Ray ray, double tFrame) {
		return firstHit(ray, 0, tFrame);
	}


	/** Is there any hit between afterTime and beforeTime. */
	public boolean hitBetween(Ray ray, double afterTime, double beforeTime, double tFrame) {
		double t = firstHit(ray, afterTime, tFrame).t();
		return t < beforeTime;
	}


	public Solid transformed(Affine t) {
		return new Solid() {
			final Affine tInv = t.inverse();
			final Affine tInvTransposed = tInv.transposeWithoutTranslation();

			@Override
			public Hit firstHit(Ray ray, double afterTime, double tFrame) {
				Ray rayO = tInv.at(ray);
				Hit hitO = Solid.this.firstHit(rayO, afterTime, tFrame);
				Vec3 n = tInvTransposed.at(hitO.n());
				return hitO.withN(n);
			}

			//@Override
			public boolean intersects(BoundingBox boundingBox) {
				return false;
			}
		};
	}


	/** The solid made of all the points contained in at least k of the given solids. */
	static Solid atLeast(int k, Solid... solids) {
		return new Solid() {
			@Override
			public Hit firstHit(Ray ray, double afterTime, double tFrame) {
				int n = solids.length;
				Hit[] hits = new Hit[n];
				int[] d = new int[n];
				int count = 0;

				for (int i = 0; i < n; i++) {
					Hit hit = solids[i].firstHit(ray, afterTime, tFrame);
					hits[i] = hit;
					boolean inside = hit.n().dot(ray.d()) > 0;
					d[i] = inside ? -1 : 1;
					count += inside ? 1 : 0;
				}

				boolean isInside = count >= k;

				while (true) {
					double tHitMin = Double.POSITIVE_INFINITY;
					int iMin = -1;
					for (int i = 0; i < n; i++) {
						double t = hits[i].t();
						if (t < tHitMin) {
							tHitMin = t;
							iMin = i;
						}
					}

					if (tHitMin == Double.POSITIVE_INFINITY) {
						return Hit.AtInfinity.axisAligned(ray.d(), isInside);
					}

					count += d[iMin];
					d[iMin] = -d[iMin];
					boolean wasInside = isInside;
					isInside = count >= k;
					if (wasInside != isInside) {
						return hits[iMin];
					}
					hits[iMin] = solids[iMin].firstHit(ray, tHitMin);
				}
			}

			@Override
			public boolean intersects(BoundingBox boundingBox) {
				return false;
			}
		};

	}

	public Solid withMaterial(F1<Material, Vector> mapMaterial) {
		return new Solid() {
			@Override
			public Hit firstHit(Ray ray, double afterTime, double tFrame) {
				Hit hit = Solid.this.firstHit(ray, afterTime, tFrame);
				return hit.withMaterial(mapMaterial.at(hit.uv()));
			}

			@Override
			public boolean intersects(BoundingBox boundingBox) {
				return false;
			}
		};
	}

	static Solid union(Solid... solids) {
		return atLeast(1, solids);
	}


	public static Solid intersection(Solid... solids) {
		return atLeast(solids.length, solids);
	}


	public Solid complement() {
		return new Solid() {
			@Override
			public Hit firstHit(Ray ray, double afterTime, double tFrame) {
				Hit hit = Solid.this.firstHit(ray, afterTime, tFrame);
				return hit.inverted();
			}

			@Override
			public boolean intersects(BoundingBox boundingBox) {
				return false;
			}
		};
	}


	public static Solid difference(Solid a, Solid b) {
		return intersection(a, b.complement());
	}
}
