// The listen_socket class is used in the client to listen for
// incoming messages.
import java.io.*;
import java.util.*;
import java.net.*;

public class listen_socket extends Thread {

	public DatagramSocket listen;
	public DatagramPacket packet;
	public ArrayList<node> neighbors;
	public ArrayList<vector> distance_vector;
	public ArrayList<node> backup;
	public message received_message;
	public DatagramSocket write;
	public DatagramPacket write_packet;

	public boolean isRunning = true;
	public boolean hasChanged = false;

	public listen_socket(DatagramSocket listen, DatagramPacket packet, ArrayList<node> neighbors, ArrayList<vector> distance_vector, DatagramSocket write, ArrayList<node> backup) {
		this.listen = listen;
		this.packet = packet;
		this.neighbors = neighbors;
		this.distance_vector = distance_vector;
		this.write = write;
		this.received_message = null;
		this.backup = backup;
	}

	public void run() {

		while(isRunning){

			if (listen == null)
				break;

			// receive the packet
			try {
				listen.receive(packet);
			}catch (IOException ioe){
				System.out.println("Cannot receive the packet.");
				break;
			}
			
			// reset the hasChanged boolean
			hasChanged = false;

			// who sends this packet?
			InetAddress sender = packet.getAddress();
			int sender_port = (packet.getPort()) / 13;

			InetAddress receiver;
			int receiver_port = listen.getLocalPort();
			try {
				receiver = InetAddress.getByName("localhost");
			}catch(UnknownHostException uhe){
				System.out.println("Cannot bind to local host");
				continue;
			}

			// convert it to a message type
			try {
				received_message = toMessage(packet.getData());
			} catch (IOException ioe) {
				System.out.println("Corrupted packet. Drop.");
			} catch (ClassNotFoundException cnfe) {
				System.out.println("Cannot convert packet to message type.");
			}

			ArrayList<vector> received_distance_vector = received_message.distance_vector;

			// assign this DV to the sender in the neighbors list
			boolean isMyNeighbor = false;
			for(int a = 0; a < neighbors.size(); a++){
				node tmp = neighbors.get(a);
				if(tmp.ip_address.equals(sender) && tmp.port == sender_port){
					tmp.setDV(received_distance_vector);
					isMyNeighbor = true;
				}
			}

			if(isMyNeighbor == false) {

				for(int i = 0; i < received_distance_vector.size(); i++){
					vector v = received_distance_vector.get(i);
					if(v.destination.equals(receiver) || v.port == receiver_port) {
						neighbors.add(new node(sender, sender_port, v.cost));
						backup.add(new node(sender, sender_port, v.cost));
					}
				}
			}
			
			// check if there's any new nodes, if yes, add it to its distance vector
			for(int b = 0; b < received_distance_vector.size(); b++){
				
				InetAddress target = received_distance_vector.get(b).destination;
				int target_port = received_distance_vector.get(b).port;
				boolean flag = false;

				for(int c = 0; c < distance_vector.size(); c++){
					if(distance_vector.get(c).destination.equals(target) && distance_vector.get(c).port == target_port)
						flag = true;
				}

				// if the target does not exist in its distance vector
				// add it to the list
				if(flag == false && target_port != receiver_port) {
					distance_vector.add(new vector(target, Double.POSITIVE_INFINITY, target_port));
					hasChanged = true;
				}
			}
			

			if(received_message.ROUTE_UPDATE == true){

				handleRouteUpdate(sender, sender_port);

			}else if(received_message.LINKDOWN == true){

				handleLinkDown(sender, sender_port, receiver, receiver_port);

			}else if(received_message.LINKUP == true){
				handleLinkUp(sender, sender_port, receiver, receiver_port);

			}else if(received_message.CLOSE == true){

				handleClose(sender, sender_port, receiver, receiver_port);

			}else{
				// non-valid message, drop it
				continue;
			}

		}

		if(listen != null)
			listen.close();
		if(write != null)
			write.close();

	}

