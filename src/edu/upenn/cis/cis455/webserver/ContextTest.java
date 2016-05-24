package edu.upenn.cis.cis455.webserver;

import java.util.Enumeration;

import junit.framework.TestCase;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ContextTest {
	Context context;

	@Before
	public void setup() {
		context = new Context();
	}

	@Test
	public void testAttributeNull() {
		assertNull(context.getAttribute("a"));
	}

	@Test
	public void testAttributeNotNull() {
		context.setAttribute("test", "123");
		assertEquals("123", context.getAttribute("test"));
	}

	@Test
	public void testRemoveAttributes() {
		context.setAttribute("test", "123");
		context.setAttribute("test2", "456");
		context.removeAttribute("test");
		assertTrue(context.getAttributeNames().hasMoreElements());
		context.removeAttribute("test2");
		assertFalse(context.getAttributeNames().hasMoreElements());
	}

	@Test
	public void testAttributeNames() {
		context.setAttribute("test", "123");
		context.setAttribute("test2", "456");
		context.setAttribute("test3", "789");
		Enumeration enumeration = context.getAttributeNames();
		while (enumeration.hasMoreElements()) {
			String t = (String) enumeration.nextElement();
			assertTrue(t.equals("test") || t.equals("test2")
					|| t.equals("test3"));
		}
	}

	@Test
	public void testParams() {
		context.setInitParam("test", "1234");
		assertEquals(context.getInitParameter("test"), "1234");
	}
}
