package xyz.marsavic.gfxlab.graphics3d.raytracing;

import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.ColorFunctionT;
import xyz.marsavic.gfxlab.graphics3d.Camera;
import xyz.marsavic.gfxlab.graphics3d.Ray;
import xyz.marsavic.gfxlab.graphics3d.Scene;


public abstract class Raytracer implements ColorFunctionT {
	
	protected final Scene scene;
	private final Camera camera;
	
	
	public Raytracer(Scene scene, Camera camera) {
		this.scene = scene;
		this.camera = camera;
	}
	
	
	protected abstract Color sample(Ray ray, double t);
	
	
	@Override
	public Color at(double t, Vector p) {
		return sample(camera.exitingRay(p), (t + 1) / 2);
	}
	
}
