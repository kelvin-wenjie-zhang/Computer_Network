Name: Wenjie Zhang
UNI: wz2261

a. A brief description of your code

My code can basically satisfy most of the requirement of the assignment.
The functionality includes:
— successful log in
— multiple clients support by multi-threads programming
— implementation of whoelse
— implementation of logout
— implementation of wholasthr
— implementation of broadcast:
  — Every online user can receive the broadcast message instantly.
— implementation of private message
  — Private message can be sent successfully. The receiver can view it instantly.
— graceful exit of client and server using control+c
— code quality is decent and satisfactory 

b. Details on development environment

java version “1.6.0_65”
IDE: Sublime Text 2
System: Mac OS X Version 10.9.5

c. Instructions on how to run your code

— open terminal or other command line bash shell
— go to the directory that contains my source code
— type “make”
— type “java Server 4119” to run the server
— type “java Client <Server_IP_Address> 4119” to run the client

d. Sample commands to invoke your code

Here’s the scenario:
1. First, seas user runs “java Client <Server_IP_Address> 4119”
   and logs into the console and then “Command:” shows up.
   Here’s what its terminal would show:
    Username: 
    seas
    Password: 
    summerisover
    Welcome to simple chat server!
    Command: 
2. Second, columbia user does the same thing as seas user.
3. Third, wikipedia user does the same thing as seas user.
4. seas user types “whoelse” and then “wholasthr”, here’s what its terminal would show:
    Command: 
    whoelse
    columbia, wikipedia, 
    Command: 
    wholasthr
    columbia, seas, wikipedia, 
    Command: 
5. seas user types “broadcast hello world”.
    1) on columbia user console:
    Username: 
    columbia
    Password: 
    116bway
    Welcome to simple chat server!
    Command: 
    seas: hello world
    2) on wikipedia user console:
    Username: 
    wikipedia
    Password: 
    donation
    Welcome to simple chat server!
    Command: 
    seas: hello world
6. seas user types “message columbia how are you???”
1) on columbia user console:
    Username: 
    columbia
    Password: 
    116bway
    Welcome to simple chat server!
    Command: 
    seas: hello world
    seas: how are you???
    2) on wikipedia user console:
    Username: 
    wikipedia
    Password: 
    donation
    Welcome to simple chat server!
    Command: 
    seas: hello world
7. seas types “logout”. 
   on seas user console:
    Command: 
    logout

    Log out successfully.

e. Description of any additional functionalities and how they should be executed/tested
— The inactivity function may not be correct, although I use the TIME_OUT variable to check if the user is inactive for 30 minutes.

— If the user has 3 consecutive failure log-in, the user would be blocked and cannot log in for BLOCK_TIME minutes.

— The server CANNOT sign in multiple users simultaneously. The server can only sign in the user one by one. Thus, when creating multiple users, please make sure to sign in the users one by one. For example, sign in seas and “Command” shows up, then sign in columbia and “Command” shows up, then sign in wikipedia and “Command” shows up.

— If the “make” in makefile does not work, you can compile the code in this way:
javac Server.java
javac Client.java




