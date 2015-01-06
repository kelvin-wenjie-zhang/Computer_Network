// Bellman-Ford algorithm in distributed client processes
import java.io.*;
import java.util.*;
import java.net.*;

public class bfclient {

	public static int PORT = 0;
	public static int TIMEOUT = 0;
	public static ArrayList<node> neighbors = new ArrayList<node>();
	public static ArrayList<vector> distance_vector = new ArrayList<vector>();
    
    // for the UDP listening socket
    public static DatagramSocket listen;
    public static DatagramPacket listen_packet;
    public static int BUFF_SIZE = 2000;

    // for the UDP writing socket
    public static DatagramSocket write;
    public static DatagramPacket write_packet;

    // for linkup restoring
    public static ArrayList<node> backup = new ArrayList<node>();

	public static void main(String[] args) throws IOException{

		// read all the arguments from the command line
		int args_length = args.length;
		if (args_length < 5) {
            System.out.println("Usage: java bfclient <PORT> <TIMEOUT> [IP_Address1 port1 weight1 ...]");
            System.exit(0);
        } else if ((args_length - 2) % 3 != 0) {
        	System.out.println("Usage: java bfclient <PORT> <TIMEOUT> [IP_Address1 port1 weight1 ...]");
            System.out.println("Please provide valid numbers of parameters.");
            System.exit(0);
        } else {
            // read the local port and timeout
            try {
                PORT = Integer.parseInt(args[0]);
                TIMEOUT = Integer.parseInt(args[1])*1000;
            } catch (NumberFormatException nfe) {
                System.out.println("Please provide valid integers for local port and timeout parameters.");
                System.exit(1);
            }

            // add the neighbors information
        	int count = 2;
        	while(count < args_length) {
        		neighbors.add(new node(args[count], args[count+1], args[count+2]));
        		count = count + 3;
        	}
        }

        // set isNeighbor to true
        for(int i = 0; i < neighbors.size(); i++){
            neighbors.get(i).isNeighbor = true;
        }

        // add the neighbors' weight as costs in the distance vector
        for(int i = 0; i < neighbors.size(); i++){
            node n = neighbors.get(i);
            vector v = new vector(n.ip_address, n.weight, n.port);
            v.link_address = n.ip_address;
            v.link_port = n.port;

            distance_vector.add(v);
        }

        // create the UDP socket to listen for incoming messagess
        try {
            listen = new DatagramSocket(PORT);
            write = new DatagramSocket(PORT*13);
        }catch (SocketException se) {
            System.out.println("The socket could not be opened, or the socket could not bind to the specified local port.");
            System.exit(1);
        }
        byte[] listen_buf = new byte[BUFF_SIZE];
        listen_packet = new DatagramPacket(listen_buf, listen_buf.length);
        write_packet = null;

        // add itself to the distance vector
        //distance_vector.add(new vector("192.168.0.104", 0.0, listen.getLocalPort(), "192.168.0.104", PORT));
        // can use InetAddress.getLocalHost(); or maybe listen.getLocalAddress();

        // keep listening on that port
        listen_socket mylisten = new listen_socket(listen, listen_packet, neighbors, distance_vector, write, backup);
        mylisten.start();

        // run another thread to monitor TIMOUT
        timeout mytimeout = new timeout(write, write_packet, neighbors, distance_vector, TIMEOUT);
        mytimeout.start();

        // create a copy of neighbors to backup
        for(int i = 0; i < neighbors.size(); i++){
            node n = neighbors.get(i);
            backup.add(new node(n.ip_address, n.port, n.weight));
        }

        // provide the user interface
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(">> ");
        String command = reader.readLine();

        while(!command.equals("CLOSE")){

            if(command.equals("SHOWRT")){
                for(int i = 0; i < distance_vector.size(); i++){
                    vector v = distance_vector.get(i);
                    if(v.port != PORT && v.link_address != null)
                        System.out.println("Destination = " + v.destination.toString() + ":" + v.port + ", Cost: " + v.cost + ", Link = (" + v.link_address.toString() + ":" + v.link_port + ")");
                    else if(v.port != PORT && v.link_address == null)
                        System.out.println("Destination = " + v.destination.toString() + ":" + v.port + ", Cost: " + v.cost + ", Link = (" + "Unreachable" + ")");
                }

            }else if(command.startsWith("LINKDOWN")){
                String[] s = command.split(" ");
                if(s.length != 3) {
                    System.out.println("Usage: LINKDOWN <IP_Address> <Port>");
                    System.out.print(">> ");
                    command = reader.readLine();
                    continue;
                }else{
                    // get the linkdown ip address
                    InetAddress target;
                    try {
                        target = InetAddress.getByName(s[1]);
                    }catch(UnknownHostException uhe){
                        System.out.println("Cannot find the linkdown IP Address.");
                        System.out.print(">> ");
                        command = reader.readLine();
                        continue;
                    }

                    // get the linkdown port
                    int target_port;
                    try {
                        target_port = Integer.parseInt(s[2]);
                    }catch(NumberFormatException nfe){
                        System.out.println("Unvalid port number.");
                        System.out.print(">> ");
                        command = reader.readLine();
                        continue;
                    }

                    handleLinkDown(target, target_port);

                }

            }else if(command.startsWith("LINKUP")){

                String[] s = command.split(" ");
                if(s.length != 3) {
                    System.out.println("Usage: LINKUP <IP_Address> <Port>");
                    System.out.print(">> ");
                    command = reader.readLine();
                    continue;
                }else{
                    // get the linkup ip address
                    InetAddress target;
                    try {
                        target = InetAddress.getByName(s[1]);
                    }catch(UnknownHostException uhe){
                        System.out.println("Cannot find the linkdown IP Address.");
                        System.out.print(">> ");
                        command = reader.readLine();
                        continue;
                    }

                    // get the linkup port
                    int target_port;
                    try {
                        target_port = Integer.parseInt(s[2]);
                    }catch(NumberFormatException nfe){
                        System.out.println("Unvalid port number.");
                        System.out.print(">> ");
                        command = reader.readLine();
                        continue;
                    }

                    handleLinkUp(target, target_port);
                    handleLinkUp(target, target_port);
                }
            }else{
                System.out.println("Invalid Command.");
            }

            System.out.print(">> ");
            command = reader.readLine();
        }

        // CLOSE command
        // use the CLOSE flag in message
        // and send the message to all neighbors to let them delete itself
        // from their distance vector and neighbors.
        handleClose();

        System.exit(0);

	}

