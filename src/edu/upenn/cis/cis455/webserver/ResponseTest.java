package edu.upenn.cis.cis455.webserver;

import static org.junit.Assert.*;

import java.net.Socket;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.Before;
import org.junit.Test;

public class ResponseTest {

	Request request;
	Response response;

	@Before
	public void setUp() throws Exception {
		response = new Response();
	}

	@Test
	public void testContainsHeader() {
		assertEquals(false, response.containsHeader("Test"));
		response.addHeader("Test", "alifia");
		assertEquals(true, response.containsHeader("Test"));
	}

	@Test
	public void testEncodeURL() {
		assertEquals("abc", response.encodeURL("abc"));
	}

	@Test
	public void testEncodeRedirectURL() {
		assertEquals("xyz", response.encodeURL("xyz"));
	}

	@Test
	public void testSetDateHeader() {
		response.setDateHeader("date", 123);
		assertEquals(true, response.containsHeader("date"));
	}

	@Test
	public void testAddDateHeader() {
		assertEquals(false, response.containsHeader("date"));
		response.addDateHeader("date", 123);
		assertEquals(true, response.containsHeader("date"));
	}

	@Test
	public void testSetHeader() {
		assertEquals(false, response.containsHeader("Test"));
		response.setHeader("Test", "123");
		assertEquals(true, response.containsHeader("Test"));
	}

	@Test
	public void testAddHeader() {
		assertEquals(false, response.containsHeader("Test"));
		response.addHeader("Test", "Test");
		assertEquals(true, response.containsHeader("Test"));
	}

	@Test
	public void testSetIntHeader() {
		assertEquals(false, response.containsHeader("Test"));
		response.addIntHeader("Test", 123);
		assertEquals(true, response.containsHeader("Test"));
	}

	@Test
	public void testAddIntHeader() {
		assertEquals(false, response.containsHeader("Test"));
		response.addIntHeader("Test", 123);
		assertEquals(true, response.containsHeader("Test"));
	}

	@Test
	public void testGetCharacterEncoding() {
		assertEquals("ISO-8859-1", response.getCharacterEncoding());
		response.setCharacterEncoding("UTF-8");
		;
		assertEquals("UTF-8", response.getCharacterEncoding());
	}

	@Test
	public void testGetContentType() {
		assertEquals("text/html", response.getContentType());
	}

	@Test
	public void testSetCharacterEncoding() {
		assertEquals("ISO-8859-1", response.getCharacterEncoding());
		response.setCharacterEncoding("UTF-8");
		;
		assertEquals("UTF-8", response.getCharacterEncoding());
	}

	@Test
	public void testSetContentType() {
		assertEquals("text/html", response.getContentType());
		response.setContentType("image/jpg");
		assertEquals("image/jpg", response.getContentType());
	}

	@Test
	public void testSetBufferSize() {
		response.setBufferSize(100);
		assertTrue("message: ", 100 <= response.getBufferSize());
	}

	@Test
	public void testGetBufferSize() {
		response.setBufferSize(100);
		assertTrue("message: ", 100 <= response.getBufferSize());
	}
}