	// send its message to its neighbors
	public void sendMessage(boolean hasChanged, message m ){

		if(hasChanged == true) {

			// send its distance vector to its neighbors
			for(int p = 0; p < neighbors.size(); p++) {

				node neighbor = neighbors.get(p);
				
				if(neighbor.isNeighbor == false)
					continue;

				// create the output buffer
				byte[] buffer = new byte[1];

				try {
					// write the distance vector into the buffer
					buffer = toByteArray(m);
				}catch(IOException ioe){
					System.out.println("Cannot put the message into the output buffer.");
				}
				
				write_packet = new DatagramPacket(buffer, buffer.length, neighbor.ip_address, neighbor.port);
				try {
					write.send(write_packet);
				}catch(IOException ioe){
					System.out.println("Cannot send the packet.");
				}

			}
		}
	}

	// update its distance vector by using Bellman-Ford Algorithm
	public void updateDV() {

		for(int i = 0; i < distance_vector.size(); i++){
				
			vector v = distance_vector.get(i);
			InetAddress y = v.destination;
			int y_port = v.port;
			double min = v.cost;

			for(int j = 0; j < neighbors.size(); j++){
				node neighbor = neighbors.get(j);

				if(neighbor.isNeighbor == false)
					continue;

				double cost_to_neighbor = neighbor.weight;
				ArrayList<vector> dv = neighbor.DV;

				for(int z = 0; z < dv.size(); z++){

					if(dv.get(z).destination.equals(y) && dv.get(z).port == y_port && (cost_to_neighbor + dv.get(z).cost < min)) {
						min = cost_to_neighbor + dv.get(z).cost;
						v.link_address = neighbor.ip_address;
						v.link_port = neighbor.port;
						hasChanged = true;
					}
				}
				
			}
			//assign the min to the cost
			v.cost = min;
		}
	}

	// handle the close message
	public void handleClose(InetAddress sender, int sender_port, InetAddress receiver, int receiver_port){
		
		// which node is leaving?
		// then delete it from the neighbors list
		int target_port = received_message.close_port;

		// delete the leaving node from the neighbors list
		for(int i = 0; i < neighbors.size(); i++){
			node n = neighbors.get(i);
			if( n.port == target_port ){
				n.weight = Double.POSITIVE_INFINITY;
				n.isNeighbor = false;
				break;
			}
		}

		// delete the leaving node from the distance vector
		for(int i = 0; i < distance_vector.size(); i++) {
			vector v = distance_vector.get(i);
			if(v.port == target_port || v.link_port == target_port) {
				v.cost = Double.POSITIVE_INFINITY;
				v.link_address = null;
				v.link_port = 0;
				break;
			}
		}

		// delete the leaving node from the neighbors
		for(int i = 0; i < neighbors.size(); i++){
			node n = neighbors.get(i);
			ArrayList<vector> n_dv = n.DV;
			for(int j = 0; j < n_dv.size(); j++){
				vector v = n_dv.get(j);
				if(v.port == target_port || v.link_port == target_port){
					v.cost = Double.POSITIVE_INFINITY;
					v.link_address = null;
					v.link_port = 0;
					break;
				}
			}
		}

		// re-assign the distance vector
		hasChanged = true;
/*
        for(int i = 0; i < distance_vector.size(); i++){
            vector v = distance_vector.get(i);
            boolean flag = false;
            for(int j = 0; j < neighbors.size(); j++){
                node n = neighbors.get(j);
                if(n.isNeighbor == true && n.ip_address.equals(v.destination) && n.port == v.port) {
                    v.cost = n.weight;
                    v.link_address = n.ip_address;
                    v.link_port = n.port;
                    flag = true;
                }
            }
            if(flag == false) {
                v.cost = Double.POSITIVE_INFINITY;
                v.link_address = null;
                v.link_port = 0;
                if(v.destination.equals(receiver) || v.port == receiver_port) 
                	v.cost = 0.0;
            }
        }
*/

        // create a CLOSE message
		message cm = new message();
		cm.setDistanceVector(distance_vector);
		cm.CLOSE = true;
		cm.close_port = target_port;

		sendMessage(hasChanged, cm);

		updateDV();

	}

