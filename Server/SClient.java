/*
 *Thread class of server node used to receive messages from other servers
 *
 */

import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;

public class SClient extends Thread {
	private Socket socket;
	private BufferedReader inFromServer;
	private DataOutputStream outToServer;
	private ServerNode sn;

	//to achieve mutual exclusion between threads try to access the same arrylist
	private static Semaphore mutex=new Semaphore(1);

	public static ArrayList<Integer> partArr;

	//constructor
	public SClient(Socket socket,ServerNode sn) {
		this.socket = socket;
		this.sn=sn;
		partArr=new ArrayList<Integer>();
		for(int i=0;i<ServerNode.numServers;i++)
		{
			partArr.add(i);
		}
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
		String receivedSentence = null;
		while(true)
		{
			try {
				receivedSentence = inFromServer.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("recvd msg: "+receivedSentence);
			StringTokenizer st = new StringTokenizer(receivedSentence);
			
			int numTokens = st.countTokens();

			/*
			 *numTokens==2 means that "restore partition" is received from server 0
			 *numTokens==3 means that "partition serverSet clientSet" is received from server 0
			 */		
			if(numTokens==2)
			{
				String filename=st.nextToken();
				String newVal=st.nextToken();
				
				if(filename.equals("restore") && newVal.equals("partition"))
				{
					partArr.clear();
					for(int i=0;i<ServerNode.serverList.size();i++)
					{
						partArr.add(i);
					}
					continue;
				}
				
				try {
					mutex.acquire();
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
				boolean flag=true;
				while(flag)
				{
					for(int i=0;i<SServer.pendingUpdates.size();i++)
					{
						if(filename.equals(SServer.pendingUpdates.get(i).get(0)) && Integer.parseInt(newVal)==Integer.parseInt(SServer.pendingUpdates.get(i).get(1)))
						{
							ServerNode.OBject newObject = sn.new OBject(Integer.parseInt(SServer.pendingUpdates.get(i).get(1)));
							sn.hashmap.put(SServer.pendingUpdates.get(i).get(0), newObject);
							System.out.println("update completed, "
										+ SServer.pendingUpdates.get(i).get(0) + " " + SServer.pendingUpdates.get(i).get(1) + " is updated");
							SServer.pendingUpdates.remove(i);
							flag=false;
							break;
						}
					}
				}
				mutex.release();
			}
			else if(numTokens==3)
			{
				if(st.nextToken().equals("partition"))
				{
					String partStr=st.nextToken();
					String partNotServers="";
					partArr.clear();
					for(int i=0;i<ServerNode.numServers;i++)
						{
							if(partStr.contains(i+""))
								continue;
							else
							{
								partNotServers=partNotServers+i+"";
							}
						}
						if(partStr.contains(sn.nodeID+""))
						{
							for(int i=0;i<partStr.length();i++)
							{
								SClient.partArr.add(Integer.parseInt(""+ partStr.charAt(i)));
							}
						}
						else
						{
							for(int i=0;i<partNotServers.length();i++)
							{
								SClient.partArr.add(Integer.parseInt(""+ partNotServers.charAt(i)));
							}
						}
				}
				else
				{
					System.out.println("Unknown command received\n");
				}
			}
		}
	}
}
