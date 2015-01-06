// vector = <destination, cost>
import java.io.*;
import java.util.*;
import java.net.*;

public class vector implements Serializable{

	public InetAddress destination;
	public Double cost;
	public int port;

	// for the next hop link
	public InetAddress link_address;
	public int link_port;

	public vector(InetAddress destination, Double cost, int port) {
		this.destination = destination;
		this.cost = cost;
		this.port = port;
		this.link_address = null;
	}

	public vector(String destination, Double cost, int port, String link_address, int link_port) {
		try{
			this.destination = InetAddress.getByName(destination);
		} catch( UnknownHostException uhe) {
			System.out.println("Cannot get the InetAddress of " + destination);
			System.exit(1);
		}

		try{
			this.link_address = InetAddress.getByName(link_address);
		} catch( UnknownHostException uhe) {
			System.out.println("Cannot get the InetAddress of " + link_address);
			System.exit(1);
		}

		this.cost = cost;
		this.port = port;
		this.link_port = link_port;
	}

	public vector(String d, String c, String p) {
		try {
			this.destination = InetAddress.getByName(d);
			this.cost = Double.parseDouble(c);
			this.port = Integer.parseInt(p);
		}catch (NumberFormatException nfe){
			System.out.println("Please provide a valid integer for the cost.");
			System.exit(1);
		}catch (UnknownHostException uhe) {
			System.out.println("Please provide a valid IP address.");
			System.exit(1);
		}catch (SecurityException se) {
			System.out.println("Please provide a valid IP address.");
			System.exit(1);
		}
	}

	public vector(String d, Double cost, int port) {
		this.cost = cost;
		this.port = port;
		try {
			this.destination = InetAddress.getByName(d);
		}catch (UnknownHostException uhe) {
			System.out.println("Please provide a valid IP address.");
			System.exit(1);
		}catch (SecurityException se) {
			System.out.println("Please provide a valid IP address.");
			System.exit(1);
		}
	}
}