	// handle the link up message
	public void handleLinkUp(InetAddress sender, int sender_port, InetAddress receiver, int receiver_port){
		
		// need to know which link is up
		ArrayList<vector> received_distance_vector = received_message.distance_vector;
		int link_up_port = received_message.link_up_port;
		
		// if receiver itself is the link-up
		if(link_up_port == receiver_port) {
			for(int i = 0; i < neighbors.size(); i++) {
				node n = neighbors.get(i);
				if(n.ip_address.equals(sender) && n.port == sender_port) {
					n.isNeighbor = true;
					for(int j = 0; j < received_distance_vector.size(); j++){
						vector v = received_distance_vector.get(j);
						if(v.port == receiver_port)
							n.weight = v.cost;
					}
				}
			}
		}

		// re-assign the distance vector
		hasChanged = true;

        for(int i = 0; i < distance_vector.size(); i++){
            vector v = distance_vector.get(i);
            boolean flag = false;
            for(int j = 0; j < neighbors.size(); j++){
                node n = neighbors.get(j);
                if(n.isNeighbor == true && n.ip_address.equals(v.destination) && n.port == v.port) {
                    v.cost = n.weight;
                    v.link_address = n.ip_address;
                    v.link_port = n.port;
                    flag = true;
                }
            }
            if(flag == false) {
                v.cost = Double.POSITIVE_INFINITY;
                v.link_address = null;
                v.link_port = 0;
                if(v.destination.equals(receiver) || v.port == receiver_port) 
                	v.cost = 0.0;
            }
        }

        //for(vector v : distance_vector)
        //	System.out.println("port: " + v.port + ", cost: " + v.cost + ", link: " + v.link_port);

        updateDV();

        //for(vector v : distance_vector)
        //	System.out.println("port: " + v.port + ", cost: " + v.cost + ", link: " + v.link_port);

        // create a LINKUP message
		message lu = new message();
		lu.setDistanceVector(distance_vector);
		lu.LINKUP = true;
		lu.link_up_port = link_up_port;

		// send the distance vector to neighbors
		sendMessage(hasChanged, lu);
	}

	// handle the link down message
	public void handleLinkDown(InetAddress sender, int sender_port, InetAddress receiver, int receiver_port){

		// need to know which link is down
		ArrayList<vector> received_distance_vector = received_message.distance_vector;

		// a flag to check if there's any infinity cost of neighbors
		boolean inf = false;
		for(int i = 0; i < received_distance_vector.size(); i++){
			vector v = received_distance_vector.get(i);
			if(v.cost == Double.POSITIVE_INFINITY)
				inf = true;

			// if it's neighbor, update the weight to infinity
			if((v.destination.equals(receiver) || v.port == receiver_port) && v.cost == Double.POSITIVE_INFINITY) {
				
				for(int j = 0; j < neighbors.size(); j++){
					node n = neighbors.get(j);
					if(n.ip_address.equals(sender) && n.port == sender_port) {
						n.isNeighbor = false;
						n.weight = Double.POSITIVE_INFINITY;
					}
				}
			}

		}

		// re-assign the distance vector
		hasChanged = true;

        for(int i = 0; i < distance_vector.size(); i++){
            vector v = distance_vector.get(i);
            boolean flag = false;
            for(int j = 0; j < neighbors.size(); j++){
                node n = neighbors.get(j);
                if(n.isNeighbor == true && n.ip_address.equals(v.destination) && n.port == v.port) {
                    v.cost = n.weight;
                    v.link_address = n.ip_address;
                    v.link_port = n.port;
                    flag = true;
                }
            }
            if(flag == false) {
                v.cost = Double.POSITIVE_INFINITY;
                v.link_address = receiver;
                v.link_port = receiver_port;
                if(v.destination.equals(receiver) || v.port == receiver_port) 
                	v.cost = 0.0;
            }
        }

        // assign itself's distance vector to the neighbor list
        for(int i = 0; i < neighbors.size(); i++){
        	node n = neighbors.get(i);
        	if(n.ip_address.equals(receiver) || n.port == receiver_port)
        		n.setDV(distance_vector);
        }

        updateDV();

        // create a ROUTE UPDATE message
		message ld = new message();
		ld.LINKDOWN = true;
		ld.setDistanceVector(distance_vector);

		// send the distance vector to neighbors
		sendMessage(hasChanged, ld);

	}

	// handle the route update message
	public void handleRouteUpdate(InetAddress sender, int sender_port) {

		// now we update the distance vector
		updateDV();

		// create a ROUTE UPDATE message
		message ru = new message();
		ru.ROUTE_UPDATE = true;
		ru.setDistanceVector(distance_vector);

		// send the distance vector to neighbors
		sendMessage(hasChanged, ru);
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