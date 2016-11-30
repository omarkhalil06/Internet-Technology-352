/**
 * 
 * @author Omar Khalil ok77
 * 
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UploadPeer implements Runnable {
	
	private DataInputStream din;
	private DataOutputStream dout;
	private byte[] info_hash;
	private Tracker tracker = RUBTClient.tracker;
	private Socket socket;
	private String ip;
	
	public UploadPeer(byte[] info_hash, Socket socket) {
		this.info_hash = info_hash;
		this.socket = socket;
	}
	
	public void run() {
		ip = socket.getInetAddress().getHostAddress();
		
		try {
			dout = new DataOutputStream(socket.getOutputStream());
			din = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			System.err.println("Data streams creation error");
			return;
		}
		byte[] received_handshake = new byte[68];
		
		try {
			din.readFully(received_handshake);
		} catch (IOException e) {
			System.err.println("Cannot receive handshake");
			closeStreams();
			return;
		}
		
		byte[] check = Arrays.copyOfRange(received_handshake, 28,48);
		if (!Arrays.equals(info_hash, check)) {
			System.err.println("Handshake is not equal");
			closeStreams();
			return;
		} 
		
		// send handshake back
		byte[] handshake_message = Utils.createHandshake();
		try {
			dout.write(handshake_message);
			dout.flush();
		} catch (IOException e) {
			System.err.println("Unable to send back handshake");
			closeStreams();
			return;
		}
		
		// send pieces downloaded
		System.out.println("Sending pieces downloaded");
		int i = 0;
		while (i < tracker.downloaded_pieces.length && tracker.downloaded_pieces[i]) {
			try {
				Message have = new Have(Tracker.downloaded_file[i]);
				dout.write(have.message);
			} catch (IOException e) {
				System.err.println("Unable to send have message");
				closeStreams();
				return;
			}
			i++;
		}
		
		// wait 5 seconds for peer to send message
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			System.out.println("This peer cannot sleep");
		}
	
		// read interested message
		boolean is_interested = false;
		while (!is_interested) {
			try {
				byte[] interested = new byte[4];
				din.readFully(interested);
				int length = bytesToInt(interested);
				System.out.println("Length: " + length);
				
				int id = din.readByte();
				System.out.println("ID: " + id);
				
				if (id == 2) {
					is_interested = true;
				}
				
				if (id == 3) {
					System.out.println("Peer " + ip + " is not interested");
					return;
				}
				
				if (id == 5) {
					System.out.println("Received bitfield message");
					closeStreams();
					return;
				}
				
				if (RUBTClient.stop) {
					return;
				}
			} catch (IOException e) {
				System.err.println("Could not read interested message"); 
				closeStreams();
				return;
			}
		}
		System.out.println("Interested message received!");

		// send unchoke message
		try {
			Message unchoke = new Message(1, (byte) 1);
			dout.write(unchoke.message);
			dout.flush();
		} catch (IOException e) {
			System.err.println("Unable to send unchoke message");
			closeStreams();
		}
		System.out.println("Unchoked peer!");
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			System.out.println("This peer cannot sleep");
		}
		
		// read request message
		int piece_length = 16384;
		while (!RUBTClient.stop) {
			byte[] request = new byte[4];
			int length = 0;
			int id = 0;
			try {
				din.readFully(request);
				length = bytesToInt(request);
				System.out.println("Length2: " + length);
				id = din.readByte();
				System.out.println("ID2: " + id);
				
			} catch (IOException e) {
				System.err.println("Cannot read request");
				closeStreams();
				return;
			}
			
			int request_index = 0;
			int request_begin = 0;
			int request_length = 0;
			if (id == 6) {
				try {
					request_index = din.readInt();
					request_begin = din.readInt();
					request_length = din.readInt();
				} catch (IOException e) {
					System.err.println("Request error");
					closeStreams();
				}
				
				System.out.println("Index: " + request_index);
				System.out.println("Begin: " + request_begin);
				System.out.println("Length: " + request_length);
				
				if (request_length > piece_length) {
					System.out.println("Request size > 16384");
					closeStreams();
					return;
				}
				
				if (tracker.downloaded_pieces[request_index]) {
					System.out.println("Uploading to " + ip);
					byte[] request_block = new byte[request_length];
					Message piece = new Piece(request_index, request_begin,request_block);
					try {
						dout.write(piece.message);
						dout.flush();
					} catch (IOException e) {
						System.err.println("Upload failed");
						closeStreams();
					}
					
					System.out.println("Uploaded Piece #" + request_index + " to " + ip);
				}
			}
					
		}
		
		closeStreams();
		
	}
	
	/**
	 * Closes socket, datainputstream, and dataoutputstream
	 * 
	 */
	public void closeStreams() {
		System.out.println("Disconnecting from " + ip);
		try {
			dout.close();
			din.close();
			socket.close();
		} catch (IOException e) {
			System.err.println("Closing error");
		}
	}
	
	/**
	 * Convertes 4-bytes big endian to integer
	 * 
	 * @param bytes
	 * @return int
	 */
	public int bytesToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	}

}
