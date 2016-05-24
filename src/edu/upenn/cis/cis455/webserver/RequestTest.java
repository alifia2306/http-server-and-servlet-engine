package edu.upenn.cis.cis455.webserver;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class RequestTest {

	InputStream inputStream;
	Request request;

	@Before
	public void setUp() throws Exception {
		HashMap<String, String> hash = new HashMap<String, String>();
		hash.put("Host", "localhost:8080");
		hash.put("method", "get");
		hash.put("version", "http/1.1");
		hash.put("content-length", "52");
		hash.put("content-type", "text/html");
		hash.put("path", "http://localhost:8080/index");
		Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(
				"http://localhost:8080/calculate?num1=1&num2=5",
				"CalculatorServlet");
		request = new Request(null, hash, null, entry, null, null);

	}

	@Test
	public void testGetHeader() {
		assertEquals("localhost:8080", request.getHeader("Host"));
	}

	@Test
	public void testGetMethod() {
		request.setMethod("get");
		assertEquals("get", request.getMethod().toLowerCase());
	}

	@Test
	public void testGetContextPath() {
		assertEquals("", request.getContextPath());
	}

	public void testGetQueryString() {
		assertEquals("test=test&a=0", request.getQueryString());
	}

	@Test
	public void testGetRequestURI() {
		assertEquals("/index", request.getRequestURI());
	}

	@Test
	public void testIsRequestedSessionIdFromURL() {
		assertEquals(false, request.isRequestedSessionIdFromURL());
	}

	@Test
	public void testGetContentLength() {
		assertEquals(52, request.getContentLength());
	}

	@Test
	public void testGetContentType() {
		assertEquals("text/html", request.getContentType());
	}

	@Test
	public void testGetParameter() {
		request.setParameter("test", "1");
		assertEquals("1", request.getParameter("test"));
	}

	@Test
	public void testGetParameterValues() {
		request.setParameter("test", "1");
		assertEquals("1", request.getParameter("test"));
	}

	@Test
	public void testGetProtocol() {
		assertEquals("http/1.1", request.getProtocol().toLowerCase());
	}

	@Test
	public void testGetScheme() {
		assertEquals("http", request.getScheme());
	}

	@Test
	public void testIsSecure() {
		assertEquals(false, request.isSecure());
	}
}
