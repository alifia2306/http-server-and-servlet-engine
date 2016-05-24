package edu.upenn.cis.cis455.webserver;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

public class Context implements ServletContext {

	static Logger logger = Logger.getLogger(Context.class);
	private HashMap<String, Object> attributes;
	private HashMap<String, String> initParams;

	public Context() {
		attributes = new HashMap<String, Object>();
		initParams = new HashMap<String, String>();
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public Enumeration getAttributeNames() {
		Set<String> keys = attributes.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}

	public ServletContext getContext(String name) {
		return this;
	}

	public String getInitParameter(String name) {
		return initParams.get(name);
	}

	public Enumeration getInitParameterNames() {
		Set<String> keys = initParams.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}

	public int getMajorVersion() {
		return 2;
	}

	// Not to be implemented
	public String getMimeType(String file) {
		return null;
	}

	public int getMinorVersion() {
		return 4;
	}

	// Not to be implemented
	public RequestDispatcher getNamedDispatcher(String name) {
		return null;
	}

	
	public String getRealPath(String path) {
		return null;
	}

	// Not to be implemented
	public RequestDispatcher getRequestDispatcher(String name) {
		return null;
	}

	// Not to be implemented
	public java.net.URL getResource(String path) {
		return null;
	}

	// Not to be implemented
	public java.io.InputStream getResourceAsStream(String path) {
		return null;
	}

	// Not to be implemented
	public java.util.Set getResourcePaths(String path) {
		return null;
	}

	// Confirm
	public String getServerInfo() {
		return "HTTP Server/1.1";
	}

	// Deprecated
	public Servlet getServlet(String name) {
		return null;
	}

	public String getServletContextName() {
		return (String) attributes.get("display-name");
	}

	// Deprecated
	public Enumeration getServletNames() {
		return null;
	}

	// Deprecated
	public Enumeration getServlets() {
		return null;
	}

	// Deprecated
	public void log(Exception exception, String msg) {
		log(msg, (Throwable) exception);
	}

	// Not to be implemented
	public void log(String msg) {
		System.err.println(msg);
	}

	// Not to be implemented
	public void log(String message, Throwable throwable) {
		System.err.println(message);
		throwable.printStackTrace(System.err);
	}

	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public void setAttribute(String name, Object object) {
		if (object == null)
			removeAttribute(name);
		else
			attributes.put(name, object);
	}

	void setInitParam(String name, String value) {
		initParams.put(name, value);
	}
}
