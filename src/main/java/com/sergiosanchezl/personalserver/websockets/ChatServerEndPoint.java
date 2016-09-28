package com.sergiosanchezl.personalserver.websockets;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

@ServerEndpoint(value="/chat/{nickname}", configurator=ChatServerEndPointConfigurator.class)
public class ChatServerEndPoint {
    
    private Set<Session> userSessions = Collections.synchronizedSet(new HashSet<Session>());
    private ConcurrentHashMap<String,String> sessionToNickname = new ConcurrentHashMap<>();
    
    // Array of 16 colors to be used by the users inside the chat.
    private String[] colors = {"#b58900","#cb4b16","#dc322f","#d33682","#6c71c4","#268bd2",
    		"#2aa198","#859900","#f0b500","#e7581e","#e25350","#da5897","#484eb2","#64aee3",
    		"#49d0c5","#c3e000"};
    private DateFormat dateFormat = new SimpleDateFormat("HH:mm");
    
    /**
     * Callback hook for Connection open events. This method will be invoked when a 
     * client requests for a WebSocket connection.
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(@PathParam("nickname") String rawNickname, Session userSession) {
    	
    	String nickname = rawNickname;
		try {
			nickname = escapeHtml4(URLDecoder.decode(rawNickname, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
		}
    	String time = dateFormat.format(new Date());
    	String peopleInside = "";
    	String color = colors[Integer.parseInt(userSession.getId()) % 16];
    	
    	String preparedMessage = "<div class=\"incoming\">"	+ "<span class=\"nickname\" style=\"color: " 
    			+ color + ";\">" + nickname + "</span>" + " has joined the chatroom." + "<span class=\"time\">" 
    			+ time + "</span></div>";
    	
    	for(Session session : userSessions) {
    		// Send the message to each session to tell someone new has joined.
    		session.getAsyncRemote().sendText(preparedMessage);
    		// Now that we are checking all sessions, we add them to a string to show the list of people inside.
    		String sessionNickname = sessionToNickname.get(session.getId());
    		color = colors[Integer.parseInt(session.getId()) % 16];
    		peopleInside += "<span class=\"nickname\" style=\"color: " + color + ";\">" + sessionNickname + "</span> ";
    	}
    	// We add the new user to the chat, along with its nickname.
        userSessions.add(userSession);
        sessionToNickname.put(userSession.getId(), nickname);
        
        // Prepare the new user's color.
        color = colors[Integer.parseInt(userSession.getId()) % 16];
        // Prepare the message to show the welcome and the people inside.
        preparedMessage = "<div class=\"incoming\">" + "Welcome " + "<span class=\"nickname\" style=\"color: " 
        		+ color + ";\">" + nickname + "</span>" + "!<br> People currently inside: " + peopleInside 
        		+ "<span class=\"time\">" + time + "</span></div>";
        // Send the welcome message to the new user.
        userSession.getAsyncRemote().sendText(preparedMessage);
    }
     
    /**
     * Callback hook for Connection close events. This method will be invoked when a
     * client closes a WebSocket connection.
     * @param userSession the userSession which is opened.
     */
    @OnClose
    public void onClose(Session userSession) {
    	// Get the time.
    	String time = dateFormat.format(new Date());
    	// Get the color of the user who is leaving.
    	String color = colors[Integer.parseInt(userSession.getId()) % 16];
    	// Get the nickname of the user who is leaving.
    	String nickname = sessionToNickname.get(userSession.getId());
    	// Prepare the "{user} has left the room" message.
    	String preparedMessage = "<div class=\"incoming\"><span class=\"nickname\" style=\"color: " + color + ";\">" + nickname + "</span> has left the chatroom." + "<span class=\"time\">" + time + "</span></div>";
        // Remove the user from the chat.
    	userSessions.remove(userSession);
        sessionToNickname.remove(userSession.getId());
        // Send the message to all remaining users.
        for(Session session : userSessions) {
        	session.getAsyncRemote().sendText(preparedMessage);
        }
    }
     
    /**
     * Callback hook for Message Events. This method will be invoked when a client
     * send a message.
     * @param message The text message
     * @param userSession The session of the client
     */
    @OnMessage
    public void onMessage(String message, Session userSession) {
        System.out.println("Message Received: " + message);
        
        // Process the message to eliminate html tags.
        String preparedMessage = escapeHtml4(message);
        // Get the color for the user
        String color = colors[Integer.parseInt(userSession.getId()) % 16];
        // Get the nickname of the user
        String nickname = sessionToNickname.get(userSession.getId());
        // Get the time
        String time = dateFormat.format(new Date());
        
        for (Session session : userSessions) {
            System.out.println("Sending to " + session.getId());
            // If the session is the user that sent the message, mark it as outgoing.
            if(session.getId().equals(userSession.getId())) {
            	preparedMessage = "<div class=\"outgoing\">"
            			+ "<span class=\"nickname\" style=\"color: " + color + ";\">" + nickname + ": </span>"
            			+ escapeHtml4(message) + "<span class=\"time\">" + time + "</span></div>";
        	} else {
        		// Else, mark it as incoming.
        		preparedMessage = "<div class=\"incoming\">"
            			+ "<span class=\"nickname\" style=\"color: " + color + ";\">" + nickname + ": </span>"
            			+ escapeHtml4(message) + "<span class=\"time\">" + time + "</span></div>";
        	}
            // Send the message
            session.getAsyncRemote().sendText(preparedMessage);
        }
    }
}
