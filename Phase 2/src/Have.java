/**
 * 
 * @author Omar Khalil ok77
 *
 */

public class Have extends Message {

	private int peer_index;

	public Have(int peer_index) {
		super(5, (byte) 4);
		this.peer_index = peer_index;
		System.arraycopy(Message.intToBytes(5), 0, this.message, 0, 4);
		message[4] = (byte) 4;
		
		// payload
		System.arraycopy(Message.intToBytes(peer_index), 0, this.message, 5, 4);
	}

}