/**
 * 
 * @author Omar Khalil ok77
 * 
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RUBTServer implements Runnable {
	
	private Tracker tracker = RUBTClient.tracker;
	private boolean running = true;
	private byte[] info_hash;
	
	public RUBTServer(byte[] info_hash) {
		this.info_hash = info_hash;
	}

	public void run() {
		// wait until everything gets set up correctly
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			System.out.println("This peer cannot sleep");
		}		
		
		ServerSocket server = null;
		Socket socket = null;
		ArrayList<String> uploaded_peers = new ArrayList<String>();
		
		// limit to 50
		ExecutorService executor = Executors.newFixedThreadPool(50);

		try {
			server = new ServerSocket(tracker.port);
			System.out.println("Server created on port: " + tracker.port);
		} catch (IOException e) {
			System.err.println("Server socket error");
			e.printStackTrace();
		}
		
		// periodically check every 3 seconds to see if user stops program
		try {
			server.setSoTimeout(3000);
		} catch (SocketException e1) {
			System.err.println("Timeout error");
		}
		
		while (running) {
			try {

				socket = server.accept();
				String temp = socket.getInetAddress().toString();
				String ip = temp.substring(1,  temp.length());
				boolean already_has_file = false;

				for (Peer p : Tracker.peers) {
					if (ip.equals(p.getIP())) {
						already_has_file = true;
					}
				}

				if (!already_has_file && !uploaded_peers.contains(ip)) {
					uploaded_peers.add(ip);
					System.out.println("Peer IP address: " + ip + " on port " + socket.getPort());
					UploadPeer peer = new UploadPeer(info_hash, socket);
					Runnable p = peer;
					executor.execute(p);
				} 		


			} catch (SocketTimeoutException e) {
				if (RUBTClient.stop) {
					stopRunning();
					try {
						server.close();
						if (socket != null) {
							socket.close();
						}
						executor.shutdown();
					} catch (IOException e1) {
						System.err.println("Closing error");
					}
					System.out.println("Server closed");
				}
			} catch (IOException e) {
				System.err.println("Cannot accept connection from " + socket.getInetAddress());
			}
			
		}	
		
	}

	public void stopRunning() {
		running = false;
	}

}
