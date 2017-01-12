package MapGen;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FinalCluster {

	private List<Point> points;
	
	public FinalCluster(Collection<Point> points){
		this.points = new ArrayList<Point>(points.size());
		this.points.addAll(points);
	}
	
	public List<Point> getPoints(){
		return points;
	}
		
}
