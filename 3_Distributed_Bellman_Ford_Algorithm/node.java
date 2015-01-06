// Node class that contains IP address, port number and weight
import java.io.*;
import java.util.*;
import java.net.*;

public class node {

	public InetAddress ip_address;
	public int port;
	public Double weight;
	public ArrayList<vector> DV = new ArrayList<vector>();

	public boolean isNeighbor;

	public node(InetAddress ip_address, int port, double weight) {
		this.ip_address = ip_address;
		this.port = port;
		this.weight = weight;
		this.isNeighbor = true;
	}

	public node(String i, String p, String w) {
		try {
			this.ip_address = InetAddress.getByName(i);
			this.port = Integer.parseInt(p);
			this.weight = Double.parseDouble(w);
		}catch (NumberFormatException nfe){
			System.out.println("Please provide a valid integer for port and weight parameters.");
			System.exit(1);
		}catch (UnknownHostException uhe) {
			System.out.println("Please provide a valid IP address.");
			System.exit(1);
		}catch (SecurityException se) {
			System.out.println("Please provide a valid IP address.");
			System.exit(1);
		}
	}

	public void setDV(ArrayList<vector> dv){
		this.DV.clear();
		for(int i = 0; i < dv.size(); i++){
			vector v = dv.get(i);
			DV.add(new vector(v.destination, v.cost, v.port));
		}
	}
}