/**
 * 
 * @author Omar Khalil ok77
 *
 */

import java.net.*;
import java.io.*;
import java.util.*;
import givenTools.TorrentInfo;

public class Peer implements Runnable {

	private byte[] peer_id;
	private byte[] info_hash;
	private String ip;
	private int port;
	private Socket socket;
	private DataInputStream din;
	private DataOutputStream dout;
	private TorrentInfo torrent_info;
	private FileOutputStream fout;
	
	private Tracker tracker = RUBTClient.tracker;
	private static byte[] downloaded_file = Tracker.downloaded_file;
	
	public static boolean all_pieces_downloaded = false;
	public static boolean user_stop = false;
	public boolean download_complete;
	public boolean partial_complete = false; 	// used to print download time only once
	public boolean already_downloaded = false;
	
	
	public Peer(byte[] peer_id, String ip, int port, byte[] info_hash, TorrentInfo torrent_info) {
		this.peer_id = peer_id;
		this.ip = ip;
		this.port = port;
		this.info_hash = info_hash;
		this.torrent_info = torrent_info;
	}

	public void run() {

		try {
			// connects to peer
			socket = new Socket(ip, port);
			dout = new DataOutputStream(socket.getOutputStream());
			din = new DataInputStream(socket.getInputStream());
			System.out.println("Connecting to peer from " + ip + " port " + port);
			
			if (handshake()) {				
				Message interested = new Message(1, (byte) 2);
				
				// wait for unchoke message
				boolean unchoked = false;
				while (!unchoked) {
					sendMessage(interested.message);
					
					int length = din.readInt();
					byte id = din.readByte();
					Message response = getMessage(id, length);
					if (response.getID() == Message.UNCHOKE) {
						unchoked = true;
					}
				}

				// download file
				double start = System.nanoTime();
				download();
				
				if (!partial_complete) {
					createSaveFile();
					double finish = System.nanoTime();
					double elapsed_time = (finish - start) / 1000000000;
					System.out.println("Download time: " + elapsed_time + " seconds");
					all_pieces_downloaded = true;
					saveFile();

				}
				
				if (user_stop) {
					RUBTClient.num_peers++;
					if (RUBTClient.num_peers == 3) {
						createSaveFile();
						double finish = System.nanoTime();
						double elapsed_time = (finish - start) / 1000000000;
						System.out.println("Download time: " + elapsed_time + " seconds");
						tracker.sendRequest("stopped");
						saveFile();
					}
				}
				
				if (already_downloaded) {
					System.out.println("File has been already downloaded");
				}
			} else {
				System.err.println("Handshake failed!");
				return;
			}
		} catch (IOException e) {
			System.err.println("Connection failed");
		}

		// close sockets and stream readers
		try {
			socket.close();
			din.close();
			dout.close();
			if (fout != null) {
				fout.close();
			}
		} catch (IOException e) {
			System.err.println("Cannot close");
		}
	}
	
	/**
	 * Creates a save file
	 * 
	 */
	public void createSaveFile() {
		try {
			fout = new FileOutputStream(new File(RUBTClient.save_file));
		} catch (IOException e) {
			System.err.println("File error");
		}	
	}
	
	/**
	 * Saves the file 
	 * 
	 */
	public void saveFile() {
		try {
			fout.write(downloaded_file);
		} catch (IOException e) {
			System.err.println("Error in saving file");
			e.printStackTrace();
		}
	}

	/**
	 * Generates a handshake to send to the peer.
	 * 
	 * @return True if handshake between peers is equal. Otherwise, return false.
	 */
	public boolean handshake() {
		byte[] handshake_message = Utils.createHandshake();
		
		// send message and compare handshakes
		try {
			dout.write(handshake_message);
			dout.flush();
			
			byte[] received_handshake = new byte[68];
			din.read(received_handshake);

			byte[] check = Arrays.copyOfRange(received_handshake, 28,48);
			if (Arrays.equals(info_hash, check)) {
				return true;
			} else {
				System.err.println("Not equal");
			}
		} catch (IOException e) {
			System.err.println("Handshake failed.");
		}

		return false;
	}

