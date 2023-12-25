package xyz.marsavic.gfxlab.graphics3d.solids;

import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.Hit;
import xyz.marsavic.gfxlab.graphics3d.Material;
import xyz.marsavic.gfxlab.graphics3d.Ray;
import xyz.marsavic.gfxlab.graphics3d.Solid;
import xyz.marsavic.utils.Numeric;

public interface SDF extends Solid {

	double dist(Vec3 p);
	
	
	double DELTA_HIT = 1e-4;
	int MAX_STEPS = 32;
	
	@Override
	default Hit firstHit(Ray ray, double afterTime) {
		Ray ray_ = ray.normalized_();
		double l = ray.d().length();
		double t = 0;
		int nSteps = 0;
	
		while (true) {
			Vec3 p = ray_.at(t);
			double d = dist(p);
			if (d < DELTA_HIT) {
				return new SDFHit(t / l, this, p);
			}
			t += d;
			nSteps++;
			if (nSteps >= MAX_STEPS) {
				return (d < 1) ? new SDFHit(t / l, this, p) : Hit.AtInfinity.axisAlignedGoingIn(ray_.d());
			}
		}
	}

	
	class SDFHit implements Hit {
		
		
		private final double t;
		private final SDF sdf;
		private final Vec3 p;
		
		SDFHit(double t, SDF sdf, Vec3 p) {
			this.t = t;
			this.sdf = sdf;
			this.p = p;
		}
		
		@Override
		public double t() {
			return t;
		}
		
		static final double EPS = 0.0001;
		static final Vec3 dx = Vec3.EX.mul(EPS);
		static final Vec3 dy = Vec3.EY.mul(EPS);
		static final Vec3 dz = Vec3.EZ.mul(EPS);
		
		@Override
		public Vec3 n() {
			double d = sdf.dist(p);
			return Vec3.xyz(
					(sdf.dist(p.add(dx)) - d) / EPS,
					(sdf.dist(p.add(dy)) - d) / EPS,
					(sdf.dist(p.add(dz)) - d) / EPS
			);
		}
		
		@Override
		public Material material() {
			return Material.matte(Color.hsb(0.6, 0.7, 1.0)).specular(Color.WHITE).shininess(32);
		}
		
		@Override
		public Vector uv() {
			return Vector.ZERO;
		}
	}
	
	
	
	static SDF ball(Vec3 c, double r) { return p -> p.sub(c).length() - r; }
	static SDF box(Vec3 r) { return p -> Vec3.max(p.abs().sub(r), Vec3.ZERO).length() - Math.max(0, r.sub(p.abs()).min()); }
	static SDF union(SDF a, SDF b) { return p -> Math.min(a.dist(p), b.dist(p)); }
	static SDF extend(SDF sdf, double r) { return p -> sdf.dist(p) - r; }
	static SDF smoothUnion(SDF a, SDF b, double r) {
		return p -> {
			double da = a.dist(p);
			double db = b.dist(p);
			double k = Numeric.clamp((db - da) / r * 0.5 + 0.5);
			return da * k + db * (1 - k) - k * (1 - k) * r;
		};
	}
}