    // handle the close command
    public static void handleClose() {

        for(int i = 0; i < neighbors.size(); i++){
            node n = neighbors.get(i);
            n.isNeighbor = false;
            n.weight = Double.POSITIVE_INFINITY;
        }

        for(int i = 0; i < distance_vector.size(); i++){
            vector v = distance_vector.get(i);
            v.cost = Double.POSITIVE_INFINITY;
            v.link_address = null;
            v.link_port = 0;
        }

        // send CLOSE message to all neighbors
        for(int i = 0; i < neighbors.size(); i++){
            
            node neighbor = neighbors.get(i);

            // create the output buffer
            byte[] buffer = new byte[1];

            // create a close message
            message cm = new message();
            cm.CLOSE = true;
            cm.close_port = PORT;
            cm.setDistanceVector(distance_vector);
            
            try {
                // write the distance vector into the buffer
                buffer = toByteArray(cm);
            }catch(IOException ioe){
                System.out.println("Cannot put the CLOSE message into the output buffer.");
            }

            // send the packet
            write_packet = new DatagramPacket(buffer, buffer.length, neighbor.ip_address, neighbor.port);
            try {
                write.send(write_packet);
            }catch(IOException ioe){
                System.out.println("Cannot send the write_packet.");
            }
        }

    }

