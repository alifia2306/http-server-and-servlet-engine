package edu.upenn.cis.cis455.webserver;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.apache.log4j.Logger;

public class Session implements HttpSession {
	private Properties m_props = new Properties();
	private boolean m_valid = true;
	long currentTime;
	Context context;
	String sessionID;
	long lastAccessed;
	int maxInactiveInterval = 0;
	static final Logger logger = Logger.getLogger(Session.class);

	Session(Context context, String sessionID, long lastAccessed) {

		currentTime = System.currentTimeMillis();
		this.context = context;
		this.lastAccessed = lastAccessed;
		this.sessionID = sessionID;
	}

	public long getCreationTime() {
		return currentTime;
	}

	public String getId() {
		return sessionID;
	}

	public long getLastAccessedTime() {
		if (m_valid == false) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		return Worker.sessionLastAccess.get(this);
	}

	public ServletContext getServletContext() {
		return context;
	}

	public void setMaxInactiveInterval(int arg0) {
		maxInactiveInterval = arg0;
	}

	public int getMaxInactiveInterval() {

		return maxInactiveInterval;
	}

	public HttpSessionContext getSessionContext() {
		return null;
	}

	public Object getAttribute(String arg0) {
		if (m_valid == false) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		return m_props.get(arg0);
	}

	public Object getValue(String arg0) {
		if (m_valid == false) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		return m_props.get(arg0);
	}

	public Enumeration getAttributeNames() {
		if (m_valid == false) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		return m_props.keys();
	}

	// Deprecated
	public String[] getValueNames() {
		return null;
	}

	public void setAttribute(String arg0, Object arg1) {
		if (m_valid == false) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		m_props.put(arg0, arg1);
		System.out.println("attribute1:  " + arg0);
		System.out.println("attribute2:  " + arg0);
	}

	public void putValue(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		if (m_valid == false) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		m_props.remove(arg0);
	}

	// Deprecated
	public void removeValue(String arg0) {
		m_props.remove(arg0);
	}

	public void invalidate() {
		if (m_valid == false) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		m_valid = false;
	}

	public boolean isNew() {
		return (boolean) m_props.get("isnew");
	}

	boolean isValid() {
		return m_valid;
	}

}
