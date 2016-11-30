/**
 * 
 * @author Omar Khalil ok77
 *
 */

public class Request extends Message {

	public Request(int index, int begin, int length) {
		super(13, (byte) 6);
		this.message = new byte[13 + 4];
		System.arraycopy(Message.intToBytes(13), 0, this.message, 0, 4);
		this.message[4] = (byte) 6;

		// payload
		System.arraycopy(Message.intToBytes(index), 0, this.message, 5, 4);
		System.arraycopy(Message.intToBytes(begin), 0, this.message, 9, 4);
		System.arraycopy(Message.intToBytes(length), 0, this.message, 13, 4);
		
	}

}