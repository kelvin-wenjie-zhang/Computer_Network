//Server
import java.io.*;
import java.util.*;
import java.net.*;
/**
 * The server source code
 * @author Wenjie Zhang, UNI: wz2261
 *
 */
public class Server {
	// port number
	private static int PORT;
	// store all the client information
	private static ArrayList<user> clients;
	// the server name
	private static ServerSocket server;
	// print writer for the server
	private static PrintWriter out;
	// buffered reader for the server
	private static BufferedReader in;
	// the block time that is used after 3 failure attempt to log in
	private static final int BLOCK_TIME = 60;
	// a time variable to be used by a client to view other users who were online in the past one hour
	// notice that this is in "hours" unit
	private static final int LAST_HOUR = 1;
	// a time variable to be used for inactive user,
	// notice that this is in "miniutes" unit
	private static final int TIME_OUT = 30;

	/*
	 * The main function of the server. 
	 * It reads the text file and store all the information of the user into
	 * a clients array list.
	 * Then it waits for a socket connection and create a thread to handle.
	 */
	public static void main(String[] args) throws Exception{
		
		System.out.println("The server is now running...");

		// ensure the port number is on the command line
		if(args.length != 1){
			System.out.println("Please enter a port number at the command line.");
			System.exit(0);
		}

		// read the server port number
		String port_no = new String(args[0]);
		try{
			PORT = Integer.parseInt(port_no);
		}catch (NumberFormatException nfe){
			System.out.println("Please enter a valid port number.");
			System.exit(0);
		}

		// read the user_pass.txt file and store all the
		// user name and password into the arraylist
		clients = new ArrayList<user>();
		BufferedReader brd = new BufferedReader(new FileReader("user_pass.txt"));
		while(brd.ready()){
			String record = new String(brd.readLine());
			// when reading from each line,
			// we add a new user class object to the arraylist
			clients.add(new user(record.split(" ")[0],record.split(" ")[1]));
		}
		brd.close(); // close the buffer

		// establish the server
		server = new ServerSocket(PORT);
		try {
			// infinite loop for the server to listen from clients
			while (true) {
				Socket client = server.accept();
				
				// initialize the buffered reader and print writer for this socket client
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out = new PrintWriter(client.getOutputStream(), true);

				// create a new thread to handle this socket client
				// each thread would handle only one socket client
				// multi-threading programming
				new Handler(client).start();
			}
		} catch (Exception e) {
			// handle the exception
			System.out.println("Something wrong. The chat server closed.");
		} 

	}
	
