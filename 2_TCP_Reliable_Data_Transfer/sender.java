import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class sender {

    private static int segments_sent = 0;
    private static int segments_retransmitted = 0;
	private static int seq_number = 0;
    private static int total_bytes_sent = 0;

    private static double alpha = 0.125; // for estimateRTT
    private static double beta = 0.25; // for devRTT
    private static double estimateRTT = 200;
    private static double devRTT = 0;
    private static double sampleRTT = 0;
    private static double time_out = estimateRTT;

	private static final int DATA_BUFF_SIZE = 576;
	private static final int BUFF_SIZE = 1040;
    
    private static final int READ_ACK_BUFF_SIZE = 1040;
    private static String log;

    private static DecimalFormat myFormatter = new DecimalFormat("###.##");

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException{
		
		if (args.length < 5) {
             System.out.println("Usage: java sender <filename> <remote_IP> <remote_port> <ack_port_num> <log_filename> <window_size>");
             System.exit(0);
        }

        // declare the variables
        String filename = new String(args[0]);
        short ack_port_num = Short.parseShort(args[3]);
        InetAddress remote_IP = InetAddress.getByName(args[1]);
        short remote_port = Short.parseShort(args[2]);
        String log_filename = new String(args[4]);

        int window_size = 1;

        if( args.length == 6 && args[5] != null){
            try{
                window_size = Integer.parseInt(args[5]);
            }catch(NumberFormatException nfe){
                System.out.println("Please provide a valid integer for <window_size> parameter.");
                window_size = 1;
            }
        }

        TcpPacket send_packet;
        TcpPacket receive_packet;

        File f1 = null;
        FileInputStream fis = null;
        try {
            f1 = new File(filename);
            fis = new FileInputStream(f1);
        }catch(FileNotFoundException fnfe){
            System.out.println("File: " + filename + " Not Found.");
            System.exit(1);
        }
        
        File f2 = null;
        FileWriter log_fw = null;
        if(!log_filename.equals("stdout")){
            f2 = new File(log_filename);
            log_fw = new FileWriter(f2, true);
        }
        
        byte[] data_buf = new byte[DATA_BUFF_SIZE];
        byte[] buf = new byte[BUFF_SIZE];
        byte[] read_ack_buff = new byte[READ_ACK_BUFF_SIZE];

        // establish the TCP socket to receive ACK
        ServerSocket server_socket = new ServerSocket(ack_port_num);
        Socket ack_socket = server_socket.accept();
        InputStream ack_socket_inputstream = ack_socket.getInputStream();
        receive_ack receive_response;

        // establish the UDP socket to send packets
        DatagramSocket udp_socket = new DatagramSocket();

        // create the TCP data field
        int result = 0;

        // keep sending if it's not yet EOF
        while(result != -1){
        	// read the file into bytes
        	result = fis.read(data_buf);
            //System.out.println("read data result: "+result);

            // make TCP packet
            send_packet = new TcpPacket(ack_socket.getLocalAddress(), (short)udp_socket.getLocalPort(), remote_IP, remote_port);

            if(result != -1) {
                total_bytes_sent += result;
                // set it to the data field in the TCP packet
                send_packet.setData(data_buf, result);
            }

            // set sequence number
            send_packet.setSequenceNumber(seq_number++);

            if(result == -1)
                send_packet.setFIN();

        	// calculate the checksum
            send_packet.setChecksum(0);
        	send_packet.setChecksum(computeChecksum(toByteArray(send_packet)));

            // construct the log record of the sent packet
            log = (new Date()).toString() + ", ";
            log += (send_packet.getSourceIP()).toString() + ", ";
            log += (send_packet.getDestinationIP()).toString() + ", \t";
            log += "seq # " + Integer.toString(send_packet.getSequenceNumber()) + ", ";
            log += "ACK # " + Integer.toString(send_packet.getAcknowledgeNumber()) + ", ";
            log += "sent, \tEstimateRTT: ";
            log += myFormatter.format(estimateRTT);
            log += "ms";

            // write the log
            if(log_filename.equals("stdout")){
                System.out.println(log);
            }else{
                log_fw.write(log);
                log_fw.write("\n");
            }

        	// put the TCP packet into the buffer
        	buf = toByteArray(send_packet);
        	// send the packet
        	DatagramPacket packet = new DatagramPacket(buf, buf.length, remote_IP, remote_port);
        	udp_socket.send(packet);
            double start_time = System.currentTimeMillis();
            double original_start_time = start_time;
            double end_time = 0;

            // increment the segement number
            segments_sent++;

            //System.out.println("packet buff size is : " + buf.length);

            // start the receive ack port in the sender
            read_ack_buff = new byte[READ_ACK_BUFF_SIZE];
            receive_response = new receive_ack(ack_socket_inputstream, read_ack_buff);
            receive_response.start();
            //System.out.println("wait for receiving ack");

            //if(receive_response.isRunning())
            //    System.out.println("Thread is running");
            //else
            //    System.out.println("Thread is NOT running");

            // receive the ACK tcp packet
            while(receive_response.isRunning()){

                end_time = System.currentTimeMillis();

                if(end_time - start_time > time_out) {
                    //resend the packet
                    udp_socket.send(packet);
                    start_time = System.currentTimeMillis();

                    // increment the retransmit number
                    segments_retransmitted++;
                    // increment the segement number
                    segments_sent++;
                }
            }

            end_time = System.currentTimeMillis();
            sampleRTT = end_time - original_start_time;
            devRTT = (1 - beta)*devRTT + beta * Math.abs(sampleRTT - estimateRTT);
            estimateRTT = (1 - alpha)*estimateRTT + alpha*sampleRTT;
            time_out = estimateRTT + devRTT;

            // now we receive the ack, convert it to a TCP packet
            receive_packet = toTcpPacket(read_ack_buff);

            log = (new Date()).toString() + ", ";
            log += (receive_packet.getSourceIP()).toString() + ", ";
            log += (receive_packet.getDestinationIP()).toString() + ", \t";
            log += "seq # " + Integer.toString(receive_packet.getSequenceNumber()) + ", ";
            log += "ACK # " + Integer.toString(receive_packet.getAcknowledgeNumber()) + ", ";
            log += "received, \tEstimateRTT: ";
            log += myFormatter.format(estimateRTT);
            log += "ms";

            // write the log
            if(log_filename.equals("stdout")){
                System.out.println(log);
            }else{
                log_fw.write(log);
                log_fw.write("\n");
            }

        }

        server_socket.close();
        ack_socket.close();
        udp_socket.close();
        fis.close();
        if(log_fw != null){
            log_fw.close();
        }
        System.out.println("Delivery completed successfully");
        System.out.println("Total bytes sent = " + total_bytes_sent);
        System.out.println("Segments sent = " + segments_sent);
        System.out.println("Segments retransmitted = " + segments_retransmitted);
	}

    private static class receive_ack extends Thread {
        
        private boolean isRunning = true;
        private InputStream ack_socket_inputstream = null;
        private byte[] read_ack_buff = null;
        private int i = -1;

        public receive_ack(InputStream ack_socket_inputstream, byte[] read_ack_buff){
            this.ack_socket_inputstream = ack_socket_inputstream;
            this.read_ack_buff = read_ack_buff;
        }

        public void run() {
            while(isRunning) {
                try{
                    //System.out.println("start to read");
                    i = ack_socket_inputstream.read(read_ack_buff);
                    //System.out.println("read over");
                    if(i != -1){
                        //System.out.println("I read something!!! Bye!!!");
                        isRunning = false;
                    }
                }catch(IOException ioe){
                    System.out.println("IOException has been caught.");
                    //isRunning = false;
                }
            }
        }

        public boolean isRunning(){
            return isRunning;
        }

        public void stopRunning() {
            isRunning = false;
        }

    }

	/**
    * Compute the check sum based on the array of byte.
    * Usage of the bit shifting.
    */
    private static long computeChecksum( byte[] buf ){
        int length = buf.length;
        int i = 0;
        long sum = 0;
        long data;

        // We keep looping all the bytes until we get to the last bit of 0 or 1
        while( length > 1 ){
            // bit shifting and bit & operation
            data = ( ((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
            
            // sum up
            sum += data;

            // if the & operation of sum is larger than zero,
            // we replace the sum, and move the MSB to the right
            if( (sum & 0xFFFF0000) > 0 ){
                sum = sum & 0xFFFF;
                sum += 1;
            }
            i += 2;
            length -= 2;
        }

        // the last 8 bits
        if (length > 0 ){ 

            // continue to do the bit operation
            // the 16 bit word has a LSB, we add it to the sum
            sum += (buf[i] << 8 & 0xFF00);
            if( (sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }
        }

        // negate the sum
        sum = ~sum; 
        // move the MSB to the right
        sum = sum & 0xFFFF;
        // return the computed check sum
        return sum;
    }

    // convert a TCP Packet to a byte array
    public static byte[] toByteArray(TcpPacket pkt) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(pkt);
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

    // convert a byte array to a TCP packet
    public static TcpPacket toTcpPacket(byte[] bytes) throws IOException, ClassNotFoundException {
        TcpPacket packet = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            packet = (TcpPacket)ois.readObject();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return packet;
    }


}