import java.io.*;
import java.util.*;
import java.net.*;

public class TcpPacket implements Serializable{

	private InetAddress source_IP;
	private short source_port;
	private InetAddress destination_IP;
	private short destination_port;
	private int sequence_number;
	private int acknowledge_number = 0;
	private short head_length;
	private boolean URG;
	private boolean ACK;
	private boolean PSH;
	private boolean RST;
	private boolean SYN;
	private boolean FIN;
	private int receive_window;
	private long check_sum;
	private int Urg_Data_Pointer;
	private byte[] data = null;

	public TcpPacket(String source_IP, short source_port, String destination_IP, short destination_port) throws IOException{
		this.source_IP = InetAddress.getByName(source_IP);
		this.source_port = source_port;
		this.destination_IP = InetAddress.getByName(destination_IP);
		this.destination_port = destination_port;
	}

	public TcpPacket(InetAddress source_IP, short source_port, InetAddress destination_IP, short destination_port) throws IOException{
		this.source_IP = source_IP;
		this.source_port = source_port;
		this.destination_IP = destination_IP;
		this.destination_port = destination_port;
	}

	public InetAddress getSourceIP() {
		return source_IP;
	}

	public short getSourcePort() {
		return source_port;
	}

	public InetAddress getDestinationIP() {
		return destination_IP;
	}

	public short getDestinationPort() {
		return destination_port;
	}

	public int getSequenceNumber() {
		return sequence_number;
	}

	public void setSequenceNumber(int seq) {
		this.sequence_number = seq;
	}

	public int getAcknowledgeNumber() {
		return acknowledge_number;
	}

	public void setAcknowledgeNumber(int ack) {
		this.acknowledge_number = ack;
	}

	public boolean getACK() {
		return this.ACK;
	}

	public void setACK() {
		this.ACK = true;
	}

	public void setSYN() {
		this.SYN = true;
	}

	public boolean getFIN() {
		return FIN;
	}

	public void setFIN() {
		this.FIN = true;
	}

	public long getChecksum() {
		return this.check_sum;
	}

	public void setChecksum(long sum){
		check_sum = sum;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] d, int len) {
		
		data = new byte[len];
		for(int i = 0; i < len; i++) {
			data[i] = d[i];
		}
	}

	public static void main(String[] args) throws IOException{
		new TcpPacket("localhost", (short)5000, "127.0.0.1", (short)6000);
		System.out.println("OK");
	}

}