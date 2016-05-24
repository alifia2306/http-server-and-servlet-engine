package edu.upenn.cis.cis455.webserver;

import java.util.Date;

import org.apache.log4j.Logger;

public class SessionTimer implements Runnable {

	static final Logger logger = Logger.getLogger(SessionTimer.class);

	long currentTime = System.currentTimeMillis();
	private long maxInactiveTime;
	private long lastAccess;

	public void run() {

		try {
			Thread.sleep(10000);
		}

		catch (InterruptedException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		for (String sessionID : Worker.stringToSession.keySet()) {
			Session session = Worker.stringToSession.get(sessionID);
			maxInactiveTime = session.maxInactiveInterval * 1000;
			lastAccess = session.lastAccessed;
			if ((currentTime - lastAccess) > maxInactiveTime) {
				session.invalidate();
				Worker.stringToSession.remove(sessionID);
			}
		}

	}

}