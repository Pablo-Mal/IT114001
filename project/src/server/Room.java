package server;

import java.util.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Room implements AutoCloseable {
    private static SocketServer server;// used to refer to accessible server functions
    private String name;
    private final static Logger log = Logger.getLogger(Room.class.getName());

    // Commands
    private final static String COMMAND_TRIGGER = "/";
    private final static String CREATE_ROOM = "createroom";
    private final static String JOIN_ROOM = "joinroom";
    
    Random randomNum = new Random();
    private final static String ROLL_DIE = "roll";
    private int roll;
    private final static String FLIP_COIN = "flipcoin";
    private int num;
    private final static String DM = "@";
    private final static String MUTE = "mute";
    private final static String UNMUTE = "unmute";
    

    public Room(String name) {
	this.name = name;
    }

    public static void setServer(SocketServer server) {
	Room.server = server;
    }

    public String getName() {
	return name;
    }

    private List<ServerThread> clients = new ArrayList<ServerThread>();

    protected synchronized void addClient(ServerThread client) {
	client.setCurrentRoom(this);
	if (clients.indexOf(client) > -1) {
	    log.log(Level.INFO, "Attempting to add a client that already exists");
	}
	else {
	    clients.add(client);
	    if (client.getClientName() != null) {
		client.sendClearList();
		sendConnectionStatus(client, true, "joined the room " + getName());
		updateClientList(client);
	    }
	}
    }
    
    private void updateClientList(ServerThread client) {
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread c = iter.next();
	    if (c != client) {
		boolean messageSent = client.sendConnectionStatus(c.getClientName(), true, null);
	    }
	}
    }

    protected synchronized void removeClient(ServerThread client) {
	clients.remove(client);
	if (clients.size() > 0) {
	    // sendMessage(client, "left the room");
	    sendConnectionStatus(client, false, "left the room " + getName());
	}
	else {
	    cleanupEmptyRoom();
	}
    }

    private void cleanupEmptyRoom() {
	// If name is null it's already been closed. And don't close the Lobby
	if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
	    return;
	}
	try {
	    log.log(Level.INFO, "Closing empty room: " + name);
	    close();
	}
	catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    protected void joinRoom(String room, ServerThread client) {
	server.joinRoom(room, client);
    }

    protected void joinLobby(ServerThread client) {
	server.joinLobby(client);
    }

    /***
     * Helper function to process messages to trigger different functionality.
     * 
     * @param message The original message being sent
     * @param client  The sender of the message (since they'll be the ones
     *                triggering the actions)
     */
    private boolean processCommands(String message, ServerThread client) {
	boolean wasCommand = false;
	Iterator<ServerThread> iter = clients.iterator();
	try {
	    if (message.indexOf(COMMAND_TRIGGER) > -1) {
		String[] comm = message.split(COMMAND_TRIGGER);
		log.log(Level.INFO, message);
		String part1 = comm[1];
		String[] comm2 = part1.split(" ");
		String command = comm2[0];
		
		
		if (command != null) {
		    command = command.toLowerCase();
		}
		String roomName;
		String clientMuteUnmute;
		switch (command) {
		case CREATE_ROOM:
		    roomName = comm2[1];
		    if (server.createNewRoom(roomName)) {
			joinRoom(roomName, client);
		    }
		    wasCommand = true;
		    break;
		case JOIN_ROOM:
		    roomName = comm2[1];
		    joinRoom(roomName, client);
		    wasCommand = true;
		    break;
		case FLIP_COIN:
			num = randomNum.nextInt(2);
			if (num == 0) {
				sendMessage(client, "<b style=color:orange><i> You flipped HEADS </i></b>");
			}else {
				sendMessage(client, "<b style=color:orange><i> You flipped TALES </i></b>");
			}
			wasCommand = true;
			break;
		case ROLL_DIE:
			roll = randomNum.nextInt(6)+1;
			sendMessage(client, "<b style=color:orange><i> You rolled a " + roll +" </i></b>");
			wasCommand = true;
			break;
		case MUTE:
			clientMuteUnmute = comm2[1];
			if (!client.getClientName().equals(clientMuteUnmute)) {
				client.mutedClients.add(clientMuteUnmute);
				while (iter.hasNext()) {
					ServerThread mutedC = iter.next();
					if (mutedC.getClientName().equals(clientMuteUnmute)) {
						mutedC.send("[Notification] ",client.getClientName() + " has MuTed you");
						client.send("[Notification] ",mutedC.getClientName() + " has been muted");
					}
				}
			}
			wasCommand = true;
			break; 
		case UNMUTE:
			clientMuteUnmute = comm2[1];
			client.mutedClients.remove(clientMuteUnmute);
			while (iter.hasNext()) {
					ServerThread mutedC = iter.next();
					if (mutedC.getClientName().equals(clientMuteUnmute)) {
						mutedC.send("[Notification] ",client.getClientName() + " has UnMUted you");
						client.send("[Notification] ",mutedC.getClientName() + " has been UnMuted");
					}
				}
				wasCommand = true;
				break;
			}	
		}
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
	return wasCommand;
    }
    
    protected boolean processPrivateMessage(String message, ServerThread client) {
    	boolean wasPrivate = false;
    	String privClient = null;
    	String newMessage = message;
    	try {
    		if (message.indexOf(DM) > -1) 
    		{
    			String[] comm = message.split(DM);
    			log.log(Level.INFO, message);
    			String part1 = comm[1];
    			String[] comm2 = part1.split(":");
    			privClient = comm2[0];
    			newMessage = comm2[1];
    			wasPrivate = true;
    			}
    			
    		Iterator<ServerThread> iter = clients.iterator();
    		while (iter.hasNext()) {
    			ServerThread c = iter.next();
    			if (c.getClientName().equals(privClient)) {
    				c.send(client.getClientName(), "PrivateMessageReceive" + newMessage);
    				client.send(client.getClientName(), "PrivateMessageSent" + newMessage);
    			}
    		}
    	}  catch (Exception e) {
    	    e.printStackTrace();
    	}
    	return wasPrivate;
	}

    // TODO changed from string to ServerThread
    protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread c = iter.next();
	    boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
	    if (!messageSent) {
		iter.remove();
		log.log(Level.INFO, "Removed client " + c.getId());
	    }
	}
    }

    /***
     * Takes a sender and a message and broadcasts the message to all clients in
     * this room. Client is mostly passed for command purposes but we can also use
     * it to extract other client info.
     * 
     * @param sender  The client sending the message
     * @param message The message to broadcast inside the room
     */
    protected void sendMessage(ServerThread sender, String message) {
	log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
	if (processCommands(message, sender)) {
	    // it was a command, don't broadcast
	    return;
	}
	if (processPrivateMessage(message, sender)) {
		return;
	}
	
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread client = iter.next();
	    
	    if (!client.isMuted(sender.getClientName())) {
	    	boolean messageSent = client.send(sender.getClientName(), message);
	    	if (!messageSent) {
	    
	    		iter.remove();
	    		
	    	}
	    }
	}
    }

    /***
     * Will attempt to migrate any remaining clients to the Lobby room. Will then
     * set references to null and should be eligible for garbage collection
     */
    @Override
    public void close() throws Exception {
	int clientCount = clients.size();
	if (clientCount > 0) {
	    log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
	    Iterator<ServerThread> iter = clients.iterator();
	    Room lobby = server.getLobby();
	    while (iter.hasNext()) {
		ServerThread client = iter.next();
		lobby.addClient(client);
		iter.remove();
	    }
	    log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
	}
	server.cleanupRoom(this);
	name = null;
	// should be eligible for garbage collection now
    }

}