	/**
	 * Sends a message to the peer.
	 * 
	 * @param message
	 */
	public void sendMessage(byte[] message) {
		try {
			dout.write(message);
			dout.flush();
		} catch(IOException e) {
			System.err.println("Unable to send message.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Retrieves a message from the peer.
	 * 
	 * @param id 
	 * @param length 
	 * @return Message
	 */
	public Message getMessage(byte id, int length) {
		try {
			switch(id) {
			case(Message.CHOKE):
				return new Message(1, id);
			case(Message.UNCHOKE):
				return new Message(1, id);
			case(Message.INTERESTED):
				return new Message(1, id);
			case(Message.UNINTERESTED):
				return new Message(1, id);
			case(Message.HAVE):
				int peer_index = din.readInt();
				return new Have(peer_index);
			case(Message.BITFIELD):
				byte[] bitfield = new byte[length - 1];
				din.readFully(bitfield);
				return new Bitfield(bitfield);
			case(Message.PIECE):
				int index = din.readInt();
				int begin = din.readInt();
				byte[] block = new byte[length];
				return new Piece(index, begin, block);
			} 
		} catch (IOException e) {
			System.err.println("Getting message error");
		}
		
		return null;

	}
	
	/**
	 * Downloads all pieces and verifies its SHA-1 hash value. Client can enter <quit> 
	 * to stop download whenever and pieces will still be saved.
	 * 
	 */
	public void download() {	
		int count = Utils.index;
		int pieces = torrent_info.piece_hashes.length;
		download_complete = false;
		
		Thread thread = new Thread(new keepAlive());
		thread.start();	
		
		int temp = count;
		while (count != pieces) {	
			while (tracker.downloaded_pieces[temp]) {
				temp++;
			}
			count = temp;
			tracker.downloaded_pieces[count] = true;
			
			byte[] piece;
			int blocks = torrent_info.piece_length / 16384;
			int begin = 0;
			
			if (count == pieces - 1) {
				if (torrent_info.file_length % torrent_info.piece_length == 0) {
					piece = new byte[torrent_info.piece_length];
				} else {
					piece = new byte[torrent_info.file_length % torrent_info.piece_length];
				}
			} else {
				piece = new byte[torrent_info.piece_length];
			}
			
			int i = 0;
			while (i < blocks) {
				
				int block_length = 16384;
				
				if (i == blocks - 1 && piece.length % 16384 != 0) {
					if (piece.length % 16384 != 0) {
						block_length = piece.length % 16384;
					}
				}
				
				try {
					Message request = new Request(count, begin, block_length);
					dout.write(request.message);
					dout.flush();
					
					int piece_length = din.readInt() - 9;
					byte id = din.readByte();
					int piece_index = din.readInt();
					int piece_begin = din.readInt();
					
					if (id == Message.PIECE) {
						byte[] block = new byte[piece_length];
						din.readFully(block);
						System.arraycopy(block, 0, piece, piece_begin, piece_length);
						begin += 16384;
					}
				} catch (IOException e) {
					System.err.println("Error in request message");
					//e.printStackTrace();
				}
				
				i++;
			}
			
			if (Utils.verifySHA1(piece, count)) {
				tracker.downloaded += piece.length;
				tracker.left -= piece.length;
				Tracker.updateFile(piece, count * torrent_info.piece_length, piece.length);
				System.out.println("Piece #" + count + " from " + ip + " verified!");
			}
			
			count++;
			if (count == 509 || count == 510) {
				download_complete = true;
				partial_complete = true;
				return;
			}

			// retrieve user input
			if (RUBTClient.stop) {
				download_complete = true;
				partial_complete = true;
				user_stop = true;
				return;
			}

		}
		
		download_complete = true;
		tracker.sendRequest("completed");

	}
	
	public byte[] getPeerID() {
		return peer_id;
	}

	public int getPort() {
		return port;
	}

	public String getIP() {
		return ip;
	}
	
	/**
	 * Class used to send keep alive messages to peer every 2 minutes to prevent peers from closing
	 * connections.
	 * 
	 */
	public class keepAlive implements Runnable {
		
		boolean running = true;

		public void run() {
			int timer = 120;
			while (running) {
				try {
					Thread.sleep(1000L);
					timer--;
				} catch (InterruptedException e) {
					// do nothing
				}
				
				if (timer == 0) {
					Message alive = new Message(0, Message.KEEP_ALIVE);
					try {
						dout.writeInt(0);
					} catch (IOException e) {
						System.err.println("Keep alive message error");
					}
					timer = 0;
				}
				
				if (download_complete) {
					running = false;
				}
			}
			
		}
		
	}

}