/**
 * 
 * @author Omar Khalil ok77
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import givenTools.BencodingException;
import givenTools.TorrentInfo;

public class RUBTClient {

	public static String save_file;
	public static Tracker tracker;
	public static boolean stop;
	public static int num_peers = 0;

	public static void main(String[] args) {
		
		String torrent;
		byte[] torrent_file = null;
		TorrentInfo torrent_info = null;
		stop = false;
		
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
		}
		
		// convert torrent bytes into TorrentInfo
		try {
			torrent_info = new TorrentInfo(torrent_file);
		} catch (BencodingException e) {
			System.err.println("Could not process torrent.");
		}
				
		tracker = new Tracker(torrent_info);
		tracker.sendRequest("started");
		tracker.getPeers();
		ArrayList<Peer> peers = Tracker.peers;
		
		int number_of_threads = peers.size() + 2; // one for tracker and server
		ExecutorService executor = Executors.newFixedThreadPool(number_of_threads);
		
		Utils.checkFileAndGetIndex();
		if (tracker.left == 0) {
			tracker.sendRequest("completed");
			System.out.println("File is already downloaded");
			System.out.println("Downloaded: " + tracker.downloaded + " bytes");
			System.out.println("Left: " + tracker.left);
		} else {
			// run each peer if download is not completed yet
			for (Peer p : peers) {
				Runnable peer = p;
				executor.execute(peer);
				
			}
			// make tracker runnable
			Runnable t = tracker;
			executor.execute(t);
		}

		// make RUBTServer runnable
		RUBTServer server = new RUBTServer(torrent_info.info_hash.array());
		Runnable s = server;
		executor.execute(s);
				
		// waits for user to quit
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter <quit> to stop download");
		while (!stop) {
			try {
				if (br.ready()) {
					if (br.readLine().equals("quit")) {
						stop = true;	
						System.out.println("Stopping program ... ");
					}
				}
			} catch (IOException e) {
				System.err.println("Input error");
			}
		}	
		
		executor.shutdown();
	}

}