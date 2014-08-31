/*
 *Class for the clients. When instantiated will read the configuration
 *file and parse user inputs. Based on use inputs, it will either randomly
 *send requests to one of the servers or write to three or two servers, if
 *not successful, will outupt error. 
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

public class ClientNode {
	//client's id, ranging from 0 to 4
	public int nodeID;

	//client's hostname
	public String hostNmae;

	//the server socket that client wants to connect to 
	public static ServerSocket serverSoc = null;

	//the config variable store the config file after processing
	public static String config;

	//#servers, which is 7 in this case
	public static int numServers;

	//#clients, which is 5 in this case
	public static int numClients;

	//store sockets for all servers, listed in order of server id
	public ArrayList<Server> nodeList;
	public static BufferedReader inFromUser;
	public static String inputSentence;
	public static Random random;
	public static ArrayList<CClientSend> clientList;
	public ArrayList<Boolean> isInSamePartition;

	//constructor
	public ClientNode() {
		/*
		 *Upuon initializing, read the config file to a string named "config",
		 *ignoring all characters after the "#"
		 */
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
		nodeList = new ArrayList<Server>();
		for (int i = 0; i < numServers; i++) {
			Server server = new Server();
			nodeList.add(server);
			int nodeID = Integer.parseInt(st.nextToken());
			String hostName = st.nextToken();
			int port = Integer.parseInt(st.nextToken());
			server.sId = nodeID;
			server.sHostName = hostName;
			server.sPort = port;
		}
		random = new Random();
		clientList = new ArrayList<CClientSend>();
		isInSamePartition = new ArrayList<Boolean>();
		for (int i = 0; i < (numServers + numClients); i++) {
			isInSamePartition.add(true);
		}
	}

	/*
	 *inner class for Server structure, which stroes the information
	 *read from the configuration file of a server
	*/
	class Server {
		int sId;
		String sHostName;
		int sPort;
	}

	public static void main(String[] args) {
		//cn is an instantiation of the class
		ClientNode cn = new ClientNode();

		//get it's id from the command line
		cn.nodeID = Integer.parseInt(args[0]);
		for (Server s : cn.nodeList) {
			try {
				Socket client = new Socket(InetAddress.getByName(s.sHostName),
						s.sPort);
				CClientReceive cr = new CClientReceive(client, s, cn);
				cr.start();
				CClientSend cs = new CClientSend(client, s);
				cs.start();
				clientList.add(cs);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		while (true) {
			inFromUser = new BufferedReader(new InputStreamReader(System.in));
			try {
				inputSentence = inFromUser.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			StringTokenizer st = new StringTokenizer(inputSentence);
			int numTokens = st.countTokens();
			if (numTokens == 2) {
				String read = st.nextToken();

				/*
				 *if the user input command is "read", randomly select a server and send request to that 
				 *server
				*/
				if (read.equals("read")) {
					String fileName = st.nextToken();
					int value = hash(fileName);

					//serverRead stores the server ids that the client can access
					ArrayList<Integer> serverRead = new ArrayList<Integer>();

					for (int k = 0; k < 3; k++) {
						if (cn.isInSamePartition.get((value + k)%numServers)) {
							serverRead.add((value + k)%numServers);
						}
					}

					//if at least one server is in the same partition with the client
					if (serverRead.size() > 0) {

						//randomly select one server and send request to that server
						int serverReadId = serverRead.get(random.nextInt(serverRead.size()));
						try {
							clientList.get(serverReadId).outToServer
									.writeBytes(inputSentence + "\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.out
								.println("read failed because files are not stored in the same partition");
					}
				} else {
					System.out.println("unrecognized two words input from user, please enter again");

				}
			}

			// send insert or update operation to the server
			else if (numTokens == 3) {
				String operation = st.nextToken();
				if (operation.equals("insert") || operation.equals("update")) {
					String fileName = st.nextToken();
					int location = hash(fileName);
					int numOfServersInSameComponent = 0;

					for (int k = 0; k < 3; k++) {
						if (cn.isInSamePartition.get((location + k)
								% numServers)) {
							numOfServersInSameComponent++;
						}
					}

					if (numOfServersInSameComponent >= 2) {
						for (int k = 0; k < 3; k++) {
							if (cn.isInSamePartition.get((location + k)
									% numServers)) {
								try {
									clientList.get((location + k) % numServers).outToServer
											.writeBytes(inputSentence + "\n");
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					} else {
						System.out
								.println(operation
										+ " denied because servers in same component is "
										+ numOfServersInSameComponent);
					}
				} else {
					System.out
							.println("unrecognized 3 words input from user, please enter again");
				}
			} else {
				System.out.println("unrecognized input from user, please enter again");
			}
		}
	}

	//hash function, which takes input a string and outputs a nubmer between 0 and 6
	private static int hash(String fileName) {
		int hash = 0;
		for (int i = 0; i < fileName.length(); i++) {
			hash = fileName.charAt(i) + (hash << 6) + (hash << 16) - hash;
		}
		return (hash & 0x7FFFFFFF) % 7;
	}
}
