/*
 *Thread class of server node used to send messages to other servers
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;

public class SendServerThread extends Thread {
	public Socket socket;
	private BufferedReader inFromServer;
	private DataOutputStream outToServer;
	private ServerNode sn;
	private static AtomicInteger count;
	private static Semaphore mutex=new Semaphore(1);
	private static boolean[] msgSent;

	//constructor
	public SendServerThread(Socket socket,ServerNode sn) {
		this.socket = socket;
		this.sn=sn;
		msgSent=new boolean[sn.numServers];
		count=new AtomicInteger(0);
		try {
			inFromServer = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			outToServer = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while(true)
		{

			/*
			 *if server has already sent to other servers of
			 *the same partition who is not the primay server of 
			 *a certain file, sleep a while and check back later
			 */
			 
			try{
				Thread.sleep(10);
			}catch(InterruptedException e)
			{
				e.printStackTrace();
			}
				
			if(SServer.pendingUpdatesPrimary==null)
			{
				try{
					Thread.sleep(100);
				}catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				continue;
			}
			
			try {
				mutex.acquire();
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
			
			if(SServer.pendingUpdates.size()==0 && SServer.pendingUpdatesPrimary.size()!=0)
			{
				if(SServer.hash(SServer.pendingUpdatesPrimary.get(0).get(0))==sn.nodeID 
					&& !SClient.partArr.contains((sn.nodeID+2)%ServerNode.numServers))
				{
					count.set(1);
					msgSent[((sn.nodeID+2)%ServerNode.numServers)]=true;
				}
				
				if(((SServer.hash(SServer.pendingUpdatesPrimary.get(0).get(0))+1)%ServerNode.numServers)==sn.nodeID)
				{
					count.set(1);
					msgSent[((sn.nodeID+2)%ServerNode.numServers)]=true;
				}

				if((sn.nodeList.get((sn.nodeID+1)%ServerNode.numServers).iHostName.equals(socket.getInetAddress().getHostName()) 
					&& msgSent[((sn.nodeID+1)%ServerNode.numServers)]==false) 
					|| (sn.nodeList.get((sn.nodeID+2)%ServerNode.numServers).iHostName.equals(socket.getInetAddress().getHostName()) 
					&& msgSent[((sn.nodeID+2)%ServerNode.numServers)]==false))
				{
					if(sn.nodeList.get((sn.nodeID+1)%ServerNode.numServers).iHostName.equals(socket.getInetAddress().getHostName()))
						msgSent[((sn.nodeID+1)%ServerNode.numServers)]=true;
					else
						msgSent[((sn.nodeID+2)%ServerNode.numServers)]=true;
					
					try {
						outToServer.writeBytes(SServer.pendingUpdatesPrimary.get(0).get(0)+" "+ SServer.pendingUpdatesPrimary.get(0).get(1)+"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}

					if(count.get()==1) {					
						String fileName=SServer.pendingUpdatesPrimary.get(0).get(0);
						String newVal=SServer.pendingUpdatesPrimary.get(0).get(1);
						ServerNode.OBject newObject = sn.new OBject(Integer.parseInt(newVal));
						sn.hashmap.put(fileName, newObject);
						System.out.println("update completed, "
								+ fileName + " " + newVal + " is updated");
						SServer.pendingUpdatesPrimary.remove(0);
						for(int i=0;i<msgSent.length;i++)
							msgSent[i]=false;
						count.set(0);
					} else {					
						count.set(1);
					}	
				}				
			}
			mutex.release();
		}
		
	}
}