    // handle the link up command
    public static void handleLinkUp(InetAddress target, int target_port) {

        // re-activate the neightbor in the neighbors list
        for(int i = 0; i < neighbors.size(); i++){
            node n = neighbors.get(i);
            if(n.ip_address.equals(target) && n.port == target_port) {
                for(int j = 0; j < backup.size(); j++){
                    node u = backup.get(j);
                    if(u.ip_address.equals(target) && u.port == target_port){
                        n.weight = u.weight;
                        n.isNeighbor = true;
                        break;
                    }
                }
            }
        }

        // update distance vector
        for(int i = 0; i < distance_vector.size(); i++){
            vector v = distance_vector.get(i);
            boolean isNei = false;
            for(int j = 0; j < neighbors.size(); j++){
                node n = neighbors.get(j);
                if(n.isNeighbor == true && n.ip_address.equals(v.destination) && n.port == v.port){
                    v.cost = n.weight;
                    v.link_address = n.ip_address;
                    v.link_port = n.port;
                    isNei = true;
                }
            }
            if(isNei == false) {
                v.cost = Double.POSITIVE_INFINITY;
                v.link_address = null;
                v.link_port = 0;
            }

        }

        // send LINK UP message to all neighbors
        for(int i = 0; i < neighbors.size(); i++){
            node neighbor = neighbors.get(i);

            // create the output buffer
            byte[] buffer = new byte[1];

            // create a link down message
            message link_up_message = new message();
            link_up_message.setDistanceVector(distance_vector);
            link_up_message.LINKUP = true;
            link_up_message.link_up_port = target_port;

            try {
                // write the distance vector into the buffer
                buffer = toByteArray(link_up_message);
            }catch(IOException ioe){
                System.out.println("Cannot put the link_up_message into the output buffer.");
            }

            // send the packet
            write_packet = new DatagramPacket(buffer, buffer.length, neighbor.ip_address, neighbor.port);
            try {
                write.send(write_packet);
            }catch(IOException ioe){
                System.out.println("Cannot send the write_packet.");
            }
        }
    }

    // handle the link down command
    public static void handleLinkDown(InetAddress target, int target_port){

        // de-activate the neighbor
        for(int i = 0; i < neighbors.size(); i++){
            node n = neighbors.get(i);
            if(n.ip_address.equals(target) && n.port == target_port){

                // set the weight to infinity
                n.isNeighbor = false;
                n.weight = Double.POSITIVE_INFINITY;
                break;
            }
        }

        // change the weight to infinity in the distance vector
        for(int i = 0; i < distance_vector.size(); i++){
            vector v = distance_vector.get(i);
            if(v.destination.equals(target) && v.port == target_port)
                v.cost = Double.POSITIVE_INFINITY;
            else if(v.link_address.equals(target) && v.link_port == target_port){
                boolean isNei = false;
                for(int j = 0; j < neighbors.size(); j++){
                    node n = neighbors.get(j);
                    if(n.isNeighbor && n.ip_address.equals(v.destination) && n.port == v.port){
                        v.cost = n.weight;
                        v.link_address = n.ip_address;
                        v.link_port = n.port;
                        isNei = true;
                    }
                }
                if(isNei == false){
                    v.cost = Double.POSITIVE_INFINITY;
                    try {
                        v.link_address = InetAddress.getByName("localhost");
                    }catch(UnknownHostException uhe){
                        System.out.println("cannot bind to local host");
                    }
                    v.link_port = listen.getLocalPort();
                }
            }
        }

        // send LINK DOWN message to all neighbors
        for(int i = 0; i < neighbors.size(); i++){
            node neighbor = neighbors.get(i);

            // create the output buffer
            byte[] buffer = new byte[1];

            // create a link down message
            message link_down_message = new message();
            link_down_message.LINKDOWN = true;
            link_down_message.setDistanceVector(distance_vector);

            try {
                // write the distance vector into the buffer
                buffer = toByteArray(link_down_message);
            }catch(IOException ioe){
                System.out.println("Cannot put the link_down_message into the output buffer.");
            }

            // send the packet
            write_packet = new DatagramPacket(buffer, buffer.length, neighbor.ip_address, neighbor.port);
            try {
                write.send(write_packet);
            }catch(IOException ioe){
                System.out.println("Cannot send the write_packet.");
            }
        }

    }

    // convert a distance vector arraylist to a byte array
    public static byte[] toByteArray(message m) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(m);
            oos.flush();
            bytes = bos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
        return bytes;
    }

    // convert a byte array to a distance vector arraylist
    public static message toMessage(byte[] bytes) throws IOException, ClassNotFoundException {
        message m = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            @SuppressWarnings("unchecked")
            message tmp = (message)ois.readObject();
            m = tmp;
        }catch(ClassCastException cce){
            System.out.println("cannot convert to type of message.");
        }finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return m;
    }


}
