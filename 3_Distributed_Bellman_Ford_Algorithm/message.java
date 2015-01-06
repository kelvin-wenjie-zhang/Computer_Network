// contains the type of the message:
// ROUTE UPDATE, LINKUP, LINKDOWN, CLOSE

import java.io.*;
import java.util.*;
import java.net.*;

public class message implements Serializable {

	// indicate the type of the message
	public boolean ROUTE_UPDATE;
	public boolean LINKUP;
	public boolean LINKDOWN;
	public boolean CLOSE;
	public int close_port;
	public int link_up_port;

	// the distance vector
	public ArrayList<vector> distance_vector;

	public message() {
		this.ROUTE_UPDATE = false;
		this.LINKUP = false;
		this.LINKDOWN = false;
		this.CLOSE = false;
		this.close_port = -1;
		this.link_up_port = -1;
		this.distance_vector = new ArrayList<vector>();
	}

	public void setDistanceVector(ArrayList<vector> dv){
		this.distance_vector.clear();
		for(int i = 0; i < dv.size(); i++){
			vector v = dv.get(i);
			distance_vector.add(new vector(v.destination, v.cost, v.port));
		}
	}
}