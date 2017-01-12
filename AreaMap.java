package MapGen;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import MapGen.Cluster.ClusterDistance;
import MapGen.Cluster.MergeCluster;
import MapGen.Cluster.PointCluster;
import MapGen.Path.FinishedPath;
import MapGen.Path.StarterPath;

@SuppressWarnings("serial")
public class AreaMap extends JPanel implements ActionListener{
	public static final double min_distance = 6; 
	public static final double new_conn_threshold = 1.4;
	public static final double cluster_wt=1.6;
	
	JTextField num_maps;
	JButton iter_new_cons;
	
	public Point[] points = new Point[0];
	public Path[][] distances = new Path[0][];
	public List<List<Connection>> connections = new ArrayList<List<Connection>>();
	List<StarterPath> prs = new ArrayList<StarterPath>();
	public JTextField tolerance;
	public FinalCluster[] clusters = new FinalCluster[0];
	public AreaMap(JTextField num_maps, JButton iter_new_cons,JTextField tolerance){
		this.num_maps=num_maps;
		this.iter_new_cons=iter_new_cons;
		this.tolerance=tolerance;
		this.setBackground(Color.WHITE);
		this.setPreferredSize(new Dimension(500,500));
		this.setMinimumSize(new Dimension(500,500));
	}
	public List<Polygon> areas = new ArrayList<Polygon>();
	
	public void generate(int step){
		if(step<=3){ //polygon finding cleanup
			areas = new ArrayList<Polygon>();
		}
		
		if(step<=2){ //connections cleanup
			int nodes_ct = Integer.parseInt((num_maps).getText());
			Point[] newPoints = new Point[nodes_ct];
			Path[][] new_distances = new Path[nodes_ct-1][];
			connections = new ArrayList<List<Connection>>(nodes_ct);
			prs = new ArrayList<StarterPath>();
			//Create new points
			for(int i = 0; i<nodes_ct; ){
				Point newPoint = null;
				if(step==0){
					newPoint = new Point((int)(Math.random()*100), (int)(Math.random()*100));
				}else{
					newPoint = points[i];
				}
				
				//for each point after the first, calculate the direct distance between it and every other point
				if(i>0){
					Path[] cur_connections = new Path[i];
					double min_d=Double.MAX_VALUE;
					for(int j = 0; j<i; j++){
						double distance = Point.distance(newPoint.getX(), newPoint.getY(), newPoints[j].getX(), newPoints[j].getY());
						min_d = Math.min(min_d,distance);
						StarterPath new_pr = new StarterPath(i,j,distance);
						cur_connections[j]=new_pr;
					}
					if(min_d<=AreaMap.min_distance) continue;
					new_distances[i-1]=cur_connections;
					
					for(Path pr:cur_connections) prs.add((StarterPath)pr);
				}
				newPoints[i]=newPoint;
				connections.add(new ArrayList<Connection>());
				i++;
			}
			distances=new_distances;
			
			prs.sort(null);
			points=newPoints;
		}
		
		if(step<=1){//cleanup for clustering
			clusters = new FinalCluster[0];
		}
	}
	
	public int[] randomInts(int max,int ct){
		int[] rets = new int[ct];
		for(int i=0; i<ct;){
			int cur=(int)(Math.random()*max);
			for(int j=0; j<i; j++){
				if(rets[j]==cur)cur=-1;
			}
			if(cur==-1)continue;
			rets[i++]=cur;
		}
		return rets;
	}
	
