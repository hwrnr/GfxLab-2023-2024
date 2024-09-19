package xyz.marsavic.gfxlab.graphics3d.solids;

import javafx.util.Pair;
import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vec;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.graphics3d.Hit;
import xyz.marsavic.gfxlab.graphics3d.Material;
import xyz.marsavic.gfxlab.graphics3d.Ray;
import xyz.marsavic.gfxlab.graphics3d.Solid;
import xyz.marsavic.gfxlab.helper.Vec3Comparator;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

class Triangle {
    Vec3 v0, v1, v2, n;
    double area;

    public Triangle(Vec3 v0, Vec3 v1, Vec3 v2, Vec3 n) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.n = n;
        this.area = area(v0, v1, v2);
    }

    public double area() {
        return this.area;
    }

    public static double area(Vec3 v0, Vec3 v1, Vec3 v2) {
        return v1.sub(v0).cross(v2.sub(v0)).length() / 2;
    }
}

public class STLSolid implements Solid {

    static final String STL_FILE = "/object.stl";
    static final String LIGHT_FILE = "/light";
    static final String TRANSPARENCY_FILE = "/providnost.png";
    static final String ANIMATION_DELAY_FILE = "/delay";
    static final String TEXTURE_FILE = "/tekstura.png";

    private List<Pair<Triangle, Vec3>> triangleList = new LinkedList<>();

    private final Box boundingBox;

    private final F1<Material, Vector> mapMaterial;

    private double[][] transparency; // Bolje ime bi bilo opacity
    private Color[][] texture;

    private List<Vec3> temena = new ArrayList<>(4);

    private boolean isAnimated = false;
    private double delay = 0.0;
    private double height = 0.0;

    public STLSolid(String folderPath) {
        String filePath = folderPath + STL_FILE;

//        Material material = Material.matte(Color.hsb(180, 0.6, 0.8));
        Material material = Material.matte(Color.rgb(1, 0, 0));

        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;

            br.readLine(); // First line is not important
            // Read line by line until the end of the file is reached
            while ((line = br.readLine()) != null && line.startsWith("facet")) {
                // Process the line (e.g., print it, store it in a data structure)
                String[] chunks = line.split(" ");
                Vec3 n = new Vec3(
                        Float.parseFloat(chunks[2]),
                        Float.parseFloat(chunks[3]),
                        Float.parseFloat(chunks[4]));
                Vec3[] points = new Vec3[3];
                br.readLine();
                for (int i = 0; i < 3; ++i) {
                    line = br.readLine();
                    chunks = line.split(" ");
                    points[i] = new Vec3(Float.parseFloat(chunks[1]), Float.parseFloat(chunks[2]), Float.parseFloat(chunks[3]));
                }
                br.readLine();
                br.readLine();
                Triangle triangle = new Triangle(points[0], points[1], points[2], n);
                Vec3 cooef = (triangle.v0.sub(triangle.v1).cross(triangle.v0.sub(triangle.v2))).normalized_();
                triangleList.add(new Pair<>(triangle, cooef));

                for (Vec3 vec : points) {
                    minX = Math.min(minX, vec.x());
                    minY = Math.min(minY, vec.y());
                    minZ = Math.min(minZ, vec.z());
                    maxX = Math.max(maxX, vec.x());
                    maxY = Math.max(maxY, vec.y());
                    maxZ = Math.max(maxZ, vec.z());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        height = maxZ - minZ;
        boundingBox = Box.$.pq(Vec3.xyz(minX, minY, minZ), Vec3.xyz(maxX, maxY, maxZ));

        System.out.println(Vec3.xyz(minX, minY, minZ));
        System.out.println(Vec3.xyz(maxX, maxY, maxZ));

        try (BufferedReader br = new BufferedReader(new FileReader(folderPath + LIGHT_FILE))) {
            String l = br.readLine();
            if (!Objects.equals(l, "0")) {
                double light = Double.parseDouble(l);
                material = material.add(Material.light(Color.rgb(light, 0, 0)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(folderPath + ANIMATION_DELAY_FILE))) {
            String l = br.readLine();
            delay = Double.parseDouble(l);
            isAnimated = true;
        } catch (IOException ignored) {
//            throw new RuntimeException(e);
        }

        try {
            BufferedImage img = javax.imageio.ImageIO.read(new File(folderPath + TRANSPARENCY_FILE));

            transparency = new double[img.getWidth()][img.getHeight()];
            for (int i = 0; i < img.getWidth(); ++i) {
                for (int j = 0; j < img.getHeight(); ++j) {
                    java.awt.Color c = new java.awt.Color(img.getRGB(i, j));
                    transparency[i][j] = c.getRed() / 255.0; // Trebalo bi da je gray scale
                }
            }
        } catch (IOException ignored) { // Glavni fajl još nema providnost
//            throw new RuntimeException(e);
            transparency = new double[1][1];
            transparency[0][0] = 1;
        }

        try {
            BufferedImage img = javax.imageio.ImageIO.read(new File(folderPath + TEXTURE_FILE));

            texture = new Color[img.getWidth()][img.getHeight()];
            for (int i = 0; i < img.getWidth(); ++i) {
                for (int j = 0; j < img.getHeight(); ++j) {
                    java.awt.Color c = new java.awt.Color(img.getRGB(i, j));
                    Color color = Color.rgb(c.getRed(), c.getGreen(), c.getBlue());
                    texture[i][j] = color;
                }
            }
        } catch (IOException ignored) { // Glavni fajl još nema teksturu
//            throw new RuntimeException(e);
            texture = new Color[1][1];
            texture[0][0] = Color.BLACK;
        }

        this.mapMaterial = material;
//        System.out.println(material);

        if (triangleList.size() == 2) {
            // Ovo sigurno može pametnije... Al' nije strašno, pokreće se samo jednom
            Vec3[] koordinateCetvorougla = new Vec3[6];
            koordinateCetvorougla[0] = triangleList.getFirst().getKey().v0;
            koordinateCetvorougla[1] = triangleList.getFirst().getKey().v1;
            koordinateCetvorougla[2] = triangleList.getFirst().getKey().v2;

            koordinateCetvorougla[3] = triangleList.get(1).getKey().v0;
            koordinateCetvorougla[4] = triangleList.get(1).getKey().v1;
            koordinateCetvorougla[5] = triangleList.get(1).getKey().v2;

            Vec3Comparator cmp = new Vec3Comparator();

            Arrays.sort(koordinateCetvorougla, cmp);

            Vec3[] duplikati = new Vec3[2];
            Vec3[] neDuplikati = new Vec3[2];

            int j = 0, k = 0;

            for (int i = 0; i < koordinateCetvorougla.length; ++i) {
                if (cmp.compare(koordinateCetvorougla[i], koordinateCetvorougla[ (i+1) % koordinateCetvorougla.length]) == 0) {
                    duplikati[j++] = koordinateCetvorougla[i];
                    i++;
                }
                else {
                    neDuplikati[k++] = koordinateCetvorougla[i];
                }
            }

            temena.add(duplikati[0]);
            temena.add(neDuplikati[0]);
            temena.add(duplikati[1]);
            temena.add(neDuplikati[1]);
        }
    }

    public static Solid group(String dirPath) {
        Collection<Solid> stlSolids = new LinkedList<>();
        File f = new File(dirPath);
        File[] dirs = f.listFiles();
        if (dirs == null) {
            return null;
        }
        for (File dir : dirs) {
            if (dir.isDirectory()) {
                stlSolids.add(new STLSolid(dir.getPath()));
            }
        }
        return Group.of(stlSolids);
    }

    @Override
    public Hit firstHit(Ray originalRay, double afterTime, double t) {
        Ray ray;
        if (isAnimated) {
            double newT = t - delay;
            // TODO: newT should be sin or something
            if (newT < 0) {
                newT += 1;
            }
            ray = Ray.pd(originalRay.p().sub(Vec3.EZ.mul(height).mul(newT)), originalRay.d());
        }
        else {
            ray = originalRay;
        }
        Hit bestHit = Hit.AtInfinity.axisAlignedGoingIn(ray.d());

        if (false) {
            return boundingBox.firstHit(ray, afterTime, t);
        }
        if (boundingBox.firstHit(ray, afterTime, t).getClass() == Hit.AtInfinity.class) {
            return bestHit;
        }

        for (Pair<Triangle, Vec3> pair: this.triangleList) {
            Triangle triangle = pair.getKey();
            Vec3 cooef = pair.getValue();
            double d = -(triangle.v2.dot(cooef));
            double k = - (ray.p().dot(cooef) + d ) / (ray.d().dot(cooef));

            if (k < 0.0) {
                // Udarac se desio iza kamere
                continue;
            }

            Vec3 tackaUdara = ray.at(k);
            double area1 = triangle.area();
            double area2 = Triangle.area(tackaUdara, triangle.v1, triangle.v2) + Triangle.area(triangle.v0, tackaUdara, triangle.v2) + Triangle.area(triangle.v0, triangle.v1, tackaUdara);

            if (Double.isNaN(area1 - area2)) {
                continue;
            }

            if (Math.abs(area1 - area2) > 0.001) {
                continue;
            }
            if (bestHit.t() > k && k > afterTime) {

                if (!temena.isEmpty()) {

                    double random = Math.random();

                    Vec3 v1 = temena.get(1).sub(temena.get(0));
                    Vec3 v2 = temena.get(3).sub(temena.get(0));
                    Vec3 v3 = tackaUdara.sub(temena.get(0));

                    double a = (v3.x() - v3.y() * v2.x() / v2.y()) / (v1.x() - v1.y() * v2.x() / v2.y());
                    double b = (v3.x() - a * v1.x()) / v2.x();

                    // a i b su ili u rasponu [0, 1] ili Infinity, NaN, ...

                    if (!Double.isFinite(a) || !Double.isFinite(b)) {
                        continue;
                    }

                    int x = (int) (a * transparency.length);
                    int y = (int) (b * transparency[0].length);

                    if (x >= transparency.length) x = transparency.length - 1;
                    if (y >= transparency[0].length) y = transparency[0].length - 1;

                    if (x < 0) x = 0;
                    if (y < 0) y = 0;

                    if (random < transparency[x][y]) {
                        x = (int) (a * texture.length);
                        y = (int) (b * texture[0].length);

                        if (x >= texture.length) x = texture.length - 1;
                        if (y >= texture[0].length) y = texture[0].length - 1;

                        if (x < 0) x = 0;
                        if (y < 0) y = 0;

                        bestHit = new HitStlSolid(k, cooef, Vector.xy(x, y));
                    }
                }
                else {
                    bestHit = new HitStlSolid(k, cooef, Vector.ZERO);
                }
            }
        }

        return bestHit;
    }


    final class HitStlSolid implements Hit {
        private final double t;
        private final Vec3 n_;
        private final Vector uv;

        HitStlSolid(double t, Vec3 n_, Vector uv) {
            this.t = t;
            this.n_ = n_;
            this.uv = uv;
        }

        @Override
        public double t() {
            return t;
        }

        @Override
        public Vec3 n() {
            return n_;
        }

        @Override
        public Material material() {
//            if (true) return STLSolid.this.mapMaterial.at(uv());
            Vector uv = uv();

            int x = uv.xInt();
            int y = uv.yInt();;

            Material m = //Material.light(1).add(
                    Material.light(
                STLSolid.this.texture[x][y]
            );
            return m;
        }

        @Override
        public Vector uv() {
            return this.uv;
        }
    }
}



class Program {
    public static void main(String[] args) {
        STLSolid stlSolid = new STLSolid("/home/hawerner/faks/master1/grafika/STL_files/wood");
//        System.out.println(stlSolid.firstHit(new Ray(new Vec3(0, 0, 0), new Vec3(0, 0, 1))));
    }
}
