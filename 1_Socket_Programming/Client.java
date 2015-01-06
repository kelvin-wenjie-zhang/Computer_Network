//Client
import java.io.*;
import java.util.*;
import java.net.*;
/**
 * The client source code.
 * @author Wenjie Zhang, UNI: wz2261
 *
 */
public class Client {
	// the server port number
	public static int PORT;
	// the printwriter is responsible for the output message of this socket client
	private static PrintWriter out;
	// the buffered reader is responsible for the input message from the server
	private static BufferedReader in;
	// the buffered reader to read message from the user command line
	private static BufferedReader std;
	// block the user if s/he has 3 failure trials log in
	private static final int BLOCK_TIME = 60;
	// time out for inactive user
	private static final int TIME_OUT = 30;

	public static void main(String[] args) throws Exception{
		
		// if there's no IP address provided, simply exit the program
		if(args.length != 2) {
			System.out.println("Please provide an IP address and the port number at the command line.");
			System.exit(0);
		}
		
		String serverAddress = new String(args[0]);

		// read the server port number
		String port_no = new String(args[1]);
		try{
			PORT = Integer.parseInt(port_no);
		}catch (NumberFormatException nfe){
			System.out.println("Please enter a valid port number.");
			System.exit(0);
		}

		// establish the client socket
		Socket client = new Socket(serverAddress, PORT);

		try {
			// initialize the print writer and buffered reader
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out = new PrintWriter(client.getOutputStream(),true);
			std = new BufferedReader(new InputStreamReader(System.in));
			// infinite loop to get message from the server
			// and send message to the server
			
			// create a ClientThread instance to handle the output from
			// the server
			ClientThread client_thread = new ClientThread(client, in);
			client_thread.start();

			// as long as the thread is running,
			// then we keep reading input from the user,
			// and then send it to the server.
			while (client_thread.isRunning()) {
				out.println(std.readLine());
			}
			
		}catch (IOException e){
			// when the program gets here, the user is blocked
			System.out.println("Please wait " + BLOCK_TIME +" seconds.");
			// block the user from this IP address
			Thread.sleep(BLOCK_TIME * 1000);
			System.out.println("Now you can re-connect to the server.");
		}finally{
			// close the I/O buffer
			in.close();
			out.close();
			std.close();

			// close the socket
			client.close();

			// indicate that the socket is closed
			System.out.println("Log out successfully.");

			// close the terminal
			System.exit(0);
		}
		
	}

	/**
	* This client thread class is to handle the output from the server.
	* As long as the server keeps sending, we keep reading it and print it
	* to the user concole.
	* @author Wenjie Zhang, UNI: wz2261
	*
	*/
	public static class ClientThread extends Thread {
		private Socket socket;
		// a buffered reader reference to the input of this socket
		private BufferedReader reader;
		// indicate whether or not this thread is running
		private boolean isRunning = true;

		public ClientThread(Socket socket, BufferedReader reader) {
			this.socket = socket;
			this.reader = reader;
		}

		public void run(){
			while(isRunning){
				try{
					String tmp = reader.readLine();
					if(tmp.startsWith("LOGOUT"))
						isRunning = false;
					else
						System.out.println(tmp);
				}catch(IOException e){
					System.out.println("Exception Caught.");
					isRunning = false;
				}
			}
		}
		// kill the thread
		public void kill(){
			isRunning = false;
		}
		// tell outside if this thread is running
		public boolean isRunning(){
			return isRunning;
		}
	}

}