	public void bottomsUpCluster(){
		//Start with each point as a cluster
		int cluster_ct=points.length;
		ArrayList<Cluster> clusters= new ArrayList<Cluster>(cluster_ct);
		for(int i=0; i<cluster_ct;i++){
			clusters.add(new PointCluster(points[i],i));
		}
				
		for(int i=1;i<clusters.size(); i++){
			for(int j=0; j<i; j++){
				clusters.get(i).distance(clusters.get(j));
			}
		}
		
		/*Create indexing values to determine the best clustering*/
		double x_ave = 0;
		double y_ave = 0;
		for(int i=0; i<points.length; i++){
			x_ave+=points[i].getX();
			y_ave+=points[i].getY();
		}
		x_ave/=points.length;
		y_ave/=points.length;
		double cur_error = 0;
		for(int i=0; i<points.length; i++){
			cur_error+=Math.sqrt(Math.pow(x_ave-points[i].getX(),2)+Math.pow(y_ave-points[i].getY(),2));
		}
		
		double max_error=cur_error;
		double max_clusters=points.length;
		cur_error=0;
		
		/*
		 * Remove the closest pair, 
		 * Update the remaining clusters
		 * Find the closest cluster to the newly created cluster		 * 
		 */
		int cur_id=clusters.size();
		int min_ct = clusters.size();
		double min_heuristic = 1;
		while(clusters.size()>2){
			Cluster a = null;
			Cluster b = null;
			Double min_dist = Double.MAX_VALUE;
			for(int i=1; i<clusters.size(); i++){ //Position Zero should have no distances kept
				Cluster cur = clusters.get(i);
				ClusterDistance cd = cur.getClosest();
				Cluster closest=cd.c;
				double dist = cd.d;
				if(dist<min_dist){
					a=cur;
					b=closest;
					min_dist=dist;
				}
			}
			MergeCluster replacement = Cluster.merge(a,b,cur_id++);
			
			Iterator<Cluster> i = clusters.iterator();
			while(i.hasNext()){
				Cluster c = i.next();
				if(a==c || b==c){
					i.remove();
				}
			}
			replacement.calcDistances(clusters);
			for(Cluster c:clusters){
				c.remove(a);c.remove(b);
			}
			clusters.add(replacement);
			cur_error+=replacement.getTotalError()-a.getTotalError()-b.getTotalError();
			double cur_heuristic = (cur_error/max_error+cluster_wt*clusters.size()/max_clusters)/(1+cluster_wt);
			if(cur_heuristic<min_heuristic){
				min_ct=clusters.size();
				min_heuristic = cur_heuristic;
			}
			
		}
		while(clusters.size()<min_ct){
			int to_split=0;
			for(int i=1; i<clusters.size(); i++){
				if(clusters.get(i).getId()>clusters.get(to_split).getId()){
					to_split=i;
				}
			}
			MergeCluster removed = (MergeCluster)clusters.remove(to_split);
			clusters.addAll(removed.split());			
		}
		FinalCluster[] final_clusters = new FinalCluster[clusters.size()];
		for(int i=0; i<clusters.size();i++){
			final_clusters[i]=clusters.get(i).finish();			
		}
		this.clusters=final_clusters;
		System.out.println(min_ct+" "+min_heuristic);
	}
			
	public void findPolygons(int[] indices){
		List<List<Connection>> poly_conns = new ArrayList<List<Connection>>(connections.size());
		for(List<Connection> conns:connections){
			conns.sort(null);
			poly_conns.add(new ArrayList<Connection>(conns)); 
		}
		
		//Remove any intersecting lines
		SortedSet<Connection> to_remove = new TreeSet<Connection>();
		for(int start_point=0; start_point<poly_conns.size()-1; start_point++){ //Don't need to iterate the connections for the last point
			for(Connection cur:poly_conns.get(start_point)){
				for(int i=start_point+1; i<poly_conns.size(); i++){
					for(Connection other:poly_conns.get(i)){
						if(cur.intersects(other)){
							to_remove.add(cur); to_remove.add(other);
						}
					}
				}
			}
		}
		////System.out.println("To Remove: "+to_remove.toString());
		for(Connection conn:to_remove){
			poly_conns.get(conn.start_point).remove(conn);
		}
		
		//Branch Pruning: Remove any nodes (and edges of those nodes) if that node has only 1 connection. Done recursively
		boolean to_search = true;
		int removed=0;
		while(to_search){
			to_search=false; //set to true if we find a node to remove
			for(int i=0; i<poly_conns.size();){
				if(poly_conns.get(i).size()==1){
					to_search=true;
					for(Connection conn:poly_conns.get(i)){ //remove each of these (if zero or 1)
						List<Connection> other = poly_conns.get(conn.end_point-removed);
						for(int j=0; j<other.size();){
							if(other.get(j).end_point==conn.start_point){
								other.remove(j);
							}else{j++;}
						}
					}
					poly_conns.get(i).remove(0);
				}else{i++;}
			}
		}
		
		//Find outer edge
		int top_point=-1;
		double top_y = Double.MAX_VALUE;
		for(int i=0;i<points.length;i++){
			if(points[i].getY()<top_y && poly_conns.get(i).size()!=0){
				top_y=points[i].getY();
				top_point=i;
			}
		}		

		boolean outer=true;
		int start_point=top_point;
		while(start_point<points.length){
			//System.out.println(start_point+" "+poly_conns.toString());
			if(poly_conns.get(start_point).size()==0){
				start_point++;
			}else{
				List<Integer> nodes = new ArrayList<Integer>();
				nodes.add(start_point);
				int cur_point=start_point;
				int next_point=poly_conns.get(cur_point).get(0).end_point;
				double angle = poly_conns.get(cur_point).get(0).angle;
				poly_conns.get(cur_point).remove(0);
				while(next_point!=start_point){
					int last_point=cur_point;
					cur_point=next_point;
					nodes.add(cur_point);
					angle=angle+(angle<180.0?180.0:-180.0);
					//Rotate around to find next
					int i=0;
					List<Connection> cur_conns=poly_conns.get(cur_point);
					while(i<cur_conns.size() && (cur_conns.get(i).angle<angle || cur_conns.get(i).end_point==last_point))i++;
					i%=cur_conns.size();
					next_point=cur_conns.get(i).end_point;
					angle=cur_conns.get(i).angle;
					cur_conns.remove(i);
				}
				//System.out.println("Nodes: "+nodes);
				//System.out.println("To Remove: "+to_remove);
				if(!outer){
					Polygon edge=new Polygon();
					for(int i:nodes)edge.addPoint(points[i].x, points[i].y);
					areas.add(edge);
				}
				outer=false;
				start_point=0;
			}
		}
	}
	
