------------------------------------------------------------------------------------------------
Project Documentation:

1. Compile and Run the Project:

    $ make
    $ java bfclient <local_port> <timeout> <ip_address1> <port1> <weight1> ...
    
    For example,
    $ make
    $ java bfclient 4115 3 192.168.0.101 4116 10

2. Program Features:

    a. Creation of stable initial distributed routing table with a network
    b. Correct effect of SHOWRT command
    c. Correct effect of LINKDOWN command
    d. Correct effect of LINKUP command
    e. Usage of CLOSE command, correct effect under special case (explain below)
    f. Support dynamic network
    g. No proper error messages when required
    h. Code quality (divide different classes and functions into different files)
    i. Textutual description of design protocol including syntax and semantics

3. Usage Scenarios:

	Create the provided scenario from the PA3 pdf on the same local machine:
	$ make clean && make
	$ java bfclient 4115 3 192.168.0.104 4116 5 192.168.0.104 4118 30
	$ java bfclient 4116 3 192.168.0.104 4115 5 192.168.0.104 4118 5 192.168.0.104 4117 10
	$ java bfclient 4117 3 192.168.0.104 4116 10
	$ java bfclient 4118 3 192.168.0.104 4115 30 192.168.0.104 4116 5

	Assume: client on 4115 would LINKDOWN 4116 and then LINKUP 4116.
	(1) output from 4115 terminal:

	Initial:
	>> SHOWRT
	Destination = /192.168.0.104:4116, Cost: 5.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4118, Cost: 10.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4117, Cost: 15.0, Link = (/192.168.0.104:4116)
	
	After link-down 4116
	>> LINKDOWN 192.168.0.104 4116
	>> SHOWRT
	Destination = /192.168.0.104:4116, Cost: 35.0, Link = (/192.168.0.104:4118)
	Destination = /192.168.0.104:4118, Cost: 30.0, Link = (/192.168.0.104:4118)
	Destination = /192.168.0.104:4117, Cost: 45.0, Link = (/192.168.0.104:4118)
	
	After link-up 4116
	>> LINKUP 192.168.0.104 4116
	>> SHOWRT
	Destination = /192.168.0.104:4116, Cost: 5.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4118, Cost: 10.0, Link = (/192.168.0.104:4118)
	Destination = /192.168.0.104:4117, Cost: 15.0, Link = (/192.168.0.104:4116)

	(2) output from 4116 terminal:

	Initial:
	>> SHOWRT
	Destination = /192.168.0.104:4115, Cost: 5.0, Link = (/192.168.0.104:4115)
	Destination = /192.168.0.104:4118, Cost: 5.0, Link = (/192.168.0.104:4118)
	Destination = /192.168.0.104:4117, Cost: 10.0, Link = (/192.168.0.104:4117)
	
	After link-down 4116
	>> SHOWRT
	Destination = /192.168.0.104:4115, Cost: 35.0, Link = (/192.168.0.104:4118)
	Destination = /192.168.0.104:4118, Cost: 5.0, Link = (/192.168.0.104:4118)
	Destination = /192.168.0.104:4117, Cost: 10.0, Link = (/192.168.0.104:4117)
	
	After link-up 4116
	>> SHOWRT
	Destination = /192.168.0.104:4115, Cost: 5.0, Link = (/192.168.0.104:4115)
	Destination = /192.168.0.104:4118, Cost: 5.0, Link = (/192.168.0.104:4118)
	Destination = /192.168.0.104:4117, Cost: 10.0, Link = (/192.168.0.104:4117)

	(3) output from 4117 terminal:

	Initial:
	>> SHOWRT
	Destination = /192.168.0.104:4116, Cost: 10.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4115, Cost: 15.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4118, Cost: 15.0, Link = (/192.168.0.104:4116)

	After link-down 4116
	>> SHOWRT
	Destination = /192.168.0.104:4116, Cost: 10.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4115, Cost: 45.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4118, Cost: 15.0, Link = (/192.168.0.104:4116)

	After link-up 4116
	>> SHOWRT
	Destination = /192.168.0.104:4116, Cost: 10.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4115, Cost: 15.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4118, Cost: 15.0, Link = (/192.168.0.104:4116)

	(4) output from 4118 terminal:

	Initial:
	>> SHOWRT
	Destination = /192.168.0.104:4115, Cost: 10.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4116, Cost: 5.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4117, Cost: 15.0, Link = (/192.168.0.104:4116)

	After link-down 4116
	>> SHOWRT
	Destination = /192.168.0.104:4115, Cost: 30.0, Link = (/192.168.0.104:4115)
	Destination = /192.168.0.104:4116, Cost: 5.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4117, Cost: 15.0, Link = (/192.168.0.104:4116)

	After link-up 4116
	>> SHOWRT
	Destination = /192.168.0.104:4115, Cost: 10.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4116, Cost: 5.0, Link = (/192.168.0.104:4116)
	Destination = /192.168.0.104:4117, Cost: 15.0, Link = (/192.168.0.104:4116)

	(5) CLOSE command usage:
	$ make clean && make
	$ java bfclient 4115 3 192.168.0.104 4116 5 192.168.0.104 4118 30
	$ java bfclient 4116 3 192.168.0.104 4115 5 192.168.0.104 4118 5 192.168.0.104 4117 10
	$ java bfclient 4117 3 192.168.0.104 4116 10
	$ java bfclient 4118 3 192.168.0.104 4115 30 192.168.0.104 4116 5

	on 4117 terminal:
	$ CLOSE

	Then on 4115 terminal:
	>> SHOWRT
	Destination = /192.168.0.100:4116, Cost: 5.0, Link = (/192.168.0.100:4116)
	Destination = /192.168.0.100:4118, Cost: 10.0, Link = (/192.168.0.100:4116)
	Destination = /192.168.0.100:4117, Cost: Infinity, Link = (Unreachable)

	Then on 4116 terminal:
	>> SHOWRT
	Destination = /192.168.0.100:4115, Cost: 5.0, Link = (/192.168.0.100:4115)
	Destination = /192.168.0.100:4118, Cost: 5.0, Link = (/192.168.0.100:4118)
	Destination = /192.168.0.100:4117, Cost: Infinity, Link = (Unreachable)

	Then on 4118 terminal:
	>> SHOWRT
	Destination = /192.168.0.100:4115, Cost: 10.0, Link = (/192.168.0.100:4116)
	Destination = /192.168.0.100:4116, Cost: 5.0, Link = (/192.168.0.100:4116)
	Destination = /192.168.0.100:4117, Cost: Infinity, Link = (Unreachable)

	(6) Dynamic network:
	During any point of the stable network, any node can join to the network. And all the other
	nodes would update their distance vector and create an additional destination for the new
	node.

