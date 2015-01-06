JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	vector.java \
	node.java \
	message.java \
	timeout.java \
	listen_socket.java \
	bfclient.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class *~