	public void connectPoints(int[][] indices){
		double tolerance = Double.parseDouble(this.tolerance.getText());
		for(int i = 0; i<prs.size(); i++){
			StarterPath p = prs.get(i);
			int start_point=p.point1;
			int end_point=p.point2;
			Crawler searcher = new Crawler(start_point,end_point);
			searcher.crawl();
			while(true){
				if(searcher.distance()>p.direct_distance*tolerance){
					setPath(p.point1,p.point2,p.finalizePath(p.direct_distance, end_point));
					connections.get(start_point).add(new Connection(start_point,end_point));
					connections.get(end_point).add(new Connection(end_point,start_point));
					break;
				}else if(searcher.status()==0){//Is there no path?
					setPath(p.point1,p.point2,p.finalizePath(p.direct_distance, end_point));
					connections.get(start_point).add(new Connection(start_point,end_point));
					connections.get(end_point).add(new Connection(end_point,start_point));
					break;
				}else if(searcher.status()==-2){//Is the existing path fine?
					setPath(start_point,end_point,searcher.convert());
					break;
				}else{
					searcher.crawl();
				}
			}
		}
	}
	
	public void actionPerformed(ActionEvent arg0) {
		long millis = System.currentTimeMillis();
		
		if(((JButton)arg0.getSource()).getText()=="New Points"){
			generate(0);
		}else if(((JButton)arg0.getSource()).getText()=="Cluster"){
			generate(1);
			bottomsUpCluster();
		}else if(((JButton)arg0.getSource()).getText()=="Draw Connections"){
			generate(2);
			connectPoints(null);
		}else if(((JButton)arg0.getSource()).getText()=="Fill Polygons"){
			generate(3);
			findPolygons(null);
		}
		this.repaint();
		System.out.println(System.currentTimeMillis()-millis);
	}
	
	public class Crawler implements Comparable<Crawler>{

		public int start_point;
		public int end_point;
		public ArrayList<Spider> crawlers = null;
		public SortedSet<Integer> traveled_points = new TreeSet<Integer>();
		
		public Crawler(int start_point, int end_point){
			this.start_point=start_point; this.end_point=end_point;
			traveled_points.add(start_point);
		}

		public double distance(){
			if(!isStarted() || crawlers.size()==0) return getRelation(start_point, end_point).distance();
			return crawlers.get(0).distance();
		}
		
		public boolean isStarted(){
			return crawlers!=null;
		}
		
		public int status(){
			if(!isStarted())return -1;
			else if(crawlers.size()>0 && crawlers.get(0).isDistanceActual())return -2;
			else return crawlers.size();
		}
		
		public String toString(){
			return crawlers.size()+" "+start_point+"->"+(crawlers.size()>0?(crawlers.get(0).cur_point+""):"")+"->"+end_point+" "+this.distance();
		}
		
		public FinishedPath convert(){
			return crawlers.get(0).convert();
		}
				
