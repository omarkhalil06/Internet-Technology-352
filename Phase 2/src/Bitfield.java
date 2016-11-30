/**
 * 
 * @author Omar Khalil ok77
 *
 */

public class Bitfield extends Message {

	public Bitfield(byte[] bitfield) {
		super(1 + bitfield.length, (byte) 5);
		this.message = new byte[1 + bitfield.length + 4];
		System.arraycopy(Message.intToBytes(1 + bitfield.length), 0, this.message, 0, 4);
		this.message[4] = (byte) 5;
		
		// payload
		System.arraycopy(bitfield, 0, this.message, 0, bitfield.length);	
	}

	
}