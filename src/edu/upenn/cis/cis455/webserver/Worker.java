package edu.upenn.cis.cis455.webserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

/**
 * @author cis455
 *
 */
public class Worker implements Runnable {
	static Logger logger = Logger.getLogger(Worker.class);
	public static HashMap<String, Session> stringToSession;
	public static HashMap<Session, Long> sessionLastAccess;
	protected List<Socket> taskQueue = null;
	int maxSocketLimit;
	Date date = new Date();
	DateFormat dateFormat = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); // date format for
															// response.
	HashMap<String, String> threadMap = new HashMap<String, String>(); // Hashmap
																		// for
																		// storing
																		// threads.
	HashMap<String, HttpServlet> nameToServletMap;
	HashMap<String, String> urlToServletName;
	String newUrl = null;
	public TestHarness harness;
	public Context context;
	public HashMap<String, String> exactPattern;
	public HashMap<String, String> pathMapping;

	public Worker(List<Socket> taskQueue, int maxSocketLimit) throws Exception {

		this.taskQueue = taskQueue;
		this.maxSocketLimit = maxSocketLimit;
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		stringToSession = new HashMap<String, Session>();
		sessionLastAccess = new HashMap<Session, Long>();
		harness = new TestHarness();
		context = harness.context;
		nameToServletMap = harness.servlets;
		urlToServletName = harness.handler.m_urlPattern;
		exactPattern = new HashMap<String, String>();
		pathMapping = new HashMap<String, String>();

		for (String url : urlToServletName.keySet()) {
			newUrl = (!url.startsWith("/")) ? "/" + url : url;
			if (!url.contains("*"))
				exactPattern.put(newUrl, urlToServletName.get(url));
			else if (url.endsWith("/*"))
				pathMapping.put(newUrl.replace("/*", ""),
						urlToServletName.get(url));
		}

	}

	/**
	 * Method to generate response for directory for GET request.
	 * 
	 * @param files
	 * @param relativePath
	 * @param hostName
	 * @param output
	 * @param version
	 */
	public void responseIfDirectoryForGet(String[] files, String relativePath,
			String hostName, DataOutputStream output, String version) {
		String host = "<a href=\"http://" + hostName + "\">" + "http://"
				+ hostName + "</a>" + "/";
		// System.out.println(host);
		String htmlResponse = "<html><head><meta charset='utf-8'/></head><body>"
				+ "<h4>" + host + "</h4>" + "<table style='width:100%'>";
		String responseUrl = "";
		for (int i = 0; i < files.length; i++) {
			try {
				responseUrl = "<a href=\"http://" + hostName + "/"
						+ relativePath + "/" + files[i] + "\">"
						+ URLDecoder.decode(files[i], "UTF-8") + "</a>";
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			htmlResponse = htmlResponse + "<tr><td>" + responseUrl
					+ "</td></tr>";
		}
		htmlResponse = htmlResponse + "</table></body></html>";
		String initialResponse = version.toUpperCase() + " 200 OK\r\n";
		String headers = "Date: " + dateFormat.format(date)
				+ "\r\nContent-Type: text/html" + "\r\nContent-Length: "
				+ htmlResponse.getBytes().length + "\r\nServer: Http Server"
				+ "\r\nConnection: close\r\n\r\n";
		String response = initialResponse + headers;
		try {
			output.write(response.getBytes());
			output.write(htmlResponse.getBytes());
			output.close();
		} catch (IOException e) {

			errorResponseForGet(500, version, output);
			logger.error(e.getMessage());
			return;
		}

	}

	/**
	 * Method to generate response for file for GET request.
	 * 
	 * @param fileName
	 * @param file
	 * @param version
	 * @param output
	 * @param headersMap
	 */
	public void responseIfFileForGet(String fileName, File file,
			String version, DataOutputStream output,
			HashMap<String, String> headersMap) {

		// System.out.println("FileName : " + fileName);
		InputStream f = null;
		try {
			f = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
			errorResponseForGet(404, version, output);
			return;
		}

		// Determining the mime type.

		String mimeType = "text/plain";
		if (fileName.endsWith(".html") || fileName.endsWith(".htm"))
			mimeType = "text/html";
		else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
			mimeType = "image/jpeg";
		else if (fileName.endsWith(".gif"))
			mimeType = "image/gif";
		else if (fileName.endsWith(".png"))
			mimeType = "image/png";
		else if (fileName.endsWith(".txt"))
			mimeType = "text/plain";
		else {
			logger.error("");
			errorResponseForGet(415, version, output);
			try {
				f.close();
			} catch (IOException e) {
				errorResponseForGet(500, version, output);
				logger.error(e.getMessage());
			}
			return;
		}

		// handling if-modified-since
		if (version.equalsIgnoreCase("http/1.1")
				&& headersMap.get("if-modified-since") != null) {
			String filedate = new SimpleDateFormat(
					"EEE MMM dd HH:mm:ss zzz yyyy").format(new Date(file
					.lastModified()));
			Date lastModified = null;
			try {
				lastModified = new SimpleDateFormat(
						"EEE MMM dd HH:mm:ss zzz yyyy").parse(filedate);
			} catch (ParseException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			String date = headersMap.get("if-modified-since");
			SimpleDateFormat f1 = new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			SimpleDateFormat f2 = new SimpleDateFormat(
					"E, dd-MMM-yyyy HH:mm:ss zzz", Locale.US);
			SimpleDateFormat f3 = new SimpleDateFormat(
					"E MMM  d HH:mm:ss yyyy", Locale.US);
			f1.setTimeZone(TimeZone.getTimeZone("GMT"));
			f2.setTimeZone(TimeZone.getTimeZone("GMT"));
			f3.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date ifModifiedSince = null;
			if (f1.parse(date, new ParsePosition(0)) != null) {
				ifModifiedSince = f1.parse(date, new ParsePosition(0));
			} else {
				if (f2.parse(date, new ParsePosition(0)) != null) {
					ifModifiedSince = f2.parse(date, new ParsePosition(0));
				} else if (f3.parse(date, new ParsePosition(0)) != null) {
					ifModifiedSince = f3.parse(date, new ParsePosition(0));
				} else {
					errorResponseForGet(400, version, output);
					return;
				}
			}
			String headerdate = new SimpleDateFormat(
					"EEE MMM dd HH:mm:ss zzz yyyy").format(new Date(file
					.lastModified()));
			try {
				ifModifiedSince = new SimpleDateFormat(
						"EEE MMM dd HH:mm:ss zzz yyyy").parse(headerdate);
			} catch (ParseException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			if (lastModified.before(ifModifiedSince)) {
				errorResponseForHead(304, version, output);
				return;
			}
		}

		// handling if-unmodified-since
		if (version.equalsIgnoreCase("http/1.1")
				&& headersMap.get("if-unmodified-since") != null) {

			String filedate = new SimpleDateFormat(
					"EEE MMM dd HH:mm:ss zzz yyyy").format(new Date(file
					.lastModified()));
			Date lastModified = null;
			try {
				lastModified = new SimpleDateFormat(
						"EEE MMM dd HH:mm:ss zzz yyyy").parse(filedate);
			} catch (ParseException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
			String date = headersMap.get("if-unmodified-since");
			SimpleDateFormat f1 = new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			SimpleDateFormat f2 = new SimpleDateFormat(
					"E, dd-MMM-yyyy HH:mm:ss zzz", Locale.US);
			SimpleDateFormat f3 = new SimpleDateFormat(
					"E MMM  d HH:mm:ss yyyy", Locale.US);
			f1.setTimeZone(TimeZone.getTimeZone("GMT"));
			f2.setTimeZone(TimeZone.getTimeZone("GMT"));
			f3.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date ifUnmodifiedSince = null;
			if (f1.parse(date, new ParsePosition(0)) != null) {
				ifUnmodifiedSince = f1.parse(date, new ParsePosition(0));
			} else {
				if (f2.parse(date, new ParsePosition(0)) != null) {
					ifUnmodifiedSince = f2.parse(date, new ParsePosition(0));
				} else if (f3.parse(date, new ParsePosition(0)) != null) {
					ifUnmodifiedSince = f3.parse(date, new ParsePosition(0));
				} else {
					errorResponseForGet(400, version, output);
					return;
				}
			}

			if (lastModified.after(ifUnmodifiedSince)) {
				errorResponseForHead(412, version, output);
				return;
			}
		}

		String response = version.toUpperCase() + " 200 OK\r\n";
		response = response + "Date: " + dateFormat.format(date)
				+ "\r\nContent-Type: " + mimeType + "\r\nContent-Length: "
				+ file.length() + "\r\nServer: Http Server"
				+ "\r\nConnection: close\r\n\r\n";
		try {
			output.write((response).getBytes());
			byte[] a = new byte[f.available()];
			int n;
			while ((n = f.read(a)) > 0) {
				output.write(a, 0, n);
			}

			output.close();
			f.close();
		} catch (IOException e) {
			errorResponseForGet(500, version, output);
			logger.error(e.getMessage());
			return;
		}
		// System.out.println(response);
	}
	
	/**
	 * Generating error log page
	 * @param output
	 */
	public void errorLog(DataOutputStream output) {
		String fileName = "./conf/errorlog.log";
		File file = new File(fileName);
		InputStream f = null;
		try {
			f = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
			errorResponseForGet(404, "HTTP/1.0", output);
			return;
		}

		try {
			output.write("<h3>Error Log</h3>\r\n".getBytes());
			byte[] a = new byte[f.available()];

			int n;
			while ((n = f.read(a)) > 0) {
				output.write(a, 0, n);
			}

			f.close();
		} catch (IOException e) {
			errorResponseForGet(500, "HTTP/1.0", output);
			logger.error(e.getMessage());
			return;
		}
	}
		

	/**
	 * Generating error response for GET
	 * 
	 * @param error
	 * @param version
	 * @param output
	 */
	public void errorResponseForGet(int error, String version,
			DataOutputStream output) {
		String initialResponse = "";
		String headers = "";
		String body = "";
		try {

			int contentLength = 0;
			switch (error) {

			case 400:
				initialResponse = version.toUpperCase()
						+ " 400 Bad Request\r\n";
				body = "<html><body>\n<h3>400: Bad Request</h3>\n</body></html>";
				break;

			case 403:
				initialResponse = version.toUpperCase() + " 403 Forbidden\r\n";
				body = "<html><body>\n<h3>403: Forbidden Access</h3>\n</body></html>";
				break;

			case 404:
				initialResponse = version.toUpperCase() + " 404 Not Found\r\n";
				body = "<html><body>\n<h3>404: File Not Found</h3>\n</body></html>";
				break;

			case 415:
				initialResponse = version.toUpperCase()
						+ " 415 Unsupported Media Type\r\n";
				body = "<html><body>\n<h3>415: Unsupported Media Type</h3>\n</body></html>";
				break;

			case 500:
				initialResponse = version.toUpperCase()
						+ " 500 Internal Server Error\r\n";
				body = "<html><body>\n<h3>500: Internal Server Error</h3>\n</body></html>";
				break;

			case 501:
				initialResponse = version.toUpperCase()
						+ " 501 Not Implemented\r\n";
				body = "<html><body>\n<h3>501: Not Implemented</h3>\n</body></html>";
				break;

			case 505:
				initialResponse = version.toUpperCase()
						+ " 505 HTTP Version Not Supported\r\n";
				body = "<html><body>\n<h3>505: HTTP Version Not Supported</h3>\n</body></html>\n\n";
				break;
			}
			contentLength = body.getBytes().length;
			headers = "Date: " + dateFormat.format(date)
					+ "\r\nContent-Type: text/html" + "\r\nContent-Length: "
					+ contentLength + "\r\nServer: Http Server"
					+ "\r\nConnection: close\r\n\r\n";

			output.write(initialResponse.getBytes());
			output.write(headers.getBytes());
			output.write(body.getBytes());
			output.close();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	/**
	 * Method to generate response for directory for HEAD request.
	 * 
	 * @param files
	 * @param relativePath
	 * @param hostName
	 * @param output
	 * @param version
	 */
	public void responseIfDirectoryForHead(String[] files, String relativePath,
			String hostName, DataOutputStream output, String version) {
		String host = "<a href=\"http://" + hostName + "\">" + "http://"
				+ hostName + "</a>" + "/";
		// System.out.println(host);
		String htmlResponse = "<html><head><meta charset='utf-8'/></head><body>"
				+ "<h4>" + host + "</h4>" + "<table style='width:100%'>";
		String responseUrl = "";
		for (int i = 0; i < files.length; i++) {
			responseUrl = "<a href=\"http://" + hostName + "/" + relativePath
					+ "/" + files[i] + "\">" + files[i] + "</a>";
			htmlResponse = htmlResponse + "<tr><td>" + responseUrl
					+ "</td></tr>";
		}
		htmlResponse = htmlResponse + "</table></body></html>";
		String initialResponse = version.toUpperCase() + " 200 OK\r\n";
		String headers = "Date: " + dateFormat.format(date)
				+ "\r\nContent-Type: text/html" + "\r\nContent-Length: "
				+ htmlResponse.getBytes().length + "\r\nServer: Http Server"
				+ "\r\nConnection: close\r\n\r\n";
		String response = initialResponse + headers;
		try {
			output.write(response.getBytes());
			output.close();
		} catch (IOException e) {
			errorResponseForGet(500, version, output);
			logger.error(e.getMessage());
			return;
		}

	}

	/**
	 * Method to generate response for file for HEAD request.
	 * 
	 * @param fileName
	 * @param file
	 * @param version
	 * @param output
	 * @param headersMap
	 */
	public void responseIfFileForHead(String fileName, File file,
			String version, DataOutputStream output,
			HashMap<String, String> headersMap) {

		// System.out.println("FileName : " + fileName);
		InputStream f = null;
		try {
			f = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			errorResponseForHead(404, version, output);
			logger.error(e.getMessage());
			return;
		}

		// Determining the mime types

		String mimeType = "text/plain";
		if (fileName.endsWith(".html") || fileName.endsWith(".htm"))
			mimeType = "text/html";
		else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
			mimeType = "image/jpeg";
		else if (fileName.endsWith(".gif"))
			mimeType = "image/gif";
		else if (fileName.endsWith(".png"))
			mimeType = "image/png";
		else if (fileName.endsWith(".txt"))
			mimeType = "text/plain";
		else {
			logger.error("415 Unsupported Media Type");
			errorResponseForHead(415, version, output);
			try {
				f.close();
			} catch (IOException e) {

				errorResponseForGet(500, version, output);
				logger.error(e.getMessage());
			}
			return;
		}

		if (version.equalsIgnoreCase("http/1.1")
				&& headersMap.get("if-unmodified-since") != null) {
			Date lastModified = new Date(file.lastModified());
			String date = headersMap.get("if-unmodified-since");
			SimpleDateFormat f1 = new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			SimpleDateFormat f2 = new SimpleDateFormat(
					"E, dd-MMM-yyyy HH:mm:ss zzz", Locale.US);
			SimpleDateFormat f3 = new SimpleDateFormat(
					"E MMM  d HH:mm:ss yyyy", Locale.US);
			f1.setTimeZone(TimeZone.getTimeZone("GMT"));
			f2.setTimeZone(TimeZone.getTimeZone("GMT"));
			f3.setTimeZone(TimeZone.getTimeZone("GMT"));

			Date ifUnmodifiedSince = null;
			if (f1.parse(date, new ParsePosition(0)) != null) {
				ifUnmodifiedSince = f1.parse(date, new ParsePosition(0));
			} else {
				if (f2.parse(date, new ParsePosition(0)) != null) {
					ifUnmodifiedSince = f2.parse(date, new ParsePosition(0));
				} else if (f3.parse(date, new ParsePosition(0)) != null) {
					ifUnmodifiedSince = f3.parse(date, new ParsePosition(0));
				} else {
					logger.error("400 Bad Request");
					errorResponseForHead(400, version, output);
					return;
				}
			}

			if (lastModified.after(ifUnmodifiedSince)) {
				logger.error("412 version not supported");
				errorResponseForHead(412, version, output);
				return;
			}
		}

		String response = version.toUpperCase() + " 200 OK\r\n";
		response = response + "Date: " + dateFormat.format(date)
				+ "\r\nContent-Type: " + mimeType + "\r\nContent-Length: "
				+ file.length() + "\r\nServer: Http Server"
				+ "\r\nConnection: close\r\n\r\n";
		try {
			output.write((response).getBytes());
			output.close();
			f.close();
		} catch (IOException e) {
			errorResponseForGet(500, version, output);
			logger.error(e.getMessage());
			return;
		}
		// System.out.println(response);

	}

	/**
	 * Method to generate error response for HEAD request.
	 * 
	 * @param error
	 * @param version
	 * @param output
	 */
	public void errorResponseForHead(int error, String version,
			DataOutputStream output) {
		String initialResponse = "";
		String headers = "";
		String body = "";
		try {

			int contentLength = 0;
			switch (error) {

			case 304:
				initialResponse = version.toUpperCase()
						+ " 304 Not Modified\r\n";
				body = "<html><body>\n<h3>304: Not Modified</h3>\n</body></html>";
				break;

			case 400:
				initialResponse = version.toUpperCase()
						+ " 400 Bad Request\r\n";
				body = "<html><body>\n<h3>400: Bad Request</h3>\n</body></html>";
				break;

			case 403:
				initialResponse = version.toUpperCase() + " 403 Forbidden\r\n";
				body = "<html><body>\n<h3>403: Forbidden Access</h3>\n</body></html>";
				break;

			case 412:
				initialResponse = version.toUpperCase()
						+ " 412 Precondition Failed\r\n";
				body = "<html><body>\n<h3>412: Precondition Failed</h3>\n</body></html>";
				break;

			case 404:
				initialResponse = version.toUpperCase() + " 404 Not Found\r\n";
				body = "<html><body>\n<h3>404: File Not Found</h3>\n</body></html>";
				break;

			case 415:
				initialResponse = version.toUpperCase()
						+ " 415 Unsupported Media Type\r\n";
				body = "<html><body>\n<h3>415: Unsupported Media Type</h3>\n</body></html>";
				break;

			case 500:
				initialResponse = version.toUpperCase()
						+ " 500 Internal Server Error\r\n";
				body = "<html><body>\n<h3>500: Internal Server Error</h3>\n</body></html>";
				break;

			case 501:
				initialResponse = version.toUpperCase()
						+ " 501 Not Implemented\r\n";
				body = "<html><body>\n<h3>501: Not Implemented</h3>\n</body></html>";
				break;

			case 505:
				initialResponse = version.toUpperCase()
						+ " 505 HTTP Version Not Supported\r\n";
				body = "<html><body>\n<h3>505: HTTP Version Not Supported</h3>\n</body></html>\n\n";
				break;
			}
			contentLength = body.getBytes().length;
			headers = "Date: " + dateFormat.format(date)
					+ "\r\nContent-Type: text/html" + "\r\nContent-Length: "
					+ contentLength + "\r\nServer: Http Server"
					+ "\r\nConnection: close\r\n\r\n";

			output.write(initialResponse.getBytes());
			output.write(headers.getBytes());
			output.close();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	/**
	 * Method to generate response for Shutdown for GET
	 * 
	 * @param clientSocket
	 */
	public void responseForShutdownGet(Socket clientSocket) {
		try {
			String initialResponse = "HTTP/1.0 200 OK\r\n";
			String body = "<html><body>\n<h3>Server is going to Shutdown.</h3>\n</body></html>";
			String headers = "Date: " + dateFormat.format(date)
					+ "\r\nContent-Type: " + "text/html"
					+ "\r\nContent-Length: " + body.getBytes().length
					+ "\r\nServer: Http Server"
					+ "\r\nConnection: close\r\n\r\n";
			DataOutputStream output = new DataOutputStream(
					clientSocket.getOutputStream());
			HttpServer.isStopped = true;
			ThreadPooledServer.serverSocket.close();

			output.write(initialResponse.getBytes());
			output.write(headers.getBytes());
			output.write(body.getBytes());
		} catch (IOException e) {
			logger.error(e.getMessage());
			return;
		}

	}

	/**
	 * Method to generate response for Shutdown for HEAD
	 * 
	 * @param clientSocket
	 */
	public void responseForShutdownHead(Socket clientSocket) {
		try {
			String initialResponse = "HTTP/1.0 200 OK\r\n";
			String body = "<html><body>\n<h3>Server is going to Shutdown.</h3>\n</body></html>";
			String headers = "Date: " + dateFormat.format(date)
					+ "\r\nContent-Type: " + "text/html"
					+ "\r\nContent-Length: " + body.getBytes().length
					+ "\r\nServer: Http Server"
					+ "\r\nConnection: close\r\n\r\n";
			DataOutputStream output = new DataOutputStream(
					clientSocket.getOutputStream());
			HttpServer.isStopped = true;
			ThreadPooledServer.serverSocket.close();
			output.write(initialResponse.getBytes());
			output.write(headers.getBytes());

		} catch (IOException e) {
			logger.error(e.getMessage());
			return;
		}

	}

	/**
	 * Method to generate response for control for GET
	 * 
	 * @param clientSocket
	 * @param hostName
	 */
	public void responseForControlGet(Socket clientSocket, String hostName) {

		String body = "<html><head> Control Panel </head>";
		body = body
				+ "<body><h3>Full Name: Alifia Haidry, Seas ID: ahaidry</h3>";
		body = body + "<table>";
		for (Thread thread : ThreadPooledServer.threadPool) {
			body += "<tr><td>" + thread.getName() + "</td>";
			if (thread.getState() == Thread.State.RUNNABLE) {
				body = body + "<td>" + threadMap.get(thread.getName())
						+ "</td></tr>";
			} else {
				body = body + "<td>" + thread.getState() + "</td></tr>";
			}

		}
		body = body
				+ "<tr><td><button type=\"button\" onclick=\"location.href='http://"
				+ hostName + "/shutdown'\">Shut Down</button>";
		body = body + "</tr></td></table></body></html>";
		String initialResponse = "HTTP/1.0 200 OK\r\n";
		String headers = "Date: " + dateFormat.format(date)
				+ "\r\nContent-Type: " + "text/html" + "\r\nContent-Length: "
				+ body.getBytes().length + "\r\nServer: Http Server"
				+ "\r\nConnection: close\r\n\r\n";
		try {
			DataOutputStream output = new DataOutputStream(
					clientSocket.getOutputStream());
			output.write(initialResponse.getBytes());
			output.write(headers.getBytes());
			output.write(body.getBytes());
			errorLog(output);
			output.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Method to generate response for control for HEAD
	 * 
	 * @param clientSocket
	 * @param hostName
	 */
	public void responseForControlHead(Socket clientSocket, String hostName) {

		String body = "<html><head> Control Panel </head>";
		body = body
				+ "<body><h3>Full Name: Alifia Haidry, Seas ID: ahaidry</h3>";
		body = body + "<table>";
		for (Thread thread : ThreadPooledServer.threadPool) {
			body += "<tr><td>" + thread.getName() + "</td>";
			if (thread.getState() == Thread.State.RUNNABLE) {
				body = body + "<td>" + threadMap.get(thread.getName())
						+ "</td></tr>";
			} else {
				body = body + "<td>" + thread.getState() + "</td></tr>";
			}

		}
		body = body
				+ "<tr><td><button type=\"button\" onclick=\"location.href='http://"
				+ hostName + "/shutdown'\">Shut Down</button>";
		body = body + "</tr></td></table></body></html>";
		String initialResponse = "HTTP/1.0 200 OK\r\n";
		String headers = "Date: " + dateFormat.format(date)
				+ "\r\nContent-Type: " + "text/html" + "\r\nContent-Length: "
				+ body.getBytes().length + "\r\nServer: Http Server"
				+ "\r\nConnection: close\r\n\r\n";
		try {
			DataOutputStream output = new DataOutputStream(
					clientSocket.getOutputStream());
			output.write(initialResponse.getBytes());
			output.write(headers.getBytes());
			errorLog(output);
			output.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Method for returning servlet entry.
	 * 
	 * @param hashMap
	 */
	public Entry<String, String> servletEntry(HashMap<String, String> hashMap) {

		String path = hashMap.get("path");
		String servlet = null;
		int max = 0;
		int len = 0;
		String new_url = null;
		String str = path.split("\\?")[0];
		for (String url : exactPattern.keySet()) {
			if (url.equals(str)) {
				Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(
						url, exactPattern.get(url));
				return entry;
			}
		}
		for (String url : pathMapping.keySet()) {
			if (str.contains(url)) {
				len = url.split("/").length;
				if (len > max) {
					servlet = pathMapping.get(url);
					new_url = url;
					max = len;
				}
				len = 0;
			}
		}
		Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(
				new_url, servlet);
		return entry;
	}

	/**
	 * Method to generate error response for servlet.
	 * 
	 * @param socket
	 * @param entry
	 * @param headersMap
	 * @param bufferedReader
	 */
	public void responseForServlet(Socket clientSocket,
			HashMap<String, String> headersMap, Entry<String, String> urlEntry,
			BufferedReader in) {
		try {
			Session session = null;
			String body = "";
			Response response = new Response(clientSocket, urlEntry,headersMap);
			Request request = new Request(session, headersMap, clientSocket,
					urlEntry, context, response);
			String servlet = urlEntry.getValue();
			// System.out.println("servlet  " + servlet);
			HttpServlet servlet_object = nameToServletMap.get(servlet);
			String path = headersMap.get("path");
			// System.out.println("path " + path);
			String[] strings = path.split("\\?|&|=");
			DataOutputStream output = new DataOutputStream(
					clientSocket.getOutputStream());
			// System.out.println("Strings array length " + strings.length);
			// System.out.println("method");
			// System.out.println(headersMap.get("method"));

			for (int j = 1; j < strings.length - 1; j += 2) {
				request.setParameter(strings[j], strings[j + 1]);
				// System.out.println(strings[j]);
				// System.out.println(strings[j + 1]);
			}

			if (headersMap.get("method").toUpperCase().compareTo("GET") == 0
					|| headersMap.get("method").toUpperCase().compareTo("POST") == 0) {
				request.setMethod(headersMap.get("method").toUpperCase());
				// int j = 0;

				if (headersMap.get("method").equalsIgnoreCase("POST")) {
					String postQuery = "";
					// if(headersMap.containsKey("content-length") &&
					// headersMap.containsKey("content-type")){
					// if(headersMap.get("content-type").equalsIgnoreCase("application/x-www-form-urlencoded")){
					int contentLength = Integer.parseInt(headersMap
							.get("content-length"));
					// BufferedInputStream bufferedStream = new
					// BufferedInputStream();
					// char buff;
					char[] buffer = new char[contentLength];
					if (in.read(buffer) > 0) {
						postQuery = new String(buffer);
					}

					body = postQuery;
					// System.out.println("body   " + body);
					String[] parts = postQuery.split("=|&");
					for (int j = 0; j < parts.length - 1; j += 2) {
						request.setParameter(parts[j], parts[j + 1]);
						// System.out.println("key    " + parts[j]);
						// System.out.println("value    " + parts[j + 1]);
					}
					request.setBody(body);
				}

				//
				// }
				servlet_object.service(request, response);
				response.flushBuffer();
			} else {
				errorResponseForGet(501, "Http/1.0", output);
			}

		}

		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	/*
	 * java.lang.Runnable#run()
	 */
	public void run() {

		while (!HttpServer.isStopped) { // condition for shutdown
			HashMap<String, String> headersMap = new HashMap<String, String>(); // hashmap
																				// for
																				// storing
																				// headers
			DataOutputStream output = null;
			Socket clientSocket = null;
			try {

				synchronized (taskQueue) {
					while (taskQueue.size() == 0) {
						if (HttpServer.isStopped) {
							break;
						}
						taskQueue.wait(); // making threads sleep if no request
						if (HttpServer.isStopped) {
							break;
						}
					}

					if (taskQueue.size() == maxSocketLimit) {
						taskQueue.notifyAll();
					}
					if (HttpServer.isStopped) {
						break;
					}
					clientSocket = taskQueue.remove(0); // dequeing resquest to
														// handle
				}

				BufferedReader input = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				output = new DataOutputStream(new BufferedOutputStream(
						clientSocket.getOutputStream()));
				String hostName = clientSocket.getInetAddress().getHostName()
						+ ":" + HttpServer.portNumber;
				String initialRequestLine = input.readLine();
				String header = "";
				String fileName = "";
				String relativePath = "";
				String version = "";
				StringTokenizer tokenizer = new StringTokenizer(
						initialRequestLine);
				String method = "";

				if (tokenizer.hasMoreElements()) {
					method = tokenizer.nextToken();
					method = method.toLowerCase();
					headersMap.put("method", method);
				}

				// generating error if bad format.
				else {
					errorResponseForGet(400, "HTTP/1.0", output);
					clientSocket.close();
					input.close();
					continue;
				}

				if (tokenizer.hasMoreElements()) {

					relativePath = tokenizer.nextToken();
					URL url = null;
					if (relativePath.toLowerCase().startsWith("http://")) {
						url = new URL(relativePath);
						relativePath = url.getPath();
						fileName = HttpServer.rootDirectory + relativePath;
						headersMap.put("path", url.toString());

					} else {
						fileName = HttpServer.rootDirectory + relativePath;
						headersMap.put("path", relativePath);
					}
				}

				// generating error if file missing
				else {
					errorResponseForGet(400, "HTTP/1.0", output);
					clientSocket.close();
					input.close();
					continue;
				}

				if (tokenizer.hasMoreElements()) {
					version = tokenizer.nextToken();
					version = version.toLowerCase();
					headersMap.put("version", version);
				}

				// generating error if version missing.
				else {
					errorResponseForGet(400, "HTTP/1.0", output);
					clientSocket.close();
					input.close();
					continue;
				}

				// generating error if method is valid but not supported
				if (method.equalsIgnoreCase("PUT")
						|| method.equalsIgnoreCase("DELETE")
						|| method.equalsIgnoreCase("OPTIONS")
						|| method.equalsIgnoreCase("TRACE")) { // Not
																// implemented
					errorResponseForGet(501, version, output);
					clientSocket.close();
					input.close();
					continue;
				}

				// generating error if method is not valid
				if (!method.equalsIgnoreCase("GET")
						&& !method.equalsIgnoreCase("HEAD")
						&& !method.equalsIgnoreCase("POST")) {
					errorResponseForGet(400, version, output);
					clientSocket.close();
					input.close();
					continue;
				}

				// generating 505 error if version is not supported and 400 if
				// it is invalid for GET
				if (!version.equalsIgnoreCase("HTTP/1.1")
						&& !version.equalsIgnoreCase("HTTP/1.0")
						&& method.equalsIgnoreCase("GET")) {
					if (version.startsWith("http")) {
						errorResponseForGet(505, version, output);
						clientSocket.close();
						input.close();
						continue;
					} else {
						errorResponseForGet(400, "HTTP/1.0", output);
						clientSocket.close();
						input.close();
						continue;
					}

				}

				// generating 505 error if version is not supported and 400 if
				// it is invalid for HEAD
				if (!version.equalsIgnoreCase("HTTP/1.1")
						&& !version.equalsIgnoreCase("HTTP/1.0")
						&& method.equalsIgnoreCase("HEAD")) {
					if (version.startsWith("http")) {
						errorResponseForGet(505, version, output);
						clientSocket.close();
						input.close();
						continue;
					} else {
						errorResponseForGet(400, "HTTP/1.0", output);
						clientSocket.close();
						input.close();
						continue;
					}
				}

				// reading all headers and putting them into hashmap
				while (true) {
					// System.out.println("current header:  " + header);
					header = input.readLine();
					if (header.length() == 0) {
						// System.out.println("header is blank-line");
						break;
					}
					int index = header.indexOf(':');
					headersMap.put(header.substring(0, index).trim()
							.toLowerCase(), header.substring(index + 1).trim());
				}

				// generating bad request if host missing.
				if (version.equalsIgnoreCase("HTTP/1.1")
						&& headersMap.get("host") == null) {
					if (method.equalsIgnoreCase("get")) {
						errorResponseForGet(400, version, output);
					} else {
						errorResponseForHead(400, version, output);
					}
					clientSocket.close();
					input.close();
					continue;
				}

				// supporting request with only host name and with both host
				// name and port number
				else if (version.equalsIgnoreCase("HTTP/1.1")
						&& headersMap.get("host") != null) {
					if (headersMap.get("host").contains(":")) {
						hostName = headersMap.get("host");
					} else {
						hostName = headersMap.get("host") + ":"
								+ HttpServer.portNumber;
					}
					headersMap.put("host", hostName);
				}

				// putting threads into map
				threadMap.put(Thread.currentThread().getName(), hostName
						+ relativePath);

				// executing shutdown request
				if (method.equalsIgnoreCase("get")
						&& relativePath.equalsIgnoreCase("/shutdown")) {
					responseForShutdownGet(clientSocket);
					continue;
				}

				if (method.equalsIgnoreCase("head")
						&& relativePath.equalsIgnoreCase("/shutdown")) {
					responseForShutdownHead(clientSocket);
					continue;
				}

				// executing control request
				if (method.equalsIgnoreCase("get")
						&& relativePath.equalsIgnoreCase("/control")) {
					responseForControlGet(clientSocket, hostName);
					continue;
				}

				if (method.equalsIgnoreCase("head")
						&& relativePath.equalsIgnoreCase("/control")) {
					responseForControlHead(clientSocket, hostName);
					continue;
				}

				Entry<String, String> url_servlet = servletEntry(headersMap);
				String servlet = url_servlet.getValue();
				// System.out.println("servlet value: " + servlet);
				if (servlet != null) {
					responseForServlet(clientSocket, headersMap, url_servlet,
							input);
					clientSocket.close();
					input.close();
					continue;
				}

				else {
					if (method.equalsIgnoreCase("POST")) {
						errorResponseForGet(501, version, output);
						clientSocket.close();
						input.close();
						continue;
					}
					File file = new File(fileName);
					file = file.getCanonicalFile();
					fileName = file.getCanonicalPath();

					// checking if file is in root directory, generating 403
					if (!fileName.startsWith(new File(HttpServer.rootDirectory)
							.getCanonicalPath())) {
						logger.error("Forbidden Access");
						errorResponseForGet(403, version, output);
						clientSocket.close();
						input.close();
						continue;
					}

					// checking if file exists, generating 404
					if (!file.exists()) {
						logger.error("File Not Found Error");
						errorResponseForGet(404, version, output);
						clientSocket.close();
						input.close();
						continue;
					}

					// checking if file has read permission, generating 403
					if (!file.canRead()) {
						errorResponseForGet(403, version, output);
						clientSocket.close();
						input.close();
						continue;
					}

					// handling 100 continue
					if (version.equalsIgnoreCase("http/1.1")
							&& headersMap.get("expect") != null) {
						output.write("HTTP/1.1 100 Continue\r\n\n".getBytes());
					}

					// generating response for directory
					if (file.isDirectory() && method.equalsIgnoreCase("GET")) {

						responseIfDirectoryForGet(file.list(), relativePath,
								hostName, output, version);
					} else if (file.isDirectory()
							&& method.equalsIgnoreCase("HEAD")) {
						responseIfDirectoryForHead(file.list(), relativePath,
								hostName, output, version);
					}

					// generating response for file
					else if (method.equalsIgnoreCase("GET")) {
						responseIfFileForGet(fileName, file, version, output,
								headersMap);
					} else {
						responseIfFileForHead(fileName, file, version, output,
								headersMap);
					}

					// closing input connection
					input.close();
				}
			}

			catch (IOException e) {
				errorResponseForGet(500, "HTTP/1.0", output);
				logger.error(e.getMessage());
				continue;

			} catch (InterruptedException e) {
				errorResponseForGet(500, "HTTP/1.0", output);
				logger.error(e.getMessage());
				continue;
			}

		}

	}
}
