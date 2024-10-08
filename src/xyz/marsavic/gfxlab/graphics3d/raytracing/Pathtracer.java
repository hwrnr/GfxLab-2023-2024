package xyz.marsavic.gfxlab.graphics3d.raytracing;

import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.*;
import xyz.marsavic.random.sampling.Sampler;
import xyz.marsavic.utils.Hashing;


public class Pathtracer extends Raytracer {
	
	private static final double EPSILON = 1e-9;
	private static final long seed = 0x68EFD508E309A865L;
	
	private final int maxDepth;
	
	
	public Pathtracer(Scene scene, Camera camera, int maxDepth) {
		super(scene, camera);
		this.maxDepth = maxDepth;
	}
	
	public Pathtracer(Scene scene, Camera camera) {
		this(scene, camera, 16);
	}
	
	
	@Override
	protected Color sample(Ray ray, double t) {
		return radiance(ray, t, maxDepth, new Sampler(Hashing.mix(seed, ray)));
	}
	
	
	private Color radiance(Ray ray, double tFrame, int depthRemaining, Sampler sampler) {
//		tFrame = 0; // DELETE THIS
		if (depthRemaining <= 0) return Color.gray(0.01);
		
		Hit hit = scene.solid().firstHit(ray, EPSILON, tFrame);
		if (hit.t() == Double.POSITIVE_INFINITY) {
			return scene.colorBackground();
		}
		
		Material material = hit.material();
		Color result = material.emittance();
		
		Vec3 i = ray.d().inverse();                 // Incoming direction
		Vec3 n_ = hit.n_();                         // Normalized normal to the body surface at the hit point
		BSDF.Result bsdfResult = material.bsdf().sample(sampler, n_, i);
		
		if (bsdfResult.color().notZero()) {
			Vec3 p = ray.at(hit.t());               // Point of collision
			Ray rayScattered = Ray.pd(p, bsdfResult.out());
			Color rO = radiance(rayScattered, tFrame,depthRemaining - 1, sampler);
			Color rI = rO.mul(bsdfResult.color());
			result = result.add(rI);
		}
		
		return result;
	}
	
}
