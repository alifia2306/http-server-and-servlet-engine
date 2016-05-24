package edu.upenn.cis.cis455.webserver;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.log4j.Logger;

public class Config implements ServletConfig {

	static Logger logger = Logger.getLogger(Config.class);
	private String name;
	private Context context;
	private HashMap<String, String> initParams;

	public Config(String name, Context context) {
		this.name = name;
		this.context = context;
		initParams = new HashMap<String, String>();
	}

	public String getInitParameter(String name) {
		return initParams.get(name);
	}

	public Enumeration getInitParameterNames() {
		Set<String> keys = initParams.keySet();
		Vector<String> atts = new Vector<String>(keys);
		return atts.elements();
	}

	public ServletContext getServletContext() {
		return context;
	}

	public String getServletName() {
		return name;
	}

	void setInitParam(String name, String value) {
		initParams.put(name, value);
	}

}