	/**
	 * This function is responsible for the log in process.
	 * It returns a boolean type to indicate whether or not this user is
	 * logged in successfully.
	 * 
	 * @param socket: the current accepted socket connection
	 * @param position: an array list that contains only one integer to indicate the index
	 * of this user in the clients array list.
	 * @return a boolean to indicate whether or not this user is logged in successfully
	 * @throws Exception: includes IOException and other exceptions
	 */
	private static boolean login(Socket socket, ArrayList<Integer> position) throws Exception {
		String user_name; 
		String user_password;
		// count the # of attempts to log in
		int[] count = new int[clients.size()]; 
		boolean flag = false; // indicate whether or not the user logs in successfully

		// initialize the count array
		for(int i = 0; i < count.length; i++)
			count[i] = 0;

		// here's log in routine
		do {
			// read the user name
			out.println("Username: ");
			user_name = in.readLine();
			
			// read the password
			out.println("Password: ");
			user_password = in.readLine();

			// check the correctness of the user name and password
			if(check_user_pass(user_name, user_password) == true ){
				// indicate which user are online now
				for(int i = 0; i < clients.size(); i++){
					//create a reference variable to point to the specific user
					user record = clients.get(i);
					// if we find out the same user name
					if(user_name.equals(record.name)) {
						// to check if it has already been log in by other concole
						// if not, then this user is successfully log in
						if(record.isOnline == false) {
							out.println("Welcome to simple chat server!");
							// turn the variable "isOnline" to true
							record.isOnline = true;
							// the position of this client in the clients array list
							// so that we can get to this user very quickly
							position.add(new Integer(i));
							// the current log in time
							record.last_log_in = System.currentTimeMillis();
							record.user_soccket = socket;
							
							// the user-specific writer and reader
							// for usage of the broadcasting and private message
							record.user_in = new BufferedReader(new InputStreamReader(record.user_soccket.getInputStream()));
							record.user_out = new PrintWriter(record.user_soccket.getOutputStream(),true);
							
							// the user is successfully log in
							flag = true;
							return flag;
						}else {
							// the user is now logged in by other socket
							// prohibit duplicate users
							out.println("The user is loged in now. Please avoid using the same user at the same time.");
							in.readLine(); // flush the input buffer
						}
					}

				}
			}
			// the user enters the wrong password or user name
			else{

				// check if the user name exists in the records
				// if not, simply reset the count array and continue the loop
				// if yes, remember how many consecutive times the user has
				for( int i = 0; i < clients.size(); i++){
					user record = clients.get(i);
					if( user_name.equals(record.name) ){
						count[i]++;
						// for others, we reset it to zero,
						// since we only care consecutive log-ins
						for(int j = 0; j < count.length; j++){
							if(j != i )
								count[j] = 0;
						}
						break;
					}
				}

				// the user name is different from last login, but the user_name doesn't exist,
				// then we should clean up all the previous counts.
				boolean illegit = true;
				for(int i = 0; i < clients.size(); i++){
					user record = clients.get(i);
					if(user_name.equals(record.name)){
						illegit = false;
					}
				}
				if(illegit){
					for(int i: count)
						count[i] = 0;
				}


				out.println("Wrong! Please try it again.");
				// keep track of how many times of consecutive failure log-in
				// if there are 3 consecutive failure log-in for one specific user name,
				// we block the user
				for(int j = 0; j < count.length; j++){
					if(count[j] == 3) {
						out.println("There are 3 consecutive failure trials.");
						return flag;
					}
				}
				
				in.readLine();
			}
		// as long as the user is not yet login successfully and 
		// trials time is less than 4, we keep looping for the input of 
		// user name and password
		}while(!flag); 

	// the program cannot get to here.
	// this is for the function return type purpose.
	return flag;
	}

	/**
	 * This function is responsible for handling the commands from the user.
	 * @param client: the client that the server is talking to
	 * @param position: the index of clients array that contains this user/client
	 * @throws Exception: includes IOException and others
	 */
	private static void use_command(Socket client, ArrayList<Integer> position) throws Exception{
		// we use the buffered reader and print writer that
		// are inside the user class object itself
		// so that we can get and send message from this user specifically
		BufferedReader client_listener = clients.get(position.get(0)).user_in;
		PrintWriter client_writer = clients.get(position.get(0)).user_out;
		String command;

		// infinite loop for a user to type command
		while (true){
			client_writer.println("Command: ");
			client_writer.flush();
			//client_listener.readLine();
			command = client_listener.readLine();

			// whoelse command
			if (command.equals("whoelse")) {
				// loop over all the active online user.
				// if we find one online user, we print the names to the console
				// of this socket/client
				for(int i = 0; i < clients.size(); i++) {
					user record = clients.get(i);
					if(record.isOnline == true && !clients.get(position.get(0)).name.equals(record.name)) {
						client_writer.print(record.name + ", ");
					}

				}
				client_writer.println();
				client_writer.flush();
			}
			// the wholasthr command: the users in the last hour
			else if (command.equals("wholasthr")){
				for(int i = 0; i < clients.size(); i++) {
					user record = clients.get(i);
					// if the last_log_in is initialized,
					// and the gap between now and the last_log_in is less than one hour,
					// we print to names to the console of this socket
					if (record.last_log_in != 0 
						&& System.currentTimeMillis() - record.last_log_in <= 3600000*LAST_HOUR
						) {
						client_writer.print(record.name + ", ");
					}
				}
				client_writer.println();
				client_writer.flush();
			}
			// the "broadcast" command
			else if (command.startsWith("broadcast")) {
				if(command.length() < 11){
					client_writer.println();
					client_writer.flush();
				}
				else {
					// get the message substring
					String message = command.substring(10);
					// get the sender name
					String sender = clients.get(position.get(0)).name;

					for(int i = 0; i < clients.size(); i++){
						// send this message to all online clients
						user record = clients.get(i);
						if( record.isOnline == true) {
							record.user_out.println(sender + ": " + message);
							record.user_out.flush();
						}
					}
				}
			}
			// the "message" command: sending private message
			else if (command.startsWith("message")) {
				String[] temp = command.split(" ");
				// send to whom? --> target
				if(temp.length < 3) {
					client_writer.println();
					client_writer.flush();
				}
				else{
					String target = temp[1];

					// store whatever message after the target name
					String message = "";
					for(int i = 2; i < temp.length; i++){
						message = message + " " + temp[i];
					}
					
					// get the sender name
					String sender = clients.get(position.get(0)).name;

					// a flag to indicate if the target is legit
					boolean isFound = false;
					// we search the list to get to this target client
					for(int i = 0; i < clients.size(); i++){
						user record = clients.get(i);
						// if we find the match, then we print to this client,
						// make sure this target is online, if not, 
						// simply discard the message
						if( target.equals(record.name) && record.isOnline) {
							record.user_out.println(sender + ": " + message);
							record.user_out.flush();
							isFound = true;
							break;
						}
					}

					// then we cannot find the target or the target is not online
					if(!isFound){
						client_writer.println("The message receiver does not exit or not online.");
						client_writer.flush();
					}

				}
			}
			// the "logout" command
			// or the user is inactive for 30 minutes
			// it will automatically log out
			else if (command.equals("logout") || System.currentTimeMillis() - clients.get(position.get(0)).last_log_in >= TIME_OUT*60000 ){
				client_writer.println("LOGOUT");
				client_writer.flush();
				
				// close all the buffer and printer
				client_writer.close();
				client_listener.close();

				// record the log out information
				clients.get(position.get(0)).isOnline = false;
				clients.get(position.get(0)).last_log_out = System.currentTimeMillis();
				
				// break out of the loop
				break;
			}

			else {
				// unknown command
				client_writer.println("Unknown command. Please enter again.");
				client_writer.flush();
			}
		}

	}

