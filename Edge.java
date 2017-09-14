//Single line in mesh used on path finding alg.
//Edge
public class Edge{
	
	public Vertex start;
	public Vertex end;
	public double weight;
	
	public Edge(Vertex a, Vertex b, int dist){
		start = a;
		end = b;
		weight = dist;
	}
}