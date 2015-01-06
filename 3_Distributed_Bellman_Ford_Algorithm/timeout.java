// monitor the timeout event
import java.io.*;
import java.util.*;
import java.net.*;

public class timeout extends Thread{

	public int TIMEOUT;
	public ArrayList<node> neighbors;
	public ArrayList<vector> distance_vector;
	
	// for the UDP writing socket
    public static DatagramSocket write;
    public static DatagramPacket write_packet;

    public boolean isRunning = true;

	public timeout(DatagramSocket write, DatagramPacket write_packet, ArrayList<node> neighbors, ArrayList<vector> distance_vector, int TIMEOUT) {
		
		this.write = write;
		this.write_packet = write_packet;
		this.neighbors = neighbors;
		this.distance_vector = distance_vector;
		this.TIMEOUT = TIMEOUT;
	}

	public void run() {
		while(isRunning) {
			// count down the time out
			try {
				Thread.sleep(TIMEOUT);
			}catch(InterruptedException ie){
				System.out.println("Cannot use timeout. Break.");
				System.exit(1);
			}
			
			// send its distance vector to its neighbors
			for(int p = 0; p < neighbors.size(); p++) {

				node neighbor = neighbors.get(p);
				
				if(neighbor.isNeighbor == false)
					continue;

				// create the output buffer
				byte[] buffer = new byte[1];

				// create a ROUTE UPDATE message
				message ru = new message();
				ru.ROUTE_UPDATE = true;
				ru.setDistanceVector(distance_vector);

				try {
					// write the distance vector into the buffer
					buffer = toByteArray(ru);
				}catch(IOException ioe){
					System.out.println("Cannot put DV into the output buffer.");
				}
				//System.out.println("The buffer length is: " + buffer.length);
				// send the packet
				write_packet = new DatagramPacket(buffer, buffer.length, neighbor.ip_address, neighbor.port);
				try {
					write.send(write_packet);
				}catch(IOException ioe){
					System.out.println("Cannot send the packet.");
				}

			}

		}
		if(write != null)
			write.close();
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