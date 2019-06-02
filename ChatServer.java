//github.com/kabosuMy3a

import java.text.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

	static ArrayList<String> banList = new ArrayList<String>() ;	

	public static void main(String[] args) {
	
		Date timeNow = new Date() ;
	 	SimpleDateFormat DateFormat = new SimpleDateFormat("HH:mm:ss") ;
		
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.print("["+DateFormat.format(timeNow)+"] ");
			System.out.println("Waiting connection...");
			HashMap hm = new HashMap();
			setBanList();

			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm, banList);
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main

	public static void setBanList(){
	
		String filepath = "banlist.txt" ;
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

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap hm;
	private boolean initFlag = false;
	private ArrayList<String> banList ;
	private PrintWriter pwToMe ;
	private Date timeNow ;
	private SimpleDateFormat DateFormat ;

	public ChatThread(Socket sock, HashMap hm, ArrayList<String> banList){
		this.sock = sock;
		this.hm = hm;
		this.banList = banList ;
		try{
			
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			pwToMe = pw ;
			banList = new ArrayList<String>() ;
			timeNow = new Date();
			DateFormat = new SimpleDateFormat("HH:mm:ss");
			broadcast(id + " entered.");
			System.out.print("["+DateFormat.format(timeNow)+"] ");
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
							
						if(!banned){ 
							pwToMe.print("["+DateFormat.format(timeNow)+"] ");
							pwToMe.print("you sent bad message");
							pwToMe.flush();
						}	
						pwToMe.print(" ||"+banWord);
						pwToMe.flush();
						banned = true ;
					}
				}

				if (banned==true){
				       	pwToMe.println();
					pwToMe.flush();
					continue ;
				}

				if(line.equals("/spamlist")){

					printSpamList();
				}

				else if(line.indexOf("/addspam ")==0){

					addSpamToBanList(line.substring(line.indexOf(" ")+1));

				}

				else if(line.indexOf("/to ") == 0){
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
				pw.print("["+DateFormat.format(timeNow)+"] ");
				pw.println(id + " whisphered: " + msg2);
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
					pw.print("["+DateFormat.format(timeNow)+"] ");
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

			pwToMe.println("----------------------------------");
			pwToMe.println("["+DateFormat.format(timeNow)+"] ");
			pwToMe.flush();
			for(String allUser : sortedUserList){	
				pwToMe.println(allUser) ;
				pwToMe.flush();
			}		
			pwToMe.println("The number of Users is "+ sortedUserList.size());
			pwToMe.flush();
			pwToMe.println("----------------------------------") ;
			pwToMe.flush();
				
			
		}		
	}

	/*
	 * set bad word from banList.txt ;
	 * if you want to edit banWord please check banList.txt
	 */


	public void printSpamList(){
		
			pwToMe.println("----------------------------------") ;
			pwToMe.println("["+DateFormat.format(timeNow)+"] ");
			synchronized(banList){
				for(String spamline : banList){
					pwToMe.println(spamline);			
				}
				pwToMe.println("The number of spams is "+ banList.size());
			}
			pwToMe.println("----------------------------------") ;
			pwToMe.flush();

	}

	public synchronized void addSpamToBanList(String spam){
		
		banList.add(spam);
		
		PrintWriter outputStream = null;

		try{

			File file = new File("banlist.txt");
			if(!file.exists()){
				outputStream = new PrintWriter("banlist.txt");
			}

			else{
				FileWriter filewriter = new FileWriter("banlist.txt", true);
				outputStream = new PrintWriter(filewriter);
			}
		}catch(Exception e){
			System.out.println(e);
		}

		outputStream.println(spam);
		outputStream.close();	


	}
}

	