		//Initiates the crawler if not started
		//Else it 
		//	removes the first elements
		//	adds to traveled points, 
		//	removes other instances of that point
		//	crawls that point and adds its children
		public void crawl(){
			//intialize if not started
			if(!isStarted()){
				crawlers = new ArrayList<Spider>(6);
				for(Connection conn:connections.get(start_point)){
					crawlers.add(new Spider(start_point, end_point,conn.end_point,
							conn.end_point,
							getRelation(start_point,conn.end_point).distance()));
				}
				crawlers.sort(null);
			}else{
				Spider first = crawlers.remove(0);
				traveled_points.add(first.cur_point);
				
				crawlers.addAll(first.crawl());
				crawlers.sort(null);
				
			}
		}
		
		public int compareTo(Crawler o) {
			if(this.distance()>o.distance())return 1;
			else if(this.distance()<o.distance()) return -1;
			else return 0;
		}
		
		public class Spider implements Comparable<Spider>{
			
			public int start_point;
			public int end_point;
			public int first_point;
			public int cur_point;
			public double dist_so_far;
			
			public String toString(){
				return cur_point+" "+this.distance();
			}
			
			public List<Spider> crawl() {
				List<Spider> ret = new ArrayList<Spider>();
				for(Connection conn:connections.get(cur_point)){
					if(!traveled_points.contains(conn.end_point)){
						ret.add(new Spider(start_point, end_point,first_point,
								conn.end_point,
								dist_so_far+getRelation(cur_point,conn.end_point).distance()));
					}
				}
				return ret;
			}

			public Spider(int start_point, int end_point, int first_point, int cur_point, double dist_so_far){
				this.start_point=start_point;
				this.end_point=end_point;
				this.first_point=first_point;
				this.cur_point=cur_point;
				this.dist_so_far=dist_so_far;
			}

			public double distance() {
				return dist_so_far + getRelation(cur_point,end_point).distance();
			}
			
			public FinishedPath convert(){
				return new FinishedPath(start_point,end_point,getRelation(start_point,end_point).distance(),this.distance(),first_point);
			}

			public boolean isDistanceActual() {
				return getRelation(cur_point,end_point).isDistanceActual();
			}

			public int compareTo(Spider arg0) {
				if(this.distance()<arg0.distance()) return -1;
				else if(this.distance()>arg0.distance()) return 1;
				else return 0;
			}			
		}
	}
			
	

	public class Connection implements Comparable<Connection>{
		public int start_point;
		public int end_point;
		public double angle;
		
		Connection(int start_point, int end_point){
			this.start_point=start_point;
			this.end_point=end_point;
			angle = 180.0/Math.PI*Math.atan2(points[end_point].getY()-points[start_point].getY(),points[end_point].getX()-points[start_point].getX());
			if(angle<0.0)angle+=360.0;
			
		}
		
		public int compareTo(Connection other) {
			if(this.angle>other.angle)return 1;
			else if(this.angle<other.angle) return -1;
			else if(this.left()<other.left())return -1;
			else if(this.left()>other.left())return 1;
			else if(this.right()<other.right())return -1;
			else if(this.right()>other.right())return 1;
			else if(this.top()<other.top())return -1;
			else if(this.top()>other.top())return 1;
			else if(this.bottom()<other.bottom())return -1;
			else if(this.bottom()>other.top())return 1;
			return 0;
			
		}
	
		public double left(){
			return Math.min(points[start_point].getX(), points[end_point].getX());
		}
		public double right(){
			return Math.max(points[start_point].getX(), points[end_point].getX());
		}
		public double top(){
			return Math.min(points[start_point].getY(), points[end_point].getY());
		}
		public double bottom(){
			return Math.max(points[start_point].getY(), points[end_point].getY());
		}
		
		public double slope(){
			return (points[end_point].getY()-points[start_point].getY())/(points[end_point].getX()-points[start_point].getX());
		}
		
		public double intercept(){
			return points[end_point].getY()-this.slope()*points[end_point].getX();
		}
		
		public boolean intersects(Connection other){
			//If they share a endpoint, they can't intersect
			if(this.start_point==other.start_point || this.start_point==other.end_point || this.end_point==other.start_point || this.end_point==other.end_point) return false;
			//If they boxes bounding them don't intersect, they can't intersect
			if(this.right() < other.left() || this.left() > other.right() || this.top() > other.bottom() || this.bottom() < this.top()) return false;
			
			//Do the math
			double intercept_x = (other.intercept()-this.intercept())/(this.slope()-other.slope());
			return this.left()<intercept_x && intercept_x<this.right() && other.left()<intercept_x && intercept_x<other.right();
		}
		
