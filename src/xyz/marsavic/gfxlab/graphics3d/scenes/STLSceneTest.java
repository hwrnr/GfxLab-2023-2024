package xyz.marsavic.gfxlab.graphics3d.scenes;

import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.Light;
import xyz.marsavic.gfxlab.graphics3d.Material;
import xyz.marsavic.gfxlab.graphics3d.Scene;
import xyz.marsavic.gfxlab.graphics3d.solids.*;
import xyz.marsavic.utils.Numeric;

import java.util.Collections;


public class STLSceneTest extends Scene.Base {

	public STLSceneTest() {

//		STLSolid stlSolid = new STLSolid("/home/hawerner/faks/master1/grafika/STL_files/wood");
		
		solid = STLSolid.group("/home/hawerner/faks/master1/grafika/STL_files");

		Collections.addAll(lights,
				Light.pc(Vec3.xyz(-1, 1, 1), Color.WHITE),
				Light.pc(Vec3.xyz( 2, 1, 2), Color.rgb(1.0, 0.5, 0.5)),
				Light.pc(Vec3.xyz(0, 0, -1), Color.gray(0.2))
		);
	}
	
}