------------------------------------------------------------------------------------------------
Protocol Specification:

The protocol for the inter-client communications follows:
1. Each client communicate with others by sending and receiving a class object called "message".
2. The "message" object contains 4 booleans and the sender's distance vector.
3. The 4 boolean (ROUTE_UPDATE, LINKUP, LINKDOWN, CLOSE) is used to indicate what type of message
    the sender sends.
4. Upon receiving a "message", the client would response differently according to the boolean type
   in the "message".
5. If it's a ROUT_UPDATE message, then the client would simply update its own distance vector 
    by using Bellman-Ford Algorithm.
6. If it's a LINKUP or LINKDOWN message, the client would re-set the distance vector and update it.
7. If it's a CLOSE message, the client would remove the node from the neighbors list and distance
    vector.
8. There is also a timer to keep sending distance vector in a ROUTE_UPDATE message to the neighbors.
9. The message object is converted to a byte array during sending and receiving process. Then the
    receiver would convert the byte array back to the "message" object.
10. The detail implementation can be seen in the "message.java" file.
------------------------------------------------------------------------------------------------

Unusual about implementation:
1. Every client in the network should have a unique port number. No client would share the same port number even though they are on different machine. Otherwise, there may be incorrect calculation since some parts of the calculation are based on the client port number. I use port number to distinguish different client in the network.

2. Notice about CLOSE command:

    a. CLOSE command is partially implemented. 
    b. The CLOSE command works when there's only ONE node in the network closing. That being said, only ONE node can be closed while the output is correct.
    c. CLOSE command must be executed before "LINKDOWN" or "LINKUP". That means, when the network is stable after you run the program initially, you can execute "CLOSE" and you would see the correct result.
    d. If there is any "LINKUP" or "LINKDOWN" before "CLOSE", then the "CLOSE" command may have some wrong output.

3. For the LINKUP Command, due to the unreliable data transfer of a UDP packet, the LINKUP may not work at all. Thus, in this case, you can type in the LINKUP command (e.g. "LINKUP 192.168.1.100 4116") again to make sure the LINKUP message has been sent to all the nodes in the network.

------------------------------------------------------------------------------------------------

