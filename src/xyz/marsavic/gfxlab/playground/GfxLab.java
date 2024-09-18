package xyz.marsavic.gfxlab.playground;

import xyz.marsavic.elements.HasOutput;
import xyz.marsavic.functions.F1;
import xyz.marsavic.gfxlab.*;
import xyz.marsavic.gfxlab.aggregation.EAggregator;
import xyz.marsavic.gfxlab.graphics3d.Affine;
import xyz.marsavic.gfxlab.graphics3d.cameras.Perspective;
import xyz.marsavic.gfxlab.graphics3d.cameras.ThinLensFOV;
import xyz.marsavic.gfxlab.graphics3d.cameras.TransformedCamera;
import xyz.marsavic.gfxlab.graphics3d.raytracing.Pathtracer;
import xyz.marsavic.gfxlab.graphics3d.raytracing.RaytracerSimple;
import xyz.marsavic.gfxlab.graphics3d.scenes.*;
import xyz.marsavic.gfxlab.gui.UtilsGL;
import xyz.marsavic.gfxlab.tonemapping.ColorTransform;
import xyz.marsavic.gfxlab.tonemapping.matrixcolor_to_colortransforms.AutoSoft;
import xyz.marsavic.resources.Resource;

import java.nio.file.attribute.AclFileAttributeView;

import static xyz.marsavic.elements.ElementF.e;


public class GfxLab {

	public HasOutput<F1<Resource<Matrix<Integer>>, Integer>> sink;
	
	
	public GfxLab() {
		//                       nFrames   width     height
		var eSize = e(Vec3::new, e(1.0), e(640.0), e(640.0));
		sink =
				e(Fs::frFrameToneMapping,
						new EAggregator(
								e(Fs::transformedColorFunction,
										e(Pathtracer::new,
												e(STLSceneTest::new),
												e(TransformedCamera::new,
														e(Perspective::new, e(0.5)),
														e(Affine.IDENTITY
																.then(Affine.rotationAboutY(0.2))
																.then(Affine.translation(Vec3.xyz(-10, 0, -5)))
																.then(Affine.rotationAboutX(0.25))
														)
												)
										),
/*
										e(Pathtracer::new,
												e(ChristmasTree::new),
												e(TransformedCamera::new,
													e(Perspective::new, e(0.5)),
													e(Affine.IDENTITY
															.then(Affine.rotationAboutX(0))
															.then(Affine.translation(Vec3.xyz(0, 8, -28)))
													)
												)
										),
*/
										e(TransformationsFromSize.toGeometric, eSize)
								),
								eSize,
								e(0xA6A08E5C173D29FL)
						),
						e(Fs::frToneMapping,
//								e(ColorTransform::asColorTransformFromMatrixColor, e(new Identity()))
								e(AutoSoft::new)
						)
				);
	}
	
	
}


class Fs {
	
	public static ColorFunction transformedColorFunction(ColorFunction colorFunction, Transformation transformation) {
		return p -> colorFunction.at(transformation.at(p));
	}
	
	public static F1<Resource<Matrix<Integer>>, Integer> frFrameToneMapping(F1<Resource<Matrix<Color>>, Integer> frFrame, F1<Resource<Matrix<Integer>>, Resource<Matrix<Color>>> frToneMapping) {
		return iFrame -> frToneMapping.at(frFrame.at(iFrame));
	}
	
	
	// Contract: When a resource is a parameter of a "pure" function, that means it will be released (consumed) inside the function.
	
	public static F1<Resource<Matrix<Integer>>, Resource<Matrix<Color>>> frToneMapping(F1<ColorTransform, Matrix<Color>> f_ColorTransform_MatrixColor) {
		return input -> {
			var r = input.f(mC -> {
				ColorTransform f = f_ColorTransform_MatrixColor.at(mC);
				Resource<Matrix<Integer>> rMatI = UtilsGL.matricesInt.borrow(mC.size(), true);
				rMatI.a(mI -> mI.fill((x, y) -> f.at(mC.get(x, y)).code()));
				return rMatI;
			});
			input.release(); // CONSUMING INPUT!!!
			return r;
		};
	}
	
}
