package edu.upenn.cis.cis455.webserver;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class Response implements HttpServletResponse {
	static final Logger logger = Logger.getLogger(Response.class);
	public Socket clientSocket;
	public HashMap<String, String> headers = new HashMap<String, String>();
	public HashMap<String, String> statusMap = new HashMap<String, String>();
	public Entry<Integer, String> error = new AbstractMap.SimpleEntry<Integer, String>(
			0, "");
	HashMap<String, String> map = new HashMap<>();
	Date date = new Date();
	DateFormat dateFormat = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); // date format for
															// response.
	StringBuffer buffer;
	int bufferSize;
	public PrintWriter printWriter;
	boolean isCommitted;
	BufferedWriter bufferedWriter;
	String url;
	String servlet;
	Locale locale;
	Writer writer;
	Response(){
		
	}
	Response(Socket client, Entry<String, String> entry, HashMap<String, String> map) {
		url = entry.getKey();
		servlet = entry.getValue();
		clientSocket = client;
		this.map = map;
		statusMap.put("100", "Continue");
		statusMap.put("200", "OK");
		statusMap.put("204", "No Content");
		statusMap.put("301", "Moved Permanently");
		statusMap.put("302", "Found");
		statusMap.put("304", "Not Modified");
		statusMap.put("400", "Bad Request");
		statusMap.put("401", "Unauthorized");
		statusMap.put("403", "Forbidden");
		statusMap.put("404", "Not Found");
		statusMap.put("408", "Request Timeout");
		statusMap.put("412", "Precondition Failed");
		statusMap.put("500", "Internal Server Error");
		statusMap.put("501", "Not Implemented");
		statusMap.put("505", "HTTP Version Not Supported");
		locale = null;
		isCommitted = false;
		bufferSize = 1000;
		buffer = new StringBuffer(bufferSize);
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(
					clientSocket.getOutputStream()));
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		writer = new Writer(bufferedWriter);
	}

	public class Writer extends PrintWriter {

		Writer(BufferedWriter bufferedWriter) {
			super(bufferedWriter);
			buffer = new StringBuffer(bufferSize);
		}

		public void write(String data) {

			try {
				flushBuffer();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			buffer.append(data);
		}

		public void println(String data) {

			try {
				flushBuffer();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			buffer.append(data);
		}
	}

	public void addCookie(Cookie arg0) {
		if (!headers.containsKey("Set-Cookie")) {
			headers.put("Set-Cookie", arg0.getName() + "=" + arg0.getValue()
					+ "; " + "Expires" + "=" + cookieExpirationDate(arg0));
			return;
		}

		String c = (String) headers.get("Set-Cookie");
		// System.out.println(c);
		headers.put("Set-Cookie", c + "\r\nSet-Cookie:" + arg0.getName() + "="
				+ arg0.getValue() + "; " + "Expires" + "="
				+ cookieExpirationDate(arg0));

	}

	public String cookieExpirationDate(Cookie arg0) {
		String date;
		SimpleDateFormat format = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		date = format.format(Calendar.getInstance().getTimeInMillis()
				+ arg0.getMaxAge());
		return date;
	}

	public boolean containsHeader(String arg0) {
		if (headers.containsKey(arg0))
			return true;
		return false;
	}

	public String encodeURL(String arg0) {
		return arg0;
	}

	public String encodeRedirectURL(String arg0) {
		return arg0;
	}

	// Deprecated
	public String encodeUrl(String arg0) {
		return null;
	}

	// Deprecated
	public String encodeRedirectUrl(String arg0) {
		return null;
	}

	private void generateHeaders(String statusCode) {
		if (isCommitted()) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}

		String status;
		if (headers.containsKey("status")) {
			status = headers.get("status");
		} else
			status = statusCode;
		String initialResponseLine = map.get("version").toUpperCase() + " ";
		initialResponseLine = initialResponseLine + status + " "
				+ statusMap.get(status) + "\r\n";
		;
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		String header = "";
		header = header + "Date: " + dateFormat.format(date)
				+ "\r\n";
		for (String key : headers.keySet()) {
			if (key.equals("status"))
				continue;
			header = header + key + ": " + headers.get(key) + "\r\n";
		}
		header = header + "Server: Http Server\r\n";
		header = header + "Connection: close\r\n\r\n";
		try {
			bufferedWriter.write(initialResponseLine);
			bufferedWriter.write(header);
			bufferedWriter.flush();
			isCommitted = true;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void sendError(int arg0, String statusMessage) throws IOException {
		if (isCommitted()) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		headers.clear();
		setStatus(arg0);
		setContentType("text/html");
		resetBuffer();
		String status = arg0 + "";
		String body = "<html><head><title>" + status + ": " + statusMessage
				+ "</title></head><body>\n<h3>" + status + ": " + statusMessage
				+ "</h3>\n</body></html>";
		setContentLength(body.length());
		generateHeaders(status);
		writer.write(body);
		try {
			flushBuffer();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		isCommitted = true;
	}

	public void sendError(int arg0) throws IOException {
		if (isCommitted()) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		String statusMessage = statusMap.get(arg0 + "");
		headers.clear();
		setStatus(arg0);
		setContentType("text/html");
		resetBuffer();
		String status = arg0 + "";
		String body = "<html><head><title>" + status + ": " + statusMessage
				+ "</title></head><body>\n<h3>" + status + ": " + statusMessage
				+ "</h3>\n</body></html>";
		setContentLength(body.length());
		generateHeaders(status);
		writer.write(body);
		try {
			flushBuffer();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		isCommitted = true;
	}

	public void sendRedirect(String redirectLocation) throws IOException {
		if (isCommitted()) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}

		String hostName = clientSocket.getLocalAddress().getHostName();
		int portNumber = clientSocket.getLocalPort();
		String location = "";
		if (redirectLocation.contains("http://")) {
			location = redirectLocation;
		}

		else if (redirectLocation.startsWith("/")) {
			location = "http://" + hostName + ":" + portNumber
					+ redirectLocation;
		}

		else {
			location = url + (url.endsWith("/") ? "" : "/") + redirectLocation;
		}

		headers.put("Content-Location", location);
		generateHeaders("200");
		buffer = new StringBuffer(bufferSize);
		flushBuffer();
	}

	public void setDateHeader(String arg0, long arg1) {
		SimpleDateFormat format = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d = new Date(arg1);
		String date = format.format(d);
		headers.put(arg0, date);
	}

	public void addDateHeader(String arg0, long arg1) {
		SimpleDateFormat format = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d = new Date(arg1);
		String date = format.format(d);
		headers.put(arg0, date);
	}

	public void setHeader(String arg0, String arg1) {
		headers.put(arg0, arg1);
	}

	public void addHeader(String arg0, String arg1) {
		String header = "";
		if (headers.containsKey(arg0)) {
			header = headers.get(arg0).toString();
		}
		headers.put(arg0, header + "," + arg1);
	}

	public void setIntHeader(String arg0, int arg1) {
		headers.put(arg0, new Integer(arg1).toString());
	}

	public void addIntHeader(String arg0, int arg1) {
		String header = "";
		if (headers.containsKey(arg0)) {
			header = headers.get(arg0).toString();
		}
		headers.put(arg0, header + "," + new Integer(arg1).toString());

	}

	public void setStatus(int arg0) {
		if (statusMap.containsKey(arg0))
			headers.put("status", new Integer(arg0).toString());
	}

	// Depreciated
	public void setStatus(int arg0, String arg1) {

	}

	public String getCharacterEncoding() {
		if (headers.containsKey("Character-Encoding")) {
			return headers.get("Character-Encoding");
		}
		return "ISO-8859-1";
	}

	public String getContentType() {
		String reply = headers.get("Content-Type");
		if (reply == null)
			return "text/html";
		return reply;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		return null;
	}

	public PrintWriter getWriter() throws IOException {
		return writer;
		// StringWriter writer = new StringWriter(bufferSize);
		// DataOutputStream out = new
		// DataOutputStream(clientSocket.getOutputStream());
		// PrintWriter pw = new PrintWriter(out);
		// pw.write();
		// return pw;
	}

	public void setCharacterEncoding(String charEncoding) {
		headers.put("Character-Encoding", charEncoding);
	}

	public void setContentLength(int length) {
		headers.put("Content-Length", new Integer(length).toString());
	}

	public void setContentType(String type) {
		headers.put("Content-Type", type);
	}

	public void setBufferSize(int size) {
		if (isCommitted) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		bufferSize = size;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void flushBuffer() throws IOException {
		if (!isCommitted()) {
			generateHeaders("200");
			isCommitted = true;
		}
		try {
			bufferedWriter.write(new String(buffer));
//			System.out.println("buffer:    " + buffer);
			bufferedWriter.flush();
			buffer = new StringBuffer(bufferSize);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	public void resetBuffer() {
		if (!isCommitted()) {
			buffer = new StringBuffer(bufferSize);
		} else {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}

	}

	public boolean isCommitted() {
		return isCommitted;
	}

	public void reset() {
		if (isCommitted()) {
			logger.error("IllegalStateException");
			throw new IllegalStateException();
		}
		headers.clear();
		buffer = new StringBuffer(bufferSize);
	}

	public void setLocale(Locale arg0) {
		locale = arg0;
	}

	public Locale getLocale() {
		return locale;
	}
}
