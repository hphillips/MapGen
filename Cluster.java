package MapGen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public interface Cluster{
	double distance(Point p);
	double distance(Cluster c);
	public boolean equals(Object o);
	public int getId();
	public double[] getCenter();
	public List<Point> getElements();
	public double getTotalError();
	public double getErrorFrom(double x, double y);
	public int getSize();
	public void remove(Cluster c);
	public ClusterDistance getClosest();
	public boolean contains(Cluster c);
	public FinalCluster finish();
	
	public static MergeCluster merge(Cluster a, Cluster b, int id){
		return new MergeCluster(a,b,id);
	}
	
	class PointCluster implements Cluster{
		private int id;
		private double x;
		private double y;
		private List<Point> cluster_elements = new ArrayList<Point>();
		private SortedSet<ClusterDistance> distances = new TreeSet<ClusterDistance>();
		double error = 0;
		
		public PointCluster(Point p,int id){
			this.id=id;
			this.x=p.x;
			this.y=p.y;
			cluster_elements.add(p);
		}
				
		public int getId(){
			return id;
		}
		
		public PointCluster(double x,double y){
			this.x=x; this.y=y;
		}
		
		public void addPoint(Point p){
			cluster_elements.add(p);
			error+=Math.sqrt(Math.pow(p.getX()-this.x, 2)+Math.pow(p.getY()-this.y, 2));
		}
		
		public double distance(Point p){
			return Math.sqrt(Math.pow((this.x-p.getX()),2)+Math.pow((this.y-p.getY()),2));
		}
		
		public PointCluster refresh(){
			double sum_x=0;
			double sum_y=0;
			for(Point p:cluster_elements){
				sum_x+=p.x;
				sum_y+=p.y;
			}
			sum_x/=cluster_elements.size();
			sum_y/=cluster_elements.size();
			return new PointCluster(sum_x,sum_y);
		}
				
		public double distance(Cluster c){
			if(c.getId()>this.getId())return c.distance(this);
			else {
				PointCluster p = (PointCluster)c;
				double min_d=Double.MAX_VALUE;
				for(Point i:this.cluster_elements){
					for(Point j:p.cluster_elements){
						min_d=Math.min(min_d, (Math.pow(i.getX()-j.getX(),2))+Math.pow(i.getY()-j.getY(),2));
					}
				}
				min_d = Math.sqrt(min_d);
				distances.add(new ClusterDistance(c,min_d));
				return min_d;
			}
		}
		
		public void remove(Cluster c){
			if(this.id<c.getId())return;
			Iterator<ClusterDistance> i = distances.iterator();
			while(i.hasNext()){
				ClusterDistance cd=i.next();
				if(cd.c.getId()==c.getId()){
					i.remove();
					return;
				}
			}
		}
			
		public boolean equals(Object o){
			return ((Cluster)o).getId()==this.getId();
		}
		
		public String toString(){return this.getId()+"";}

		public List<Point> getElements() {
			return cluster_elements;
		}

		public double[] getCenter() {
			return new double[]{x,y};
		}

		public double getTotalError() {
			return this.error;
		}

		public int getSize() {
			return cluster_elements.size();
		}

		public double getErrorFrom(double x, double y) {
			double ret = 0;
			for(Point p:cluster_elements){
				ret+=Math.sqrt(Math.pow((x-p.getX()),2)+Math.pow((y-p.getY()),2));
			}
			return ret;
		}

		public ClusterDistance getClosest() {
			return distances.first();
		}

		public boolean contains(Cluster c) {
			for(ClusterDistance cd:distances){
				if(cd.c==c) return true;
			}
			return false;			
		}

		public FinalCluster finish() {
			return new FinalCluster(cluster_elements);
		}
	}
	
	class MergeCluster implements Cluster{

		private Cluster[] subs;
		private int id;
		private SortedSet<ClusterDistance> distances = new TreeSet<ClusterDistance>();
		private HashMap<Cluster,Double> distances2 = new HashMap<Cluster,Double>();
		private int size;
		private double x;
		private double y;
		private double error;
		
		public MergeCluster(Cluster a, Cluster b, int id){
			this.id=id;
			subs=new Cluster[]{a,b};
			this.size=a.getSize()+b.getSize();
			x=(a.getCenter()[0]*a.getSize()+b.getCenter()[0]*b.getSize())/(a.getSize()+b.getSize());
			y=(a.getCenter()[1]*a.getSize()+b.getCenter()[1]*b.getSize())/(a.getSize()+b.getSize());
			error = a.getErrorFrom(x, y)+b.getErrorFrom(x, y);
		}
		
		public double distance(Point p) {
			return Math.min(subs[0].distance(p), subs[1].distance(p));
		}
		
		public void calcDistances(List<Cluster> clusters){
			for(Cluster c:clusters){
				double distance=Math.min(subs[0].distance(c), subs[1].distance(c));
				distances.add(new ClusterDistance(c,distance));
				distances2.put(c,distance);
			}
		}

		public double distance(Cluster c) {
			if(this.id>c.getId()){
				return distances2.get(c);
			}else{
				return c.distance(this);
			}
		}
		
		public void remove(Cluster c){
			if(this.id<c.getId())return;
			Iterator<ClusterDistance> i = distances.iterator();
			while(i.hasNext()){
				ClusterDistance cd=i.next();
				if(cd.c.getId()==c.getId()){
					i.remove();
					return;
				}
			}
		}

		public int getId() {
			return id;
		}

		public List<Point> getElements() {
			List<Point> ret = new ArrayList<Point>(this.getSize());
			ret.addAll(subs[0].getElements());
			ret.addAll(subs[1].getElements());
			return ret;
		}

		public double[] getCenter() {
			return new double[]{x,y};
		}

		public double getTotalError() {
			return error;
		}

		public int getSize() {
			return this.size;
		}

		public double getErrorFrom(double x, double y) {
			return subs[0].getErrorFrom(x, y)+subs[1].getErrorFrom(x, y);
		}

		public ClusterDistance getClosest() {
			return distances.first();
		}
		
		public String toString(){return this.getId()+"";}

		public boolean contains(Cluster c) {
			for(ClusterDistance cd:distances){
				if(cd.c==c) return true;
			}
			return false;			
		}
		
		public List<Cluster> split(){
			ArrayList<Cluster> ret = new ArrayList<Cluster>(2);
			ret.add(subs[0]); ret.add(subs[1]);
			return ret;
		}

		public FinalCluster finish() {
			return new FinalCluster(this.getElements());
		}
	}
	
	class ClusterDistance implements Comparable<ClusterDistance>{

		Cluster c;
		double  d;
		
		private ClusterDistance(Cluster c, double d){
			this.c=c; this.d=d;
		}
		
		public int compareTo(ClusterDistance o) {
			if(this.d<o.d)return -1;
			else if(this.d>o.d) return 1;
			else return c.getId()-o.c.getId();
		}
		
	}

}