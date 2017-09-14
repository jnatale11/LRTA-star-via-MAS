import java.awt.Graphics;
import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

//Defining a single thread worker
public class Worker extends Thread{
	public MyAgent agent;
	public Network net;
	public int agent_num;
	public ArrayList<Edge> path = null;
	private CyclicBarrier b;
	private Vertex T;
	private Vertex S;
	//constructor
	public Worker(Network n, MyAgent a, int agentnum, CyclicBarrier cb, Vertex goal, Vertex start){
		agent = a;
		net = n;
		agent_num = agentnum;
		b = cb;
		T = goal;
		S = start;
	}
	//thread execution
	public void run(){
		try{
		//c.register();
		Vertex u = S;
		Vertex v;
		double new_val;
		double min_vert_val;
		Vertex vPrime;
		boolean first;
		while(true && !Thread.currentThread().isInterrupted()){
			Edge adder = null;
			u = S;
			//empty out agent bucket
			net.agentbuckets.get(agent_num).clear();
			//iterate until hitting end goal
			while(!u.equals(T) && !Thread.currentThread().isInterrupted()){
				vPrime = null;
				min_vert_val = 1000000;
				first = true;
				//find all vertices connected to u
				for(Edge e : net.edges){
					if(e.start.equals(u)){
						v = e.end;
					}
					else if(e.end.equals(u)){
						v = e.start;
					}
					else
						continue;
					new_val = e.weight + v.GetHeur();
					if(new_val < min_vert_val || first){
						min_vert_val = new_val;
						vPrime = v;
						adder = e;
						first = false;
					}	
				}
				//set shared heuristic value
				u.SetHeur(Math.max(u.GetHeur(), min_vert_val));
				agent.message("LOG", agent.name+" has updated vertex "+u+"'s heuristic to "+u.GetHeur());
				//add in edge to the given agent
				net.agentbuckets.get(agent_num).add(adder);
				u = vPrime;
			}
			//broadcast completion of 
			agent.message("LOG", agent.name+" has completed a trial.");
			
			//check to see if path info is equivalent to last run, exit if so
			if(path!=null && path.equals(net.agentbuckets.get(agent_num))){
				//send message to Agent0 to end entire procedure
				net.EndProcess(agent_num);
				break;
			}
			//save path
			path = new ArrayList<Edge>(net.agentbuckets.get(agent_num));
			//barrier to wait for all other agents' completion
			b.await();
			//only first agent calls to repaint GUI
			if(agent_num==0){
				net.MyPanel.repaint();
				Thread.sleep(80);
			}
			//sync back up
			b.await();
		}
		System.out.println(agent.name+" completed search");
	}
	catch(InterruptedException e){
		System.out.println(agent.name+" completed search");
	}
	catch(BrokenBarrierException e){
		b.reset();
	}
	catch(Exception e){
		e.printStackTrace();
	}
	}
}
