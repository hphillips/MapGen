package MapGen;

public interface Path extends Comparable<Path>{
	
	public boolean isConnected();
	
	public double distance();
	
	public boolean isDistanceActual();
	
	public int compareTo(Path arg0);
	
	public Path clone();
	
	class StarterPath implements Path{
		
		public int point1;
		public int point2;
		public double direct_distance;
		
		public StarterPath(int p1, int p2, double d){
			point1=Math.min(p1, p2); point2=Math.max(p1, p2);
			this.direct_distance=d;
		}
		
		public FinishedPath finalizePath(double path_distance, int next_point){
			return new FinishedPath(point1,point2,direct_distance,path_distance,next_point);
		}
		
		public boolean isConnected(){
			return false;
		}
		
		public double distance(){
			return direct_distance;
		}
		
		public boolean isDistanceActual(){
			return false;
		}
				
		public int compareTo(Path otherPath) {
			if(this.distance()<otherPath.distance()) return -1;
			else if(this.distance()>otherPath.distance()) return 1;
			else return 0;
		}
		
		public StarterPath clone(){
			return new StarterPath(point1,point2,this.direct_distance);
		}

		public String toString(){
			return point1+" "+point2+" "+direct_distance;
		}
	}
	
	class FinishedPath implements Path {

		public int point1;
		public int point2;
		public double direct_distance;
		public double actual_distance;
		public int next_point;
		
		public FinishedPath(int point1, int point2, double direct_distance, double actual_distance, int next_point){
			this.point1=point1;
			this.point2=point2;
			this.direct_distance=direct_distance;
			this.actual_distance=actual_distance;
			this.next_point=next_point;
		}
		
		public boolean isConnected() {
			return next_point==point2;
		}

		public double distance() {
			return actual_distance;
		}

		public boolean isDistanceActual() {
			return true;
		}

		public int compareTo(Path otherPath) {
			if(this.distance()<otherPath.distance()) return -1;
			else if(this.distance()>otherPath.distance()) return 1;
			else return 0;
		}

		public Path clone() {
			return new FinishedPath(point1, point2, direct_distance, actual_distance, next_point);
		}
		
	}
	
}