	/**
	 * This function is responsible for checking the correctness of the 
	 * type-in user name and password.
	 * @param user_name: the type-in user name
	 * @param user_password: the type-in password
	 * @return boolean to indicate the result
	 */
	private static boolean check_user_pass (String user_name, String user_password){
		for(int i = 0; i < clients.size(); i++){
			if (user_name.equals(clients.get(i).name)  && user_password.equals(clients.get(i).password))
				return true;
		}
		return false;
	}


	/**
	 * The user class object contains all information about the user,
	 * so that we can keep track of its online status, log-in and log-out time, etc.
	 * @author Wenjie Zhang, UNI: wz2261
	 *
	 */
	public static class user {
		public String name;
		public String password;
		public boolean isOnline;
		public long last_log_in, last_log_out;

		// for broadcast and private message usage
		public Socket user_soccket;
		public BufferedReader user_in;
		public PrintWriter user_out;

		public user (String name, String password) throws Exception{
			this.name = name;
			this.password = password;
			this.isOnline = false;
		}
	}

	/**
	 * A subclass extended from Thread to implement the runnable.
	 * Each thread would handle only one socket client.
	 * Thus, the multi-threading feature allows multiple socket connection and communication.
	 * @author Wenjie Zhang, UNI: wz2261
	 *
	 */
	public static class Handler extends Thread{
		// the client that we handle
		public Socket client;
		// the index of clients array list that contains this user
		public ArrayList<Integer> position;
		// kill the thread if the flag is false
		private volatile boolean isRunning = true;

		public Handler (Socket client) {
			this.client = client;
			position = new ArrayList<Integer>();
		}
		
		// the super class method run()
		public void run() {
			while(isRunning){
				try {
					// if log in is successful
					if(login(client, position)){
						// then go to the command section
						use_command(client, position);
					}
					else
						// the user is blocked for 60 seconds
						out.println("Sorry, you have to wait " + BLOCK_TIME + " seconds to continue to log in...");
				}catch (Exception e) {
					// some unknown exception
					System.out.println("Exception has been caught.");
				}finally{
					// stop the thread running
					isRunning = false;
				}
			}
			
		}
	}



}