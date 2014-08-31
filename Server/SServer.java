/*
 *A thread class instantiated when a sever is acted as a server to other servers. 
 *It will also proecess messages received from clients.
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.ArrayList;

public class SServer extends Thread {
	public Socket socket;
	public BufferedReader inFromClient;
	public DataOutputStream outToClient;

	//an instance of the ServerNode class
	public ServerNode sn;
	public static ArrayList<ArrayList<String>> pendingUpdates;
	public static ArrayList<ArrayList<String>> pendingUpdatesPrimary;

	//constructor
	public SServer(Socket clientSoc, ServerNode sn) {
		this.socket = clientSoc;
		this.sn = sn;
		pendingUpdates=new ArrayList<>();
		pendingUpdatesPrimary=new ArrayList<>();
		try {
			inFromClient = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			outToClient = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static int hash(String fileName) {
		int hash = 0;
        for(int i=0;i<fileName.length();i++)
		{
        	hash = fileName.charAt(i) + (hash << 6) + (hash << 16) - hash;
		}
        return (hash & 0x7FFFFFFF)%7;
	}

	@Override
	public void run() {
		String receivedSentence = null;
		while (true) {
			try {
				receivedSentence = inFromClient.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (receivedSentence != null) {
				StringTokenizer st = new StringTokenizer(receivedSentence);
				int numTokens = st.countTokens();

				if (numTokens == 2) {
					String operation = st.nextToken();
					if (operation.equals("read")) {
						String fileName = st.nextToken();
						System.out.println("read request received from client");
						if (sn.hashmap.containsKey(fileName)) {
							ServerNode.OBject object = sn.hashmap.get(fileName);
							int value = object.value;
							try {
								outToClient.writeBytes(value + "\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							System.out.println("file "+fileName+" does not exist");	
							try {
								outToClient.writeBytes("file does not exist"+"\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					} else {
						System.out
								.println("unrecognized two words command from client");
					}
				} else if (numTokens == 3) {
					String operation = st.nextToken();
					if (operation.equals("insert")) {
						String fileName = st.nextToken();
						if(sn.hashmap.containsKey(fileName)==true)
						{
							System.out.println("file "+fileName+" already exist .. no inserts made");
							if(hash(fileName)==sn.nodeID  || 
								(!SClient.partArr.contains(hash(fileName)) && ((hash(fileName)+1)%7)==sn.nodeID))
							{
								try {
									outToClient.writeBytes("Specified object already exists. Please use the update option to make changes." + "\n");
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							continue;
						}
						int initVal = Integer.parseInt(st.nextToken());
						ServerNode.OBject object = sn.new OBject(initVal);
						sn.hashmap.put(fileName, object);
						System.out.println("insert completed, file name of "
								+ fileName + " with value " + initVal + " is inserted");
					} else if (operation.equals("update")) {
						String fileName = st.nextToken();
						if(sn.hashmap.containsKey(fileName)==false)
						{
							System.out.println("File does not exist in the hashmap... no updates made");
							if(hash(fileName)==sn.nodeID  || 
								(!SClient.partArr.contains(hash(fileName)) && ((hash(fileName)+1)%ServerNode.numServers)==sn.nodeID))
							{
								try {
									outToClient.writeBytes("Specified object does not exist. Please insert the object before making any updates." + "\n");
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							continue;
						}

						int newVal = Integer.parseInt(st.nextToken());
						if(hash(fileName)==sn.nodeID || 
								(!SClient.partArr.contains(hash(fileName)) && ((hash(fileName)+1)%ServerNode.numServers)==sn.nodeID)) {	

							//send message to secondary replicas specifying the order of execution
							ArrayList<String> temp=new ArrayList<String>();
							temp.add(fileName);
							temp.add(String.valueOf(newVal));
							pendingUpdatesPrimary.add(temp);
						} else {						
							ArrayList<String> temp=new ArrayList<String>();
							temp.add(fileName);
							temp.add(String.valueOf(newVal));
							pendingUpdates.add(temp);
						}					
					}
				}
			}
		}
	}
}
