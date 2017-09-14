import boris.kernel.*;
import boris.utils.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;

import javax.swing.*;
import javax.swing.event.*;

/** 
2D Environment Containing Multiple Agents Performing LRTA*(n) alg
*/

public class Network extends JFrame 
{
	//agent variables
	private static Portal portal;
	public static MyAgent agents[];
	public static int correct_bucket;
	public static ArrayList<ArrayList<Edge>> agentbuckets = new ArrayList<ArrayList<Edge>>();
	//fixed thread pool which will define a number of threads that can run concurrently
	private static ExecutorService ex = null;
	public static CyclicBarrier barrier;
	private static boolean Search_Done = true;
	//private static final Coordinator c = new Coordinator();
	private static int[] vertKey;
	private static JTextArea log;
	public static JPanel MyPanel;
	//graph variables
	public static ConcurrentHashMap<String, Vertex> verts = new ConcurrentHashMap<String,Vertex>();
	public static ArrayList<Edge> edges = new ArrayList<Edge>();
	public static boolean edges_set = false;
	public static Iterator<Edge> iter = edges.iterator();
	public static Vertex S = null;
	public static Vertex T = null;
	public static Ellipse2D.Double searcher = new Ellipse2D.Double(1000,1000,7,7);
	public static Ellipse2D.Double setter = new Ellipse2D.Double(1000,1000,18,18);
	public static ArrayList<Rectangle> Rectangles = new ArrayList<Rectangle>();
	public static ArrayList<Ellipse2D.Double> Ellipses = new ArrayList<Ellipse2D.Double>();
	
