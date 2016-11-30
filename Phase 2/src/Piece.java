/**
 * 
 * @author Omar Khalil ok77
 *
 */

public class Piece extends Message {
	
	public Piece(int index, int begin, byte[] block) {
		super(9 + 16384, (byte) 7);
		int length = 9 + 16384;
		this.message = new byte[length + 4];
		System.arraycopy(intToBytes(length), 0, this.message, 0, 4);
		this.message[4] = (byte) 7;

		// payload
		System.arraycopy(intToBytes(index), 0, this.message, 5, 4);
		System.arraycopy(intToBytes(begin), 0, this.message, 9, 4);
		System.arraycopy(block, 0, this.message, 13, length - 9);
		
	}
	
}