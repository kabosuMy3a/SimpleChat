//github.com/kabosuMy3a

import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.println("Waiting connection...");
			HashMap hm = new HashMap();
			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm);
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap hm;
	private boolean initFlag = false;
	private ArrayList<String> banList ;
	private PrintWriter pwToMe ;

	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock;
		this.hm = hm;
		try{
			
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			pwToMe = pw ;
			banList = new ArrayList<String>() ;
			setBanList();
			broadcast(id + " entered.");
			System.out.println("[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	public void run(){
		try{
			String line = null;
			while((line = br.readLine()) != null){
				
				boolean banned = false ;

				if(line.equals("/quit"))
					break;
				
				/*
				 * following code is checking user sends bad message
				 * until continue;
				 */
				
				for(String banWord : banList){	
					if(line.contains(banWord)){
						pwToMe.println("you sent bad message");
						pwToMe.flush();
						banned = true ;
						break;
					}
				}

				if (banned==true) continue ;

				if(line.indexOf("/to ") == 0){
					sendmsg(line);
				}

				else if(line.equals("/userlist")) {
					send_userlist();	
				}else{
					broadcast(id + " : " + line);
				}
			}
		}catch(Exception ex){
			System.out.println(ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id + " exited.");
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	
	public void sendmsg(String msg){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		if(end != -1){
			String to = msg.substring(start, end);
			String msg2 = msg.substring(end+1);
			Object obj =null ;
			synchronized(hm){
				obj = hm.get(to);
			}
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println(id + " whisphered. : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg

	
	//don't broadcast to me	
	public void broadcast(String msg){
		synchronized(hm){
		
			Set<String> set = hm.keySet();	
			for(String iter : set){
			
				if(iter != id){
					Object obj = hm.get(iter);
					PrintWriter pw = (PrintWriter) obj;
					pw.println(msg);
					pw.flush();

				}
			/*
			Collection collection = hm.values();
			Iterator iter = collection.iterator();
			while(iter.hasNext()){
				
				PrintWriter pw = (PrintWriter)iter.next();
				pw.println(msg);
				pw.flush();

			}*/

			}
		} // broadcast
	}

	/*
	 * this method show userlist and The number of Users 
	 */
	public void send_userlist(){
			
		synchronized(hm){
			
			TreeSet<String> sortedUserList = new TreeSet<String>(hm.keySet()) ;

			for(String username : sortedUserList){	
				if (username==id){
					Object obj =null ;
					synchronized(hm){
						obj = hm.get(username);
					}	
					
					PrintWriter pw = (PrintWriter) obj ;
		
					pw.println("----------------------------------");
					pw.flush();
					for(String allUser : sortedUserList){	
						pw.println(allUser) ;
						pw.flush();
					}				
					pw.println("The number of Users are "+ sortedUserList.size());
					pw.flush();
					pw.println("----------------------------------") ;
					pw.flush();
				}
			}
		}		
	}

	/*
	 * set bad word from banList.txt ;
	 * if you want to edit banWord please check banList.txt
	 */
	public void setBanList(){
	
		String filepath = "banList.txt" ;
		String line = "" ;
		
		try{

			Scanner inputStream = new Scanner(new File(filepath));
			while(inputStream.hasNextLine()){
				line = inputStream.nextLine();
				banList.add(line);
			}
			
			inputStream.close();
		}
		catch (Exception e){

			System.out.println(e);
		}
	}
}

	

