/*
 *Class that server 0 use to send partition messages to other 
 *servers and clients
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.io.DataOutputStream;

public class Partition extends Thread {
	public BufferedReader inFromUser;
	public String partitionInfo;
	private ServerNode sn;
	
	//constructor
	public Partition(ServerNode sn) {	
		this.sn=sn;
	}

	@Override
	public void run() {
		while (true) {
			inFromUser = new BufferedReader(new InputStreamReader(System.in));
			try {
				partitionInfo = inFromUser.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			StringTokenizer st = new StringTokenizer(partitionInfo);
			int numTokens = st.countTokens();

			/*
			 *numTokens==2 means that the command from server 0 is "restore partition"
			 *numTokens==3 means that the coomand from server 0 is "partition serverList clientList"
			 *where serverList and clientList contain id of servers and clietns belong to the same 
			 *partition, all rest nodes not mentioned in these two set beong to the other partition.
			 *For example, "partition 0123 0" will partition the network to two parts, part 1 consists
			 *of server node 0,1,2,3 and client node 0. Part2 is made up of server node 4,5,6 and client 
			 *node 1,2,3,4.
			 */
			if (numTokens == 2) {
				String operation = st.nextToken();
				if (operation.equals("restore")) {
					String partition = st.nextToken();
					if (partition.equals("partition")) {
						try {
							DataOutputStream outToEveryone;
							for (int k=0;k<(ServerNode.numClients+ServerNode.numServers)-1;k++) {
								outToEveryone=new DataOutputStream(ServerNode.serverList.get(k).getOutputStream());
								outToEveryone.writeBytes(partitionInfo + "\n");
							}													
							SClient.partArr.clear();
							for(int i=0;i<ServerNode.numServers;i++)
							{
								SClient.partArr.add(i);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.out
								.println("Please put 'partition' after 'restore'");
					}
				} else {
					System.out.println("please put 'restore partition' here");
				}
			} else if (numTokens == 3) {
				String operation = st.nextToken();
				String serverList = st.nextToken();
				String clientList = st.nextToken();
				Boolean legalInput = true;
				
				try {
					Integer.parseInt(serverList);
				} catch (NumberFormatException e) {
					legalInput = false;
				}
				try {
					Integer.parseInt(clientList);
				} catch (NumberFormatException e) {
					legalInput = false;
				}


				if (operation.equals("partition")&&legalInput) {
					try {
						DataOutputStream outToEveryone;
						for (int k=0;k<(ServerNode.numClients+ServerNode.numServers)-1;k++) {
							outToEveryone=new DataOutputStream(ServerNode.serverList.get(k).getOutputStream());
							outToEveryone.writeBytes(partitionInfo + "\n");
						}
						String partServers=serverList;
						String partNotServers="";
						SClient.partArr.clear();
						for(int i=0;i<ServerNode.numServers;i++)
						{
							if(partServers.contains(i+""))
								continue;
							else
							{
								partNotServers=partNotServers+i+"";
							}
						}
						if(partServers.contains(sn.nodeID+""))
						{
							for(int i=0;i<partServers.length();i++)
							{
								SClient.partArr.add(Integer.parseInt(""+ partServers.charAt(i)));
							}
						}
						else
						{
							for(int i=0;i<partNotServers.length();i++)
							{
								SClient.partArr.add(Integer.parseInt(""+ partNotServers.charAt(i)));
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("illegal input, Please put 'partition serverSet clientSet' here");
				}
			} else {
				System.out.println("illegal input, Please put 'partition serverSet clientSet' here");
			}

		}

	}
}
