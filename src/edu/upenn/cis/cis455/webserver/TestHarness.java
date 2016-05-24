package edu.upenn.cis.cis455.webserver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Todd J. Green, modified by Nick Taylor
 */
public class TestHarness {
	Handler handler;
	static final Logger logger = Logger.getLogger(TestHarness.class);
	static HashMap<String, HttpServlet> servlets = new HashMap<String, HttpServlet>();
	HashMap<String, String> contextParams = new HashMap<String, String>();
	HashMap<String, String> servletMapping = new HashMap<String, String>();
	HashMap<String, HashMap<String, String>> servletParams = new HashMap<String, HashMap<String, String>>();
	static Context context;

	TestHarness() throws Exception {
		handler = parseWebdotxml(HttpServer.webXml);
		context = createContext(handler);
		servlets = createServlets(handler, context);
	}

	static class Handler extends DefaultHandler {
		private int m_state = 0;
		private String m_servletName;
		private String m_paramName;
		private String m_displayName;
		HashMap<String, String> m_servlets = new HashMap<String, String>();
		HashMap<String, String> m_contextParams = new HashMap<String, String>();
		HashMap<String, HashMap<String, String>> m_servletParams = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> m_urlPattern = new HashMap<String, String>();
		
		/**
		 * Method for reading web.xml.
		 * 
		 * @param uri
		 * @param localName
		 * @param qName
		 * @param attributes
		 */
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) {
			System.out.println("started server");
			if (qName.compareTo("servlet-name") == 0) {
				m_state = 1;
			} else if (qName.compareTo("servlet-class") == 0) {
				m_state = 2;
			} else if (qName.compareTo("context-param") == 0) {
				m_state = 3;
			} else if (qName.compareTo("init-param") == 0) {
				m_state = 4;
			} else if (qName.compareTo("servlet-mapping") == 0) {
				m_state = 5;
			} else if (qName.compareTo("url-pattern") == 0) {
				m_state = 6;
			}

			else if (qName.compareTo("param-name") == 0) {
				m_state = (m_state == 3) ? 10 : 20;
			} else if (qName.compareTo("param-value") == 0) {
				m_state = (m_state == 10) ? 11 : 21;
			}
		}

		/**
		 * Method for populating maps
		 * 
		 * @param ch
		 * @param start
		 * @param length
		 */
		public void characters(char[] ch, int start, int length) {
			String value = new String(ch, start, length);
			if (m_state == 1) {
				m_servletName = value;
				m_state = 0;
			} else if (m_state == 2) {
				m_servlets.put(m_servletName, value);
				m_state = 0;
			} else if (m_state == 6) {
				if (m_servletName == null) {
					System.err.println("URL pattern value '" + value
							+ "' without servlet name");
					System.exit(-1);
				}
				m_urlPattern.put(value, m_servletName);
				m_state = 0;
			}

			else if (m_state == 10 || m_state == 20) {
				m_paramName = value;
			} else if (m_state == 11) {
				if (m_paramName == null) {
					System.err.println("Context parameter value '" + value
							+ "' without name");
					System.exit(-1);
				}
				m_contextParams.put(m_paramName, value);
				m_paramName = null;
				m_state = 0;
			} else if (m_state == 21) {
				if (m_paramName == null) {
					System.err.println("Servlet parameter value '" + value
							+ "' without name");
					System.exit(-1);
				}
				HashMap<String, String> p = m_servletParams.get(m_servletName);
				if (p == null) {
					p = new HashMap<String, String>();
					m_servletParams.put(m_servletName, p);
				}
				p.put(m_paramName, value);
				m_paramName = null;
				m_state = 0;
			}
		}

	}
	
	/**
	 * Method for parsing web.xml.
	 * 
	 * @param webdotxml
	 */
	private static Handler parseWebdotxml(String webdotxml) throws Exception {
		Handler h = new Handler();
		File file = new File(webdotxml);
		if (file.exists() == false) {
			logger.error("error: cannot find " + file.getPath());
			System.err.println("error: cannot find " + file.getPath());
			System.exit(-1);
		}
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		parser.parse(file, h);
		return h;
	}
	
	/**
	 * Method to create context
	 * 
	 * @param handler
	 */
	private static Context createContext(Handler h) {

		Context fc = new Context();
		for (String param : h.m_contextParams.keySet()) {
			fc.setInitParam(param, h.m_contextParams.get(param));
		}
		return fc;
	}

	/**
	 * Method to create servlets
	 * 
	 * @param handler
	 */
	private static HashMap<String, HttpServlet> createServlets(Handler h,
			Context context2) throws Exception {
		HashMap<String, HttpServlet> servlets = new HashMap<String, HttpServlet>();
		for (String servletName : h.m_servlets.keySet()) {
			Config config = new Config(servletName, context2);
			String className = h.m_servlets.get(servletName);

			Class servletClass = Class.forName(className);
			HttpServlet servlet = (HttpServlet) servletClass.newInstance();
			HashMap<String, String> servletParams = h.m_servletParams
					.get(servletName);
			if (servletParams != null) {
				for (String param : servletParams.keySet()) {
					config.setInitParam(param, servletParams.get(param));
				}
			}
			servlet.init(config);
			servlets.put(servletName, servlet);
		}
		return servlets;
	}
	

}
