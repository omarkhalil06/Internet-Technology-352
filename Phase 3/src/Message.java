/**
 * 
 * @author Omar Khalil ok77
 *
 */

public class Message {
	
	public static final byte KEEP_ALIVE = -1;
	public static final byte CHOKE = 0;
	public static final byte UNCHOKE = 1;
	public static final byte INTERESTED = 2;
	public static final byte UNINTERESTED = 3;
	public static final byte HAVE = 4;
	public static final byte BITFIELD = 5;
	public static final byte REQUEST = 6;
	public static final byte PIECE = 7;
	
	private int length;
	private byte id;
	protected byte[] message;	
	
	public Message(int length, byte id) {
		this.length = length;
		this.id = id;
		this.message = new byte[length + 4];		

		switch(id) {
			case(CHOKE):
				System.arraycopy(intToBytes(1), 0, this.message, 0, 4);
				this.message[4] = id;
				break;
			case(UNCHOKE):
				System.arraycopy(intToBytes(1), 0, this.message, 0, 4);
				this.message[4] = id;
				break;
			case(INTERESTED):
				System.arraycopy(intToBytes(1), 0, this.message, 0, 4);
				this.message[4] = id;
				break;
			case(UNINTERESTED):
				System.arraycopy(intToBytes(1), 0, this.message, 0, 4);
				this.message[4] = id;
				break;
		}
		
	}

	/**
	 * Encodes integers as 4-bytes big-endian
	 * 
	 * @param i
	 * @return byte[] This returns 4-byte big-endian array
	 */
	public static byte[] intToBytes(int i) {
		byte[] arr = new byte[4];

		arr[0] = (byte) (i >> 24);
		arr[1] = (byte) (i >> 16);
		arr[2] = (byte) (i >> 8);
		arr[3] = (byte) (i);

		return arr;
	}

	public byte getID() {
		return id;
	}

	public int getLength() {
		return length;
	}
}