/*
 *Class that initiazlie sockets so that the client can 
 *receive messaegs from servers
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class CClientReceive extends Thread {
	public Socket socket;

	//stores the serve that the client connects to
	public ClientNode.Server server;

	public BufferedReader inFromServer;

	//the instantiate of the ClientNode class
	private ClientNode cn;
	public String receivedSentence;

	//constructor
	public CClientReceive(Socket client, ClientNode.Server s, ClientNode cn) {
		this.socket = client;
		this.server = s;
		this.cn = cn;

		try {
			inFromServer = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				receivedSentence = inFromServer.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			StringTokenizer st = new StringTokenizer(receivedSentence);
			int numTokens = st.countTokens();

			/*
			 *numTokens==1 means server send replies to the "read" request from client;
			 *numTokens==2 means server send replies to the "restore partition" request from client;
			 *numTokens==3 means server send replies to the "partition" request from client;
			 *numTokens>3 means server replies is undefined, thus disregarded and output an error message
			 */
			if (numTokens == 1) {
				System.out.println("the value is " + st.nextToken()
						+ " from server " + server.sId);
			} else if (numTokens == 2) {
				String restore = st.nextToken();
				String partition = st.nextToken();
				if (restore.equals("restore") && partition.equals("partition")) {
					for (int i = 0; i < (ClientNode.numServers + ClientNode.numClients); i++) {
						cn.isInSamePartition.set(i, true);
					}
				} else {
					System.out
							.println("unrecongnized 2 words message from server");
				}

			} else if (numTokens == 3) {
				String operation = st.nextToken();

				if (operation.equals("partition")) {

					/*
					 *serverPart and clientPart store the string representation of server
					 *and client partition set, we use two numeric arryalist to store the 
					 *server id set and client id set for easier processing
					 */
					String serverPart = st.nextToken();
					String clientPart = st.nextToken();				
					ArrayList<Integer> serverPartSet = new ArrayList<Integer>();
					ArrayList<Integer> clientPartSet = new ArrayList<Integer>();

					//put the server id one by one into serverPart arraylist
					for (int i = 0; i < serverPart.length(); i++) {
						serverPartSet.add(Integer.parseInt(""
								+ serverPart.charAt(i)));
					}
					
					//put the client id one by one into clientPart arraylist
					for (int j = 0; j < clientPart.length(); j++) {
						clientPartSet.add(Integer.parseInt(""
								+ clientPart.charAt(j)));
					}

					System.out
							.println("server "+serverPartSet+" and client "+clientPartSet+
								" are in one partition, all the rest are in the other partition");
					
					/*
					 *flag the isInSamePartition with boolean value by processing both
					 *the serverpartSet and clientPartSet. For example, if the serverPartSet
					 *is [0,1,2,3], the clientPartSet is [0] and the client id is 1, we set 
					 *isInSamePartition to be [false,false,flase,flase,true,true,true,false,
					 true,true,ture,ture].
					 */
					if (!clientPartSet.contains(cn.nodeID)) {
						for (Integer i : serverPartSet) {
							cn.isInSamePartition.set(i, false);
						}
						for (Integer j : clientPartSet) {
							cn.isInSamePartition.set(ClientNode.numServers + j,
									false);
						}						
					} else if (clientPartSet.contains(cn.nodeID)) {
						for (int k = 0; k < (ClientNode.numServers + ClientNode.numClients); k++) {
							cn.isInSamePartition.set(k, false);
						}
						for (Integer i : serverPartSet) {
							cn.isInSamePartition.set(i, true);
						}
						for (Integer j : clientPartSet) {
							cn.isInSamePartition.set(ClientNode.numServers + j,
									true);
						}						
					}
					
				} else {
					System.out
							.println("unrecognized 3 words message from server");
				}
			} else {
				System.out.println(receivedSentence);
			}
		}
	}
}
