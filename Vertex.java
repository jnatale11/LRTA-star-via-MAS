//Single point in mesh used on path finding alg.
//Vertex
public class Vertex{
	public int x,y,r,c;
	public double h;
	//constructor
	public Vertex(int coord_x, int coord_y, int row, int col){
		x=coord_x;
		y=coord_y;
		h=0;
		r = row;
		c = col;
	}
	
	//setter
	public synchronized void SetHeur(double val){
		h=val;
	}
	
	//getter 
	public synchronized double GetHeur(){
		return h;
	}
	
	//reset vert for new search
	public void Reset(){
		h=0;
	}
	
	//printout function
	public String toString(){
		return "("+r+","+c+")";
	}
}