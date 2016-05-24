package edu.upenn.cis.cis455.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

public class Request implements HttpServletRequest {
	private Properties m_params = new Properties();
	private Properties m_props = new Properties();
	private Session m_session = null;
	private String m_method;
	public HashMap<String, String> hashMap;
	String pathInfo = null;
	Socket clientSocket = null;
	public String URL = null;
	public String servlet = null;
	private String m_encoding = "ISO-8859-1";
	String body = null;
	Locale locale;
	boolean sessionFromCookie;
	Response response;
	Context context;
	static Logger logger = Logger.getLogger(Request.class);

	Request() {

	}

	Request(Session session, HashMap<String, String> map, Socket socket,
			Entry<String, String> entry, Context context, Response response) {
		m_session = session;
		hashMap = map;
		clientSocket = socket;
		URL = entry.getKey();
		servlet = entry.getValue();
		sessionFromCookie = false;
		this.response = response;
		this.context = context;
	}

	public String getAuthType() {
		return BASIC_AUTH;
	}

	public Cookie[] getCookies() {
		String cookie;
		if (hashMap.containsKey("cookie")) {
			cookie = hashMap.get("cookie");
		} else {
			return null;
		}
		String[] cookieParts = cookie.split(";");
		Cookie[] cookies = new Cookie[cookieParts.length];
		int i = 0;
		for (String part : cookieParts) {
			String[] singleCookie = part.split("=");
			if (singleCookie[0].contains("jsessionid")) {
				for (String id : Worker.stringToSession.keySet()) {
					if (singleCookie[1].equals(id)) {
						m_session = Worker.stringToSession.get(id);
					}
				}
			}
			Cookie cookie_new = new Cookie(singleCookie[0].trim(),
					singleCookie[1]);
			cookies[i] = cookie_new;
			i++;
		}
		return cookies;
	}

	public long getDateHeader(String arg0) {
		String header = hashMap.get(arg0);
		if (header == null) {
			return -1;
		}

		SimpleDateFormat format = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z");

		Date date = null;
		try {
			date = format.parse(header);
		} catch (ParseException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return date.getTime();
	}

	public String getHeader(String arg0) {
		String header = hashMap.get(arg0);
		if (header == null) {
			return header;
		}
		String[] multiHeader = header.split(",");
		return multiHeader[0];
	}

	public Enumeration getHeaders(String arg0) {
		String headers = hashMap.get(arg0);
		if (headers == null) {
			return null;
		}

		String[] multiHeader = headers.split(",");
		ArrayList<String> headerArrayList = new ArrayList<String>(
				Arrays.asList(multiHeader));
		return (Enumeration) headerArrayList;
	}

	public Enumeration getHeaderNames() {
		return (Enumeration) hashMap.keySet();
	}

	public int getIntHeader(String arg0) throws NumberFormatException {
		return Integer.parseInt(hashMap.get(arg0));
	}

	public String getMethod() {
		return m_method;
	}

	public String getPathInfo() {
		String path = hashMap.get("path");
		String pathInfo = "";
		if (path.equals(URL)
				|| path.substring(0, path.indexOf("?")).equals(URL)) {
			return null;
		}

		else {
			pathInfo = path.substring(URL.length() - 1, path.indexOf("?"));
			if (!pathInfo.startsWith("/"))
				pathInfo = "/" + pathInfo;
			return pathInfo;
		}
	}

	public String getContextPath() {
		return "";
	}

	public String getQueryString() {
		String path = hashMap.get("path");
		if (!path.contains("?"))
			return null;
		else
			return path.split("\\?")[1];
	}

	public String getRemoteUser() {
		return null;
	}

	public String getRequestedSessionId() {
		return m_session.getId();
	}

	public String getRequestURI() {
		String requestURI = "";
		String path = hashMap.get("path");
		if (path.startsWith("http://")) {
			path = path.substring(path.indexOf("//") + 2);
			path = path.split("/", 2)[1];
		}
		requestURI = path.split("\\?")[0];
		if (!requestURI.startsWith("/")) {
			requestURI = "/" + requestURI;
		}
		return requestURI;
	}

	public StringBuffer getRequestURL() {
		StringBuffer url = new StringBuffer();
		String hostName = clientSocket.getInetAddress().getHostName();
		String requestURI = getRequestURI();
		String queryString = getQueryString();
		int port = clientSocket.getPort();
		url.append("http://" + hostName + ":" + port);
		if (requestURI != null)
			url.append("/" + requestURI);
		if (queryString != null)
			url.append("?" + queryString);
		return url;
	}

	public String getServletPath() {
		return URL;
	}

	public HttpSession getSession(boolean arg0) {
		if (response.isCommitted()) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}

		getCookies();
		if (arg0) {
			if (hasSession()) {
				System.out.println("session is there.");
				sessionFromCookie = true;
				m_session.putValue("isNew", false);
				long lastAccess = System.currentTimeMillis();
				Worker.sessionLastAccess.put(m_session, lastAccess);
				return m_session;

			} else {
				if (!hasSession()) {
					sessionFromCookie = false;
					long lastAccess = System.currentTimeMillis();
					String sessionID = UUID.randomUUID().toString();
					m_session = new Session(context, sessionID, lastAccess);
					m_session.putValue("isNew", true);
					m_session.setAttribute("id", sessionID);
					Worker.stringToSession.put(sessionID, m_session);
					Worker.sessionLastAccess.put(m_session, lastAccess);
				}
			}

			Cookie cookie = new Cookie("jsessionid", m_session.getId());
			cookie.setMaxAge(4000);
			response.addCookie(cookie);
			return m_session;
		} else {
			if (!hasSession())
				return null;
		}
		return m_session;
	}

