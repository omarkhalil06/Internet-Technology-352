/**
 * 
 * @author Omar Khalil ok77
 *
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import givenTools.BencodingException;
import givenTools.TorrentInfo;

public class RUBTClient {

	public static String save_file;
	public static Tracker tracker;

	public static void main(String[] args) {
		
		String torrent;
		byte[] torrent_file = null;
		TorrentInfo torrent_info = null;
		
		// check command line arguments
		if (args.length != 2) {
			System.err.println("USAGE: java RUBTClient <torrent-file-to-read> <file-to-be-saved>");
			return;
		} else {
			torrent = args[0];
			save_file = args[1];
		}
		
		// read torrent file into byte array
		Path path = Paths.get(torrent);
		try {
			torrent_file = Files.readAllBytes(path);
		} catch (IOException e) {
			System.err.println("File not found.");
			//e.printStackTrace();
		}
		
		// convert torrent bytes into TorrentInfo
		try {
			torrent_info = new TorrentInfo(torrent_file);
		} catch (BencodingException e) {
			System.err.println("Could not process torrent.");
			//e.printStackTrace();
		}
		
		tracker = new Tracker(torrent_info);
		tracker.sendRequest("");
		ArrayList<Peer> peers = tracker.getPeers();

		// calculate average RTT (over 10 times) for each peer
		double first = 0;
		Peer fastest_peer = null;
		for (Peer p : peers ) {
			//System.out.println("IP address: " + p.getIP() + " Port: " + p.getPort());
			double rtt = p.calculateAverageRTT();
			if (first == 0) { 		// for first iteration
				first = rtt;
				fastest_peer = p;
			} else if (rtt < first)	{
				first = rtt;
				fastest_peer = p;
			}	
		}

		//System.out.println("IP " + fastest_peer.getIP());
		//System.out.println("Time: " + first);
		//System.out.println("Fastest peer: " + new String(fastest_peer.getPeerID()));
		Thread thread1 = new Thread(fastest_peer);
		thread1.start();

		Thread thread2 = new Thread(tracker);
		thread2.start();
	}

}