		public String toString(){
			return start_point+" "+end_point+" "+(int)angle;
		}
				
	}

	public static void main(String[] args){
		JFrame jf = new JFrame();
		jf.setLayout(new BorderLayout());
				
		JPanel north = new JPanel();
		north.setLayout(new GridLayout(6,1));
		
		JTextField jtf = new JTextField("25");
		JTextField tolerance = new JTextField(""+new_conn_threshold);
		north.add(jtf);
		north.add(tolerance);
		
		JButton new_points = new JButton("New Points");
		north.add(new_points);
		
		JButton cluster = new JButton("Cluster");
		north.add(cluster);
		
		JButton draw_connections = new JButton("Draw Connections");
		north.add(draw_connections);
		
		JButton fill_polygons = new JButton("Fill Polygons");
		north.add(fill_polygons);
		
		AreaMap am2 = new AreaMap(jtf,new_points,tolerance);
		jf.add(am2, BorderLayout.CENTER);
		jf.add(north,BorderLayout.NORTH);
		
		cluster.addActionListener(am2);
		fill_polygons.addActionListener(am2);
		draw_connections.addActionListener(am2);
		new_points.addActionListener(am2);
		jf.setVisible(true);
		jf.setMinimumSize(new Dimension(500,500));
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void paintComponent(Graphics g){
		super.paintComponent(g);
		int sideLen = Math.min(this.getSize().width, this.getSize().height)-10;
		
		int minX = (this.getSize().width-sideLen)/2;
		int minY = (this.getSize().height-sideLen)/2;
		
		g.drawRect(minX, minY, sideLen, sideLen);
		for(Polygon p:areas){
			int[] xpoints=Arrays.copyOf(p.xpoints,p.npoints);
			int[] ypoints=Arrays.copyOf(p.ypoints,p.npoints);
			for(int i=0; i<xpoints.length; i++){
				xpoints[i]=(int)(sideLen*xpoints[i]/100.0+minX);
				ypoints[i]=(int)(sideLen*ypoints[i]/100.0+minY);
				//System.out.println(Arrays.toString(xpoints));
				//System.out.println(Arrays.toString(ypoints));
			}
			g.setColor(new Color((float)Math.random(),(float)Math.random(),(float)Math.random()));
			
			g.fillPolygon(xpoints,ypoints,xpoints.length);
		}
		
		if(clusters.length==0){
			for(Point p: points){
				g.setColor(Color.BLACK);
				int drawX =(int) (sideLen*p.x/100.0-3+minX);
				int drawY = (int)(sideLen*p.y/100.0-3+minY);
				g.fillOval(drawX, drawY, 7, 7);
				//g.drawString(""+pt_ct++, drawX+9, drawY+16);
			}
		}else{
			int cluster_ct=0;
			for(FinalCluster c:clusters){
				for(Point p:c.getPoints()){
					g.setColor(Color.BLACK);
					int drawX =(int) (sideLen*p.x/100.0-3+minX);
					int drawY = (int)(sideLen*p.y/100.0-3+minY);
					g.fillOval(drawX, drawY, 7, 7);
					g.drawString(""+cluster_ct, drawX+9, drawY+16);
				}
				cluster_ct++;
			}
		}
		
		for(List<Connection> conns:connections){
			for(Connection conn:conns){
				if(conn.start_point<conn.end_point){
					g.drawLine((int)(sideLen*points[conn.start_point].x/100.0)+minX, (int)(sideLen*points[conn.start_point].y/100.0)+minY, (int)(sideLen*points[conn.end_point].x/100.0)+minX, (int)(sideLen*points[conn.end_point].y/100.0)+minY);
					//g.drawString((int)(10.0*getRelation(i,j).distance())/10.0+"", (int)((sideLen)*(points[i].x+points[j].x)/200.0+minX)+8, (int)((sideLen)*(points[i].y+points[j].y)/200.0+minY)+8);
				}
			}
		}		
	}

	public Path getRelation(int x, int y){
		return distances[Math.max(x, y)-1][Math.min(x, y)];
	}
	
	public void setPath(int x, int y, Path path){
		distances[Math.max(x, y)-1][Math.min(x, y)] = path;
	}
	
	
		
}
