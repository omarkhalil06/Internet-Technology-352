import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;

public class EchoServer {

	/**
	 * Runs the server on a port given by the user. When the client connects,
	 * it sends the server a string, in which the server sends back the same
	 * string, but inverted. Closes the connection once the client inputs either
	 * "#" or "$" on the command line.
	 *
	 */
	public static void main(String[] args) throws IOException {
		int port = Integer.parseInt(args[0]);
		ServerSocket listener = new ServerSocket(port); // creates a server socket to listen to a specific port

		try {
			Socket socket = listener.accept(); // creates the socket

			try {
				// IO streams for data input
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				// takes the string and sends the inverted string back to the client
				String msg = msg = in.readLine();
				while (msg != null && !msg.equals("#") && !msg.equals("$")) {
					String newMsg = invert(msg);
					System.out.println("Message from client: " + msg);
					out.println("Inverted Message: " + newMsg);	
					msg = in.readLine();			
				} 
				
				// closing IO streams
				in.close();
				out.close();
			} finally {
				// closing socket
				socket.close();
			}
		
		} finally {
			// closing server socket
			listener.close();
		}
		
	}

	/**
	 * Takes a string as input and inverts it.
	 *
	 * @param msg
	 * @return Inverted string
	 */
	public static String invert(String msg) {
		int length = msg.length();

		String inverted = "";
		for (int i = length - 1; i >= 0; i--) {
			inverted = inverted + msg.charAt(i);
		}

		return inverted;
	}

}
