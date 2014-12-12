package cn.fh.hello.web.websocket;

import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import cn.fh.hello.common.component.JsonWrapper;
import cn.fh.hello.web.utils.SessionCollection;

public class WebsocketEndPoint extends TextWebSocketHandler {
	public static Logger logger = LoggerFactory.getLogger(WebsocketEndPoint.class);

	/**
	 * Store Socket session in SessionCollection if this is the first message received.
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session,
			TextMessage message) throws Exception {
		
		// get json text
		String jsonStr = message.getPayload();
		
		// parse json string
		JsonWrapper jw = new JsonWrapper(jsonStr);

		String sId = jw.getValue("session");
		String text = jw.getValue("text");
		
		if (logger.isDebugEnabled()) {
			logger.debug("socket message received(original): " + jsonStr);
			logger.debug("socket message received(parsed): " + sId + "," + text);
		}

		// store SocketSession and http session id
		// if this is the first message received by particular client
		if ( false == SessionCollection.containsSocketSession(sId) ) {
			SessionCollection.putSocketSession(sId, session);
		}

		
		//WebSocketSender.sendToAll("server response message", 1000);

		
	}

	/**
	 * When a WebSocket connection is closed, remove this session from SessionCollection.
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("before size : " + SessionCollection.getSocketSessionAmount());;
		}
		
		// traverse all session map to find out the closed session
		// not the best method but there's no other way
		// following code is the traditional way to find the target session
		/*Set<Entry<String, WebSocketSession>> entrySet = SessionCollection.getSocketSessionMap().entrySet();
		String targetSessionId = null;
		for (Entry<String, WebSocketSession> entry : entrySet)  {
			if (entry.getValue().getId().equals(session.getId())) {
				targetSessionId = entry.getKey();
			}
		}
		SessionCollection.removeSocketSession(targetSessionId);*/
		
		// the JDK8 Stream way: better readability and performance
		Optional<Entry<String, WebSocketSession>> targetEntry = SessionCollection.getSocketSessionMap().entrySet()
				.parallelStream()
				//.stream()
				.filter( (Entry<String, WebSocketSession> entry) -> entry.getValue().getId().equals(session.getId()) )
				.findFirst();
		// target found
		if ( targetEntry.isPresent() ) {
			String httpSessionId = targetEntry.get().getKey();
			SessionCollection.removeSocketSession(httpSessionId);
		}



		
		if (logger.isDebugEnabled()) {
			logger.debug("after size : " + SessionCollection.getSocketSessionAmount());;
		}
	}

}