	public HttpSession getSession() {
		return getSession(true);
	}

	public boolean isRequestedSessionIdValid() {
		return m_session.isValid();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return sessionFromCookie;
	}

	public boolean isRequestedSessionIdFromUrl() {
		return !sessionFromCookie;
	}

	public Object getAttribute(String arg0) {
		return m_props.get(arg0);
	}

	public Enumeration getAttributeNames() {
		return m_props.keys();
	}

	public String getCharacterEncoding() {
		return m_encoding;
	}

	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		logger.error("UnsupportedCodingException");
		m_encoding = arg0;
	}

	public int getContentLength() {
		if (hashMap.containsKey("content-length")) {
			return Integer.parseInt(hashMap.get("content-length"));
		} else {
			return -1;
		}
	}

	public String getContentType() {
		if (hashMap.containsKey("content-type")) {
			return hashMap.get("content-type");
		} else {
			return null;
		}
	}

	public String getParameter(String arg0) {
		return m_params.getProperty(arg0);
	}

	public Enumeration getParameterNames() {
		return (Enumeration) m_params.keys();
	}

	public String[] getParameterValues(String arg0) {
		String value = m_params.getProperty(arg0);
		if (value == null)
			return null;
		String values[] = value.split(",");
		return values;
	}

	public Map getParameterMap() {
		HashMap<String, String> paramMap = new HashMap<>();
		for (Object param : m_params.keySet()) {
			paramMap.put(param.toString(),
					m_params.getProperty(param.toString()));
		}
		return paramMap;
	}

	public String getProtocol() {
		return hashMap.get("version");
	}

	public String getScheme() {
		return "http";
	}

	public String getServerName() {
		if (hashMap.containsKey("host"))
			return hashMap.get("host").split(":")[0];
		else {
			return clientSocket.getInetAddress().getHostName();
		}
	}

	public int getServerPort() {
		if (hashMap.containsKey("host"))
			return Integer.parseInt(hashMap.get("host").split(":")[1]);
		else {
			return clientSocket.getPort();
		}
	}

	public BufferedReader getReader() throws IOException {
		BufferedReader br = new BufferedReader(new StringReader(body));
		return br;
	}

	public String getRemoteAddr() {
		return clientSocket.getLocalAddress().getHostAddress();
	}

	public String getRemoteHost() {
		return clientSocket.getLocalAddress().getHostName() + ":"
				+ clientSocket.getLocalPort();
	}

	public void setAttribute(String arg0, Object arg1) {
		m_props.put(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		m_props.remove(arg0);
	}

	public boolean isSecure() {
		return false;
	}

	public String getRealPath(String arg0) {
		return null;
	}

	public int getRemotePort() {
		return clientSocket.getPort();
	}

	public String getLocalName() {
		return clientSocket.getInetAddress().getHostName();
	}

	public String getLocalAddr() {
		return clientSocket.getLocalAddress().toString();
	}

	public int getLocalPort() {
		return clientSocket.getLocalPort();
	}

	void setMethod(String method) {
		m_method = method;
	}

	void setParameter(String key, String value) {
		m_params.setProperty(key, value);
	}

	void clearParameters() {
		m_params.clear();
	}

	boolean hasSession() {
		if (m_session == null || !m_session.isValid()) {
			return false;
		}
		return true;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setBody(String arg0) {
		body = arg0;
	}

	// Not to be implemented
	public boolean isUserInRole(String arg0) {
		return false;
	}

	// Not to be implemented
	public Principal getUserPrincipal() {
		return null;
	}

	// Deprecated
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	// Not to be implemented
	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	// Not to be implemented
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	// Not to be implemented
	public String getPathTranslated() {
		return null;
	}

	// Not to be implemented
	public Enumeration getLocales() {
		return null;
	}

}
