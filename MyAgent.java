import java.util.concurrent.Future;

import javax.swing.JTextArea;

import boris.kernel.*;
import boris.utils.*;
//extending Boris' Agent class
public class MyAgent extends Agent implements MessageListener{
		
	public String name;
	public static JTextArea log;
	public Future<?> thread;	
	
	//constructor
	public MyAgent(String n, JTextArea l){
		super(n);
		name = n;
		log = l;
		addMessageListener(this);
	}
	
	//give agent a thread of operation
	public void addThread(Future<?> t){
		thread = t;
	}
	
	//custom function to set log and send Boris msg
	public void message(String recipient, String msg){
		if(recipient.equals("LOG")){
			log.append(msg+"\n");
		}
		else{
			log.append(this.name+" sent '"+msg+"' to "+recipient+"\n");
			this.sendMessage(recipient,msg);
		}
	}
	
	//custom function  to receive messages through Boris system
	public void messageReceived(String from, String to, String msg, MsgId msgId){
		if(msg.equals("THREAD_CANCEL")){
			thread.cancel(true);
		}
	}
}