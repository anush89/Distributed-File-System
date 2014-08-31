/*
 *Main class for servers. When instantiated will read the configuration file 
 *and store them to an arraylist called nodeList. Servers will start up as 
 *servers to servers with id bigger than its own id and clients to server with
 *id smaller than its own id.
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class ServerNode {
	public int nodeID;
	public String hostNmae;
	public int port;
	public static ServerSocket serverSoc = null;
	private static Socket clientSoc = null;
	private static String config;
	public static int numServers;
	public static int numClients;
	public ArrayList<IndividualNode> nodeList;
	public static ArrayList<Socket> serverList;
	public HashMap<String, OBject> hashmap;

	//constructor	
	public ServerNode() {

		// read the configuration file to a string
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("config.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		config = "";
		String readLine;
		String pattern = "([^#]*)(#?)(.*)";

		try {
			while ((readLine = br.readLine()) != null) {
				String matchedReadLine = readLine.replaceAll(pattern, "$1");
				config += matchedReadLine;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// parse the configuration file
		StringTokenizer st = new StringTokenizer(config);

		// number of nodes, all process should agree with this number
		numServers = Integer.parseInt(st.nextToken());
		numClients = Integer.parseInt(st.nextToken());

		// get the id, hostname and port of this process
		nodeList = new ArrayList<IndividualNode>();
		for (int i = 0; i < numServers; i++) {
			IndividualNode in = new IndividualNode();
			nodeList.add(in);
			int nodeID = Integer.parseInt(st.nextToken());
			String hostName = st.nextToken();
			int port = Integer.parseInt(st.nextToken());
			in.iId = nodeID;
			in.iHostName = hostName;
			in.iPort = port;
			try {
				if (hostName.equals(InetAddress.getLocalHost().getHostName())) {
					this.nodeID = nodeID;
					this.hostNmae = hostName;
					this.port = port;
					try {
						serverSoc = new ServerSocket(port);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}

		serverList = new ArrayList<Socket>();
		hashmap = new HashMap<String, OBject>();
	}

	public class IndividualNode {
		int iId;
		String iHostName;
		int iPort;
	}

	class OBject {
		int value;

		public OBject(int value) {
			this.value = value;
		}
	}

	public static void main(String[] args) {
		ServerNode sn = new ServerNode();

		/*
		 *iterate through all servers and connect to servers with id greater than its own
		 *id. Start two new thread to receive and send messages with other servers.
		 */
		for (IndividualNode i : sn.nodeList) {
			if (i.iId < sn.nodeID) {
				try {
					Socket client = new Socket(
							InetAddress.getByName(i.iHostName), i.iPort);
					new SClient(client,sn).start();
					new SendServerThread(client,sn).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 *server 0 is in charge of sending partition info to other servers and clietns.
		 *Start a new thread "Partiton" to send the partition message. 
		 */
		if (sn.nodeID==0) {
			new Partition(sn).start();
		}

		/*
		 *When receive a connection from a server, start two threads "SClient" and "SendServerThread"
		 *to receive and send messages between servers. If connection is from client, simply start the  
		 *"SServer" thread to deal with all communications with clients
		 */
		while (true) {
			Boolean connectionFromServer = false;
			try {
				clientSoc = serverSoc.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			for (IndividualNode i : sn.nodeList) 
			{
				if(clientSoc.getInetAddress().getHostName().equals(i.iHostName))
				{
					new SClient(clientSoc,sn).start();
					new SendServerThread(clientSoc,sn).start();
					serverList.add(clientSoc);
					connectionFromServer = true;
				}
			}
			if(!connectionFromServer)
			{
				new SServer(clientSoc, sn).start();
				serverList.add(clientSoc);
			}
		}
	}
}
