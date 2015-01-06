import java.io.*;
import java.net.*;
import java.util.*;

public class receiver {

	private static final int BUFFER_SIZE = 1040;
    private static final int DATA_BUFFER_SIZE = 576;
    private static FileOutputStream fos;
    private static FileWriter log_fw;
    private static boolean FIN = false;
    private static int last_ack = -1;
    private static int total_written_byte = 0;

	public static void main(String[] args) throws IOException, ClassNotFoundException{

		if (args.length != 5) {
             System.out.println("Usage: java receiver <filename> <listening_port> <sender_IP> <sender_port> <log_filename>");
             System.exit(0);
        }

        // declare the variables
        String filename;
        short listening_port = 4419; // for initialization purpose
        InetAddress sender_IP = InetAddress.getByName("localhost"); // for initialization purpose
        short sender_port = 4119; // for initialization purpose
        String log_filename;
        TcpPacket send_packet;
        TcpPacket receive_packet;
        File f1;
        File f2;
        int seq_number = 0;
        byte[] buf;
        byte[] data_buff;

        // get the command line information
        filename = new String(args[0]);
        try{
            f1 = new File(filename);
            fos = new FileOutputStream(f1, true);
        }catch(IOException ioe){
            System.out.println("cannot create " + filename);
            System.exit(1);
        }

        log_filename = new String(args[4]);
        if(!log_filename.equals("stdout")){
            try{
                f2 = new File(log_filename);
                log_fw = new FileWriter(f2, true);
            }catch(IOException ioe){
                System.out.println("cannot create " + log_filename);
                System.exit(1);
            }
        }

        buf = new byte[BUFFER_SIZE];
        data_buff = new byte[DATA_BUFFER_SIZE];
        try {
            listening_port = Short.parseShort(args[1]);
            sender_port = Short.parseShort(args[3]);
            sender_IP = InetAddress.getByName(args[2]);
        }catch (IOException ioe) {
            System.out.println("Not valid port number or IP address.");
            System.exit(1);
        }

        // create a datagram socket
        DatagramSocket socket = new DatagramSocket(listening_port);

        // establish the TCP socket to send ACK
        Socket receiver_to_sender = new Socket(sender_IP, sender_port);
        OutputStream socketOutputStream = receiver_to_sender.getOutputStream();

        while(FIN == false) {
            // listen to the sender and receive a packet
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            //System.out.println("receive packet in a size of : " + buf.length);

            // restore the TCP packet from byte array
            try{
                receive_packet = toTcpPacket(packet.getData());
            }catch(StreamCorruptedException sce){
                //System.out.println("The packet is corrputed. Wait for timeout.");
                continue;
            }

            if(receive_packet.getFIN() == true)
                FIN = true;

            // check if the packet is duplicate of the last received packet.
            // if it's a duplicate, do nothing
            if( receive_packet.getSequenceNumber() == last_ack ){
                continue;
            }

            // checksum routine
            long getchecksum = receive_packet.getChecksum();
            receive_packet.setChecksum(0);
            long computechecksum = computeChecksum(toByteArray(receive_packet));

            
            // if the receiver has a same checksum calculated as the sender's checksum
            if(getchecksum == computechecksum) {
                
                String log = (new Date()).toString() + ", ";
                log += (receive_packet.getSourceIP()).toString() + ", ";
                log += (receive_packet.getDestinationIP()).toString() + ", \t";
                log += "seq # " + Integer.toString(receive_packet.getSequenceNumber()) + ", \t";
                log += "ACK # " + Integer.toString(receive_packet.getAcknowledgeNumber()) + ", ";
                log += "received";

                // write the log
                if(log_filename.equals("stdout")){
                    System.out.println(log);
                }else{
                    log_fw.write(log);
                    log_fw.write("\n");
                }

                send_packet = new TcpPacket(receive_packet.getDestinationIP(), receive_packet.getDestinationPort(), sender_IP, sender_port);
                send_packet.setAcknowledgeNumber(receive_packet.getSequenceNumber());
                send_packet.setSequenceNumber(seq_number++);
                send_packet.setACK();

                byte[] response_buf = toByteArray(send_packet);

                socketOutputStream.write(response_buf);

                log = (new Date()).toString() + ", ";
                log += (send_packet.getSourceIP()).toString() + ", ";
                log += (send_packet.getDestinationIP()).toString() + ", \t";
                log += "seq # " + Integer.toString(send_packet.getSequenceNumber()) + ", \t";
                log += "ACK # " + Integer.toString(send_packet.getAcknowledgeNumber()) + ", ";
                log += "sent";

                // write the log
                if(log_filename.equals("stdout")){
                    System.out.println(log);
                }else{
                    log_fw.write(log);
                    log_fw.write("\n");
                }

                // extract the data and write to the file
                if( receive_packet.getFIN() != true && receive_packet.getData() != null){
                    data_buff = receive_packet.getData();
                    fos.write(data_buff);

                    total_written_byte += data_buff.length;
                    //System.out.println("total byte written: " + total_written_byte);
                }

                // update the last_ack number
                last_ack = receive_packet.getSequenceNumber();

            }else{
                System.out.println("checksum is different. Drop.");
            }

        }

        // report the result
        System.out.println("Delivery completed successfully");

        // close the file output stream and socket
        receiver_to_sender.close();
        fos.close();
        if(log_fw != null){
            log_fw.close();
        }
        socket.close();

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
        } 
        finally {
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