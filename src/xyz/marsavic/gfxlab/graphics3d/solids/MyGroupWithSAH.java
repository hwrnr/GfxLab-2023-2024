package xyz.marsavic.gfxlab.graphics3d.solids;

import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;


public class MyGroupWithSAH extends Solid {

    private static final int THRESHOLD_NUM = 1;
    private double THRESHOLD_SIZE = 0.01;

    private Node root;

    protected class Node {

        /**
         * kada treba da delimo, prodjemo kroz sve trenutne solide i uzmemo centre
         * centri po svakoj osi su nam potencijalne splitting planes
         * trazimo minimum SAH po njima
         * proverimo dodatno da li je novi SAH manji od SAH na prethodnom nivou, ako nije prekidamo
         *
         * na svakom nivou, proverimo tresholde, i rekurzivno delimo
         *
         * svaki solid vraca svoj bounding box
         */


        protected BoundingBox boundingBox;

        protected Solid[] solids; // solidi koji seku dati cvor
        protected Node left, right; // levo i desno dete
        protected double sah;
        protected SplittingPlane splittingPlane;
        protected HashSet<SplittingPlane> parentSplPlanes;

        public Node(BoundingBox boundingBox, Solid[] solids, HashSet<SplittingPlane> parentSplPl){
            //konstruktor - uzima prosldjene vrednosti i poziva rekurzivno pravljenje dece
            this.boundingBox = boundingBox;
            this.solids = solids;
            this.parentSplPlanes = parentSplPl;
            this.sah = Double.POSITIVE_INFINITY;
            if (solids.length > THRESHOLD_NUM && boundingBox.r().lengthSquared() > THRESHOLD_SIZE){
                split(); //napravi spliting plane i decu
            }
        }

        private void split(){
            int minAxis = 0;
            Double minPos = 0.0;
            Double minSAH = Double.POSITIVE_INFINITY;

            for(Solid solid : solids){
                Vec3 c = solid.boundingBox().c();//za svaki solid nadjemo centar
                for (int i =0; i < 3; i++){//za svaku osu, racunamo sah i trazimo minimum po njima
                    Double pos = c.get(i);
                    //ovo je da se ne ponovi dva put ista :)
                    if (parentSplPlanes.contains(SplittingPlane.pa(pos, Axis.values()[i]))) continue;
                    Double sah = computeSAH(i, pos);
                    if (sah < minSAH){
                        minAxis = i;
                        minPos = pos;
                        minSAH = sah;
                    }
                }
            }

            if (minSAH >= this.sah) return;

            this.splittingPlane = SplittingPlane.pa(minPos, Axis.values()[minAxis]);
            this.sah = minSAH;
//            System.out.println(splittingPlane);
//            System.out.println(parentSplPlanes);
//            System.out.println(minSAH + " " + sah);

            createNodes();
        }

        private Double computeSAH(int axis, Double pos) {
            BoundingBox leftbb  = BoundingBox.$.pq(boundingBox.p(), Vec3.set(axis, pos, boundingBox.q()));
            BoundingBox rightbb = BoundingBox.$.pq(Vec3.set(axis, pos, boundingBox.p()), boundingBox.q());

            int leftCount  = 0;
            int rightCount = 0;

            for (Solid solid : solids){
                if (solid.intersects(leftbb )) leftCount++;
                if (solid.intersects(rightbb)) rightCount++;
            }

            double sah = leftCount * leftbb.area() + rightCount * rightbb.area();

            return sah;
        }

        private void createNodes(){
            BoundingBox leftbb  = BoundingBox.$.pq(boundingBox.p(), Vec3.set(splittingPlane.axis().index(), splittingPlane.pos(), boundingBox.q()));
            BoundingBox rightbb = BoundingBox.$.pq(Vec3.set(splittingPlane.axis().index(), splittingPlane.pos(), boundingBox.p()), boundingBox.q());

            Solid[] leftSolids  = filterSolids(this.solids, leftbb);
            Solid[] rightSolids = filterSolids(this.solids, rightbb);

            HashSet<SplittingPlane> parentSplPlanes = (HashSet<SplittingPlane>) this.parentSplPlanes.clone();
            parentSplPlanes.add(this.splittingPlane);

            left  = new Node(leftbb, leftSolids, parentSplPlanes);
            right = new Node(rightbb, rightSolids, parentSplPlanes);
        }


        private Solid[] filterSolids(Solid[] solids, BoundingBox boundingBox) {
            ArrayList<Solid> l = new ArrayList<Solid>();
            for(Solid solid : solids){
                if (solid.intersects(boundingBox)) {
                    l.add(solid);
                }
            }

            return l.toArray(new Solid[l.size()]);
        }

        public boolean isLeaf(){
            return left == null;
        }

    }

    private MyGroupWithSAH(Solid[] solids) {
        this.boundingBox = boundingBox(solids);
        this.THRESHOLD_SIZE = boundingBox.r().lengthSquared()/16;
        this.root = new Node(boundingBox, solids, new HashSet<SplittingPlane>());
        //System.out.println(root.boundingBox);
        //ispisi(root);
    }

    private void ispisi(Node root) {
        if (root == null) return;
        System.out.println(root.boundingBox);
        ispisi(root.left);
        ispisi(root.right);
    }

    private BoundingBox boundingBox(Solid[] solids) {
        //kreira veliki bounding box svih solida

        if (solids.length == 0) return null;

        Vec3 min = solids[0].boundingBox().p();
        Vec3 max = solids[0].boundingBox().q();

        for(Solid solid : solids){
            Vec3 p = solid.boundingBox().p();
            Vec3 q = solid.boundingBox().q();

            min = Vec3.min(min, p);
            max = Vec3.max(max, q);
        }

        return BoundingBox.$.pq(min, max);
    }

    public static MyGroupWithSAH of(Solid... solids) {
        return new MyGroupWithSAH(solids);
    }

    public static MyGroupWithSAH of(Collection<Solid> solids) {
        return new MyGroupWithSAH(solids.toArray(Solid[]::new));
    }

    /**
     * nadjemo entry i exit pozicije za pocetni bb -> hits s bb
     * ako nema hita -- vratimo hit u beskonacnosti
     * ako ima, klasifikujemo decu trenutnog cvora na near i far
     * u zavisnosti od slucaja, obilazimo samo near, samo far ili near pa far rekurzivno
     * kad stignemo do lista, proverimo je l ima hita medju njegovim solidima
     *
     * napomena: moze da se desi da nadjemo hit s solidom van trenutnog noda
     * u tom slucaju ne vracamo odmah, nego nastavljamo pretragu, jer mozda ima neki blizi
     *
     * u nekom momentu mozda implementirati neki cache za ovo, da se ne racuna hit za isti objekat vise puta
     */

    @Override
    public Hit firstHit(Ray ray, double afterTime, double tFrame) {

        double entryTime = this.boundingBox.firstHit(ray, afterTime, tFrame);
        if (Double.isNaN(entryTime)) return Hit.AtInfinity.axisAlignedGoingIn(ray.d());

        double exitTime = this.boundingBox.firstHit(ray, entryTime, tFrame);
        if (Double.isNaN(exitTime)) {
            exitTime = entryTime;
            entryTime = 0;
        }

        HashMap<Solid, Hit> cache = new HashMap<Solid, Hit>();

        return findHit(root, ray, afterTime, entryTime, exitTime, tFrame);
    }

    private Hit findHit(Node node, Ray ray, double afterTime, double entryTime, double exitTime, double tFrame){

        //ako je leaf, provera hitova
        if(node.isLeaf()){
            Hit minHit = Nothing.INSTANCE.firstHit(ray, afterTime, tFrame);
            double minT = minHit.t();

            for (Solid solid : node.solids) {
                Hit hit = solid.firstHit(ray, afterTime, tFrame);

                double t = hit.t();
                if (t >= entryTime && t <= exitTime && t < minT) {
                    minT = t;
                    minHit = hit;
                }
            }

            return minHit;
        }

//        Hit hit1 = findHit(node.left, ray, afterTime, entryTime, exitTime);
//        Hit hit2 = findHit(node.right, ray, afterTime, entryTime, exitTime);
//
//        return hit1.t() < hit2.t() ? hit1 : hit2;

        // klasifikuj near i far
        int a = node.splittingPlane.axis().index();
        Node near, far;

        if(node.splittingPlane.pos() > ray.p().get(a)) { near = node.left; far  = node.right; }
        else                                     { near = node.right; far  = node.left; }

        // u zavisnosti od slucaja ispitaj samo near, samo far ili near pa far rekurzivno
        double t = (node.splittingPlane.pos() - ray.p().get(a)) / ray.d().get(a);

        if(t > exitTime || t < 0) { return findHit(near, ray, afterTime, entryTime, exitTime, tFrame); }
        else if(t <= entryTime)   { return findHit(far , ray, afterTime, entryTime, exitTime, tFrame); }
        else{
            Hit hit = findHit(near, ray, afterTime, entryTime, t, tFrame);
            if(hit.getClass() != Hit.AtInfinity.class) return hit; //nzm je l ovo moze ovako da se proveri
            return findHit(far, ray, afterTime, t, exitTime, tFrame);
        }
    }

    //@Override
    public boolean intersects(BoundingBox boundingBox) {
        return true;
    }

    @Override
    public boolean hitBetween(Ray ray, double afterTime, double beforeTime, double tFrame) {
        return this.firstHit(ray, afterTime, tFrame).t() < beforeTime;
    }

}
