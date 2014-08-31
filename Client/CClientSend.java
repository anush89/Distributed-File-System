/*
 *Class that initialize the sockets so that a client 
 *can send request to servers
 */

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class CClientSend extends Thread {
	public Socket socket;
	public ClientNode.Server server;
	public DataOutputStream outToServer;

	//constructor
	public CClientSend(Socket client, ClientNode.Server s) {
		this.socket = client;
		this.server = s;
		try {
			outToServer = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		//do nothing
	}

}
