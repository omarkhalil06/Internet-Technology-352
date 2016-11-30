/**
 * 
 * @author Omar Khalil ok77
 *
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.net.ServerSocket;
import java.security.SecureRandom;

import givenTools.Bencoder2;
import givenTools.BencodingException;
import givenTools.TorrentInfo;

public class Tracker implements Runnable {
	
	private final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {'p','e','e','r','s'});
	private final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {'i','n','t','e','r','v','a','l'});
	private final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] {'i','p'});
	private final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {'p','e','e','r',' ','i','d'});
	private final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {'p','o','r','t'});
	private final ByteBuffer KEY_MIN_INTERVAL = ByteBuffer.wrap(new byte[] {'m','i','n',' ','i','n','t','e','r','v','a','l'});
	
	public byte[] info_hash;			// 20 byte hash of the bencoded form of the info value from the metadata file
	public String peer_id;				// string (length 20) in which downloader uses as ID, randomly generated for each downloader
	public int port;					// port in which peer is listening on (6881-6889)
	public int uploaded = 0;			// total amount uploaded so far, encoded in base-10 ascii
	public int downloaded = 0;			// total amount downloaded so far, encoded in base-10 ascii
	public int left;					// number of bytes this peer still has to download, encoded in base-10 ascii 
	public int interval = 0;			// maps to number of seconds the downloader should wait between reqular rerequests
	public static ArrayList<Peer> peers;// maps to a list of dictionaries corresponding to peers
	public TorrentInfo torrent_info;	// torrent information	
	public String event;				// key that maps to STARTED, COMPLETED, or STOPPED
	
	private HashMap<ByteBuffer, Object> decoded_response;
	private boolean running = true;
	public boolean downloaded_pieces[];
	public static byte[] downloaded_file;
	
	public Tracker(TorrentInfo torrent_info) {
		this.peer_id = generateRandomID();
		this.port = getPort();
		this.left = torrent_info.file_length;
		this.info_hash = torrent_info.info_hash.array();
		this.torrent_info = torrent_info;
		this.downloaded_pieces = new boolean[torrent_info.piece_hashes.length];
		Arrays.fill(downloaded_pieces, false);
		Tracker.downloaded_file = new byte[torrent_info.file_length];
	}
	
	public void run() {
		if (interval > 180) {
			interval = 180;
		}
		
		int timer = interval;
		while (running) {
			try {
				Thread.sleep(1000L);
				timer--;
			} catch (InterruptedException e) {
				// do nothing
			}
			
			if (event.equals("stopped")) {
				System.out.println("Download stopped!");
				System.out.println("Downloaded: " + downloaded + " bytes");
				System.out.println("Left: " + left);
				stopRunning();
			} else if (event.equals("completed")) {
				System.out.println("Download complete!");
				System.out.println("Downloaded: " + downloaded + " bytes");
				System.out.println("Left: " + left);
				stopRunning();
			}

			if (timer == 0) {
				System.out.println("Tracker updated!");
				System.out.println("Downloaded: " + downloaded + " bytes");
				System.out.println("Left: " + left);
				sendRequest("");
				timer = interval;
			}

		}

	}
	
	/**
	 * Updates the download file array everytime a piece gets verified
	 * 
	 * @param piece
	 * @param pos
	 * @param length
	 */
	public static void updateFile(byte[] piece, int pos, int length) {
		System.arraycopy(piece, 0, downloaded_file, pos, piece.length);
	}
	
	/**
	 * Sends an HTTP GET request to the tracker with the following key/value pairs. Decodes response using
	 * Bencoder2.
	 * 
	 * @param event Announcement for the tracker. Maps to STARTED, STOPPED, or COMPLETED
	 */
	@SuppressWarnings("unchecked")
	public void sendRequest(String event) {
		String info_hash_string = "?info_hash=" + byteToHexString(this.info_hash);
		String peer_id_string = "&peer_id=" + byteToHexString(this.peer_id.getBytes());
		String port_string = "&port=" + this.port;
		String uploaded_string = "&uploaded=" + this.uploaded;
		String downloaded_string = "&downloaded=" + this.downloaded;
		String left_string = "&left=" + this.torrent_info.file_length;
		String event_string = "&event=" + event;
		this.event = event;
		
		String announce_URL = torrent_info.announce_url.toString();
		
		announce_URL += info_hash_string + peer_id_string + port_string + uploaded_string + downloaded_string +
				left_string + event_string;
		
		//System.out.println("URL: " + announce_URL);
		
		try {
			URL url = new URL(announce_URL);
			
			byte[] response = getResponse(url);
			try {
				decoded_response = (HashMap<ByteBuffer, Object>) Bencoder2.decode(response);
			} catch (BencodingException e) {
				System.err.println("Unable to decode tracker response.");
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			System.err.println("Unable to create URL.");
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Retrieve GET response from url.
	 * 
	 * @param url Url used to send HTTP GET request
	 * @return byte[] This returns the response in a byte array
	 */
	public byte[] getResponse(URL url) {
		byte[] response = null;
		
		try {
			URLConnection con = (URLConnection) url.openConnection();
			DataInputStream din = new DataInputStream(con.getInputStream());
			int size = con.getContentLength();
			response = new byte[size];
			din.readFully(response);
			din.close();
		} catch (IOException e) {
			System.err.println("Unable to get response from tracker.");
			e.printStackTrace();
		}
		
		return response;
	}
	
	
	/**
	 * Retrieve a list of peers generated from the GET response.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void getPeers() {
		peers = new ArrayList<Peer>();
		ArrayList<Object> peer_list = (ArrayList<Object>) decoded_response.get(KEY_PEERS);

		for (Object o : peer_list) {
			try {
				HashMap<ByteBuffer, Object> peer_info = (HashMap<ByteBuffer, Object>) o;
				ByteBuffer temp_id = (ByteBuffer) peer_info.get(KEY_PEER_ID);
				String s = new String(temp_id.array(), "ASCII");
				byte[] id = temp_id.array();
				ByteBuffer temp_ip = (ByteBuffer) peer_info.get(KEY_IP);
				String ip = new String(temp_ip.array(), "ASCII");
				int peer_port = (Integer) peer_info.get(KEY_PORT);
				
				if (s.contains("RU")) {
					peers.add(new Peer(id, ip, peer_port, info_hash, torrent_info));
				}
			} catch (IOException e) {
				System.err.println("Unable to create peer list");
				e.printStackTrace();
			}
		}
		
		
		if (decoded_response.get(KEY_MIN_INTERVAL) == null) {
			this.interval = ((Integer) decoded_response.get(KEY_INTERVAL)) / 2;
		} else {
			this.interval = (Integer) decoded_response.get(KEY_MIN_INTERVAL);
		}
		
	}

	/**
	 * Takes in a byte array and returns it as a hex string.
	 * 
	 * @param bytes 
	 * @return String in hexadecimal  format
	 */
	public static String byteToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%%%02x", b));
		}
		return sb.toString();
	}

	/**
	 * Finds a port to connect to from 6881 to 6889.
	 * 
	 * @return int This returns the port number to establish connections
	 */
	public static int getPort() {
		ServerSocket listener = null;
		for (int port = 6881; port < 6890; port++) {
			try {
				listener = new ServerSocket(port);
				listener.close();
				System.out.println("Connected to port " + port);
				return port;
			} catch (IOException e) {
				System.err.println("Port " + port + " is not available. Trying next port ...");
				//e.printStackTrace();
			}
		}
		return -1;
	}

	/**
	 * Generates a random ID for the client to use.
	 * 
	 * @return String 
	 */
	public static String generateRandomID() {
		char[] result = new char[20];
		char[] alphanumeric = "QWERTYUIOPLKJHGFDSAZXCVBNMqwertyuioplkjhgfdsazxcvbnm1234567890".toCharArray();
		Random random = new SecureRandom();
		for (int i = 0; i < 20; i++) {
			result[i] = alphanumeric[random.nextInt(alphanumeric.length)];
		}
		return new String(result);
	}
	
	public void stopRunning() {
		running = false;
	}
	
}