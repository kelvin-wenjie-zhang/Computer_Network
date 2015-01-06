Project Documentation:
1) To execute the project, please invoke the emulator first, and then invoke the sender,
   and then invoke the receiver at the end.

2) To compile the project:
	- type ÒmakeÓ at the command line

3) To run the project:
	$ ./newudpl -o <remote_IP>/<remote_port> -i <sender_IP>/* -p20000:6000 -L 50 -B 50000 -d1
	$ java sender <filename> <remote_IP> <remote_port> <ack_port_num> <log_filename> <window_size>
	$ java receiver <filename> <listening_port> <sender_IP> <sender_port> <log_filename>

AGAIN, run the sender before the receiver.

4) Program features:
	a. Sender invoked in given format
	b. Receiver invoked in given format
	c. Easy to read documentation / make file
	d. In order delivery
	e. Handle loss packets
	f. Handle corrupted packets
	g. Handle delayed packets
	h. Handle duplicate packets
	i. Log file maintained on sender side following the given format
	j. Log file maintained on receiver side following the given format
	k. Statistics on sender is printed properly
	l. Files are the same in both receiver and sender
	m. No fatal errors / exceptions


5) Usage scenarios:
	Assume: - The receiver and the proxy is running on 128.59.15.77.
		- The proxy listens on port 20000 and forward packets to port 6000
		- The receiver listens on port 20002
		- The sender runs on 160.39.132.111 and receives ACK on port 20001

	To compile and run the project:
	$ make clean
	$ make
	$ ./newudpl -o 128.59.15.77/20002 -i 160.39.132.111/* -p20000:6000 -L 50 -B 50000 -d1
	$ java sender test.txt 128.59.15.77 20000 20001 log_sender.txt 1
	$ java receiver test_receiver.txt 20002 160.39.132.111 20001 log_receiver.txt

	- Output of the sender:
	Delivery completed successfully
	Total bytes sent = 7215
	Segments sent = 65
	Segments retransmitted = 51

	- Output of the receiver:
	Delivery completed successfully

	- Sample of the log of the sender:
	Tue Nov 04 16:23:29 EST 2014, /160.39.132.111, /128.59.15.77, 	seq # 2, ACK # 0, sent, 	EstimateRTT: 310.53ms
	Tue Nov 04 16:23:33 EST 2014, /128.59.15.77, /160.39.132.111, 	seq # 2, ACK # 2, received, 	EstimateRTT: 815.96ms
	Tue Nov 04 16:23:33 EST 2014, /160.39.132.111, /128.59.15.77, 	seq # 3, ACK # 0, sent, 	EstimateRTT: 815.96ms
	Tue Nov 04 16:23:35 EST 2014, /128.59.15.77, /160.39.132.111, 	seq # 3, ACK # 3, received, 	EstimateRTT: 917.97ms

	- Sample of the log of the receiver:
	Tue Nov 04 16:23:33 EST 2014, /128.59.15.77, /160.39.132.111,     seq # 2,     ACK # 2, sent
	Tue Nov 04 16:23:34 EST 2014, /160.39.132.111, /128.59.15.77,     seq # 3,     ACK # 0, received
	Tue Nov 04 16:23:34 EST 2014, /128.59.15.77, /160.39.132.111,     seq # 3,     ACK # 3, sent
	Tue Nov 04 16:23:35 EST 2014, /160.39.132.111, /128.59.15.77,     seq # 3,     ACK # 0, received
	Tue Nov 04 16:23:35 EST 2014, /128.59.15.77, /160.39.132.111,     seq # 4,     ACK # 3, sent
	Tue Nov 04 16:23:36 EST 2014, /160.39.132.111, /128.59.15.77,     seq # 4,     ACK # 0, received

6) a) TCP segment structure:
   - The TCP segment structure implementation is in the TcpPacket.java file. It contains a basic structure of a TCP Packet,
   including source_IP, source_port, destination_IP, destination_port, sequence_number, acknowledge_number, head_length,
   receive_window, data_buffer and the flags (URG, ACK, PSH, RST, SYN, FIN).
   - The TcpPacket class contains get() and set() functions for each variables.

   b) The states typically visited by a sender and receiver:
   - SenderÕs sent packet has ACK 0 all the time, because sender does not ACK anything.
   - Then sender would implement as wait-and-stop mechanism, i.e. the sender would send one packet at a time,
     and wait for the receiverÕs ACK packet.

   c) The loss recovery mechanism:
   - The loss packet would cause the time-out event of the sender, which causes the sender to retransmit the 
     previous packet again until the sender receives the ACK from receiver.

   d) Unusual about my implementation:
   - Please run the sender first, and then the receiver.
   - The sender is implemented as wait-and-stop mechanism. The <window_size> is 1 by default.
   - The ÒflagsÓ from the log file of the receiver is either ÒsentÓ or ÒreceiveÓ.

   e) additional features:
   - Print out the log in a easy-to-read format by using DecimalFormatter
   - Usage of serialization
   - The sender can send all kinds of file including text file.