	//main execution
	public static void main(String[] args){
		
		//create portal
		portal = new Portal("portal");
		
		int num_agents;
		
		//get number of path agents, or set to default of 3
		if(args.length >1 && args[0].equals("-n"))
			try{
				num_agents = Integer.parseInt(args[1]);
			}
			catch(Throwable t){
				num_agents = 3;
			}
		else{
			num_agents = 3;
		}
		
		//creating GUI
		JFrame jf = new JFrame("LRTA*("+num_agents+") by Jason Natale");
		Container content = jf.getContentPane();
		content.setLayout(new BorderLayout());
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setSize(700,730);
		jf.setLocation(0,0);
		//defining space
		MyPanel=new JPanel(){
			public void paintComponent(Graphics g){
				super.paintComponent(g);
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(Color.LIGHT_GRAY);
				//iterate over vertices
				for(Vertex v : verts.values()){
					g.fillOval(v.x, v.y, 2, 2);
				}
				//iterate over edges
				//print_edges(g);
				g.setColor(Color.CYAN);
				//iterate over additional structures
				for(Rectangle r : Rectangles){
					g.fillRect((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
				}
				for(Ellipse2D.Double elli : Ellipses){
					g.fillOval((int)elli.x, (int)elli.y, (int)elli.width, (int)elli.height);
				}
				//repaint edges for agents
				if(Search_Done){
					if(correct_bucket >= 3){
						correct_bucket = (int)(Math.random()*3);
					}
					switch(correct_bucket){
						case 0: g.setColor(Color.RED);
							break;
						case 1: g.setColor(Color.GREEN);
							break;
						case 2: g.setColor(Color.YELLOW);
							break;
						default: g.setColor(Color.YELLOW);
					}
					for(Edge e: agentbuckets.get(correct_bucket)){
						g.drawLine(e.start.x, e.start.y, e.end.x, e.end.y);
					}
				}
				else{
					//color
					if(agents.length == 3){
						for(int i=0;i<3;i++){
							if(i == 0)
								g.setColor(Color.RED);
							else if(i==1)
								g.setColor(Color.GREEN);
							else
								g.setColor(Color.YELLOW);
							for(Edge e: agentbuckets.get(i)){
								g.drawLine(e.start.x, e.start.y, e.end.x, e.end.y);
							}
						}
					}
					//Basic Black for all agent lines
					else{
						g.setColor(Color.WHITE);
						for(int i=0;i<agents.length;i++){
							for(Edge e: agentbuckets.get(i)){
								g.drawLine(e.start.x, e.start.y, e.end.x, e.end.y);
							}
						}
					}
				}
			}
			
			//synchronized method to print out edges
			private synchronized void print_edges(Graphics g){
				if(edges!=null){
					for(Edge e : edges){
						g.drawLine(e.start.x, e.start.y, e.end.x, e.end.y);
					}
				}
			}
		};
		MyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		//defining bottom log
		log = new JTextArea(5,25);
		//scroll pane
		JScrollPane scroll = new JScrollPane (log);
		new SmartScroller(scroll);
		//adding and setting visible
		content.add(MyPanel,BorderLayout.CENTER);
		content.add(scroll,BorderLayout.PAGE_END);
		jf.setVisible(true);
		
		//create 2D environment & path finding agents
		final Network world = new Network(num_agents);
		MyPanel.repaint();
		
		//adding listeners
		MyPanel.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				world.RegisterNewClick(e);
			}
		});
		MyPanel.addMouseMotionListener(new MouseAdapter(){
			public void mouseMoved(MouseEvent e){
				world.RegisterMovement(e);
			}
		});
	}


	/*==============================================
		Constructor receiving number of
		independent learning agents
	\*============================================*/
	public Network(int num_agents){
		agents = new MyAgent[num_agents];
		ArrayList<Edge> bucket = new ArrayList<Edge>();
		ex = Executors.newFixedThreadPool(num_agents);
		barrier = new CyclicBarrier(num_agents);
		//iterate & create agents
		for(int i=0; i<num_agents; i++){
			agents[i] = new MyAgent("Agent"+i,log);
			portal.addAgent(agents[i]);
			bucket = new ArrayList<Edge>();
			agentbuckets.add(bucket);
		}
		createMesh(20);
		addObstacles();
	}

	
	/*==============================================
			Production of Graphical Mesh
	\*============================================*/
	private void createMesh(int dist){
		int max_x = MyPanel.getWidth();
		int max_y = MyPanel.getHeight();
		int x,y = 0;
		int col = 0;
		int row = 0;
		//iterate across rows and columns
		while(y<max_y){
			if(row%2==0)
				x = 0;
			else
				x = dist/2;
			col = 0;
			while(x<max_x){
				//creating vertex and setting edges
				Vertex v = new Vertex(x,y,row,col);
				String key = row+"-"+col;
				int con[] = {row,col};
				verts.put(key, v);
				//connecting leftward
				if(col!=0){
					con[1] = col-1;
					Edge e = new Edge(v,verts.get(con[0]+"-"+con[1]),dist);
					edges.add(e);
				}
				//connecting upward
				if(row!=0){
					con[0] = row-1;
					//link to the left
					if(x!=0){
						if(row%2==0){
							con[1] = col-1;
						}
						else
							con[1] = col;
						if(verts.get(con[0]+"-"+con[1])==null)
							System.out.println("b "+con[0]+"-"+con[1]);
						Edge e = new Edge(v,verts.get(con[0]+"-"+con[1]),dist);
						edges.add(e);
					}
					//link to the right
					if(x<=max_x-dist/2){
						if(row%2==0)
							con[1] = col;
						else
							con[1] = col+1;
						if(verts.get(con[0]+"-"+con[1])==null)
							System.out.println("c "+con[0]+"-"+con[1]);
						Edge e = new Edge(v,verts.get(con[0]+"-"+con[1]),dist);
						edges.add(e);
					}
				}
				col++;
				x+=dist;
			}
			y+=dist;
			row++;
		}
	}
	
	//creating and adding obstacles to frame
	private void addObstacles(){
		//adding ellipses
		Ellipse2D.Double ellip = new Ellipse2D.Double(100,70,120,200);
		Ellipses.add(ellip);
		ellip = new Ellipse2D.Double(570,85,80,80);
		Ellipses.add(ellip);
		ellip = new Ellipse2D.Double(70,400,300,120);
		Ellipses.add(ellip);
		ellip = new Ellipse2D.Double(550,450,100,100);
		Ellipses.add(ellip);
		//adding rectangles
		Rectangle rect = new Rectangle(320,90,95,95);
		Rectangles.add(rect);
		rect = new Rectangle(270,250,305,55);
		Rectangles.add(rect);
		rect = new Rectangle(395,373,240,55);
		Rectangles.add(rect);
		//iterating over all to remove vertices and edges left
		List<Edge> toRemove = new ArrayList<Edge>();
		for(Rectangle r : Rectangles){
			for(Vertex v : verts.values()){
				if(r.contains(v.x,v.y)){
					for(Edge e :edges){
						if(e.end.equals(v) || e.start.equals(v)){
							toRemove.add(e);
						}
					}
					verts.remove(v.r+"-"+v.c);
				}
			}
		}
		for(Ellipse2D.Double elli : Ellipses){
			for(Vertex v : verts.values()){
				if(elli.contains(v.x,v.y)){
					for(Edge e : edges){
						if(e.end.equals(v) || e.start.equals(v)){
							toRemove.add(e);
						}
					}
					verts.remove(v.r+"-"+v.c);
				}
			}
		}
		//get rid of all edges
		edges.removeAll(toRemove);
	}
	
	//Handle clicks which set new start for path finding
	private static void RegisterNewClick(MouseEvent e){
		setter.x = e.getX();
		setter.y = e.getY();
		for(Vertex v : verts.values()){
			if(setter.contains(v.x,v.y)){
				S = v;
				log.append("New Starting Point Set\n");
			}
		}
		//reset prior search
		Network.ResetSearch();
	}
	
	//Handle movement which set new end point for path finding
	private void RegisterMovement(MouseEvent e){
	  //only if search completed attempt a new one
	  if(Search_Done){
		//need a start to have a finish #meta
		if(S != null){
			searcher.x = e.getX();
			searcher.y = e.getY();
			Vertex t = null;
			for(Vertex v : verts.values()){
				if(searcher.contains(v.x,v.y)){
					t = v;
					break;
				}
			}
			// and a finish to have a finish #metameta
			if(t!=null){
				Search_Done = false;
				//reset prior search
				this.ResetSearch();
				T = t;
				//start new search
				this.StartSearch();
			}
		}
	  }
	}
	
	//Resets a search and all variables associated
	private static void ResetSearch(){
		//empty all edge buckets and send kill messages
		for(int i = 0; i<agentbuckets.size(); i++){
			agentbuckets.get(i).clear();
			if(agents[i].thread!=null)
				agents[i].thread.cancel(true);
		}
		for(Vertex v : verts.values()){
			v.Reset();
		}
		T = null;
		boolean allAreDead = false;
		int i;
		for(;;){
			if(allAreDead)
				break;
			allAreDead = true;
			for(i=0;i<agents.length;i++){
				if(agents[i].thread!=null && !agents[i].thread.isDone())
					allAreDead = false;
			}
		}
		barrier.reset();
	}
	
	//Begin a search through graph using LRTA*(n) alg
	private void StartSearch(){
		log.append("New Search Starting from "+S+" to "+T+"\n");
		System.out.println("New Search Starting from "+S+" to "+T);
		//start n threads and attribute each to a different agent
		for(int x=0;x<agents.length;x++){
			//create runnable worker to perform generation logic
			Worker w = new Worker(this, agents[x], x, barrier,T,S);
			//ex.submit submits a thread to begin and returns a future
			//give future to agent
			agents[x].addThread(ex.submit(w));
		}
	}
	
	//End entire search
	public synchronized void EndProcess(int correct_agent){
		if(!Search_Done){
			//message all agents to stop
			//and empty buckets not containing solution
			for(int i=0;i<agents.length;i++){
				if(i != correct_agent){
					agents[i].thread.cancel(true);
					agentbuckets.get(i).clear();
				}
			}
			agents[correct_agent].thread.cancel(true);
			//loop until all threads are dead
			boolean allAreDead = false;
			int i;
			for(;;){
				if(allAreDead)
					break;
				allAreDead = true;
				for(i=0;i<agents.length;i++){
					if(!agents[i].thread.isDone())
						allAreDead = false;
				}
			}
			Search_Done = true;
			//set bucket which is correct
			correct_bucket = correct_agent;
			//last call to repaint GUI
			MyPanel.repaint();
			System.out.println("Search Complete");
			log.append("Search Complete\n");
		}
	}
}
