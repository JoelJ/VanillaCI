package com.vanillaci.distributedinvoke;

import com.vanillaci.distributedinvoke.machines.labels.Label;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 7:39 PM
 */
public class LabelTest {
	@Test
	public void testParse_simpleInclude() throws Exception {
		String expression = "MyName";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 1);
		assertEquals(parse.getExcludes().size(), 0);
		assertTrue(parse.matches(expression));
		assertFalse(parse.matches("ThisShouldNotMatchAnythingElse"));
	}

	@Test
	public void testParse_simpleExclude() throws Exception {
		String expression = "!MyName";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 0);
		assertEquals(parse.getExcludes().size(), 1);
		assertFalse(parse.matches("MyName"));
		assertTrue(parse.matches("ShouldAcceptAnythingElse"));
	}

	@Test
	public void testParse_simpleExclude_precedence() throws Exception {
		String expression = "MyName !MyName";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 1);
		assertEquals(parse.getExcludes().size(), 1);
		assertFalse(parse.matches("MyName"), "Exclude should take precedence over include");
	}

	@Test
	public void testParse_multiple() throws Exception {
		String expression = "MyName\r\nis  Joel\tJohnson";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 4);
		assertEquals(parse.getExcludes().size(), 0);
		assertTrue(parse.matches("MyName"));
		assertTrue(parse.matches("is"));
		assertTrue(parse.matches("Joel"));
		assertTrue(parse.matches("Johnson"));
	}

	@Test
	public void testParse_empty() throws Exception {
		String expression = "";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 0);
		assertEquals(parse.getExcludes().size(), 0);
		assertTrue(parse.matches("This"));
		assertTrue(parse.matches("Should"));
		assertTrue(parse.matches("Match"));
		assertTrue(parse.matches("Everything"));
	}

	@Test
	public void testParse_whitespace() throws Exception {
		String expression = "     \t\t\t \t\t   \t";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 0);
		assertEquals(parse.getExcludes().size(), 0);
		assertTrue(parse.matches("This"));
		assertTrue(parse.matches("Should"));
		assertTrue(parse.matches("Match"));
		assertTrue(parse.matches("Everything"));
	}

	@Test
	public void testParse_whitespaceAndOneElement_end() throws Exception {
		String expression = "     \t\t\t \t\t   \tLabel";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 1);
		assertEquals(parse.getExcludes().size(), 0);
		assertTrue(parse.matches("Label"));
		assertFalse(parse.matches("ThisShouldNotMatchAnythingElse"));
	}

	@Test
	public void testParse_whitespaceAndOneElement_beginning() throws Exception {
		String expression = "Label     \t\t\t \t\t   \t";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 1);
		assertEquals(parse.getExcludes().size(), 0);
		assertTrue(parse.matches("Label"));
		assertFalse(parse.matches("ThisShouldNotMatchAnythingElse"));
	}

	@Test
	public void testParse_whitespaceAndOneElement_middle() throws Exception {
		String expression = "     \t\t\t Label\t\t   \t";
		Label.Expression parse = Label.parse(expression);

		assertEquals(parse.getIncludes().size(), 1);
		assertEquals(parse.getExcludes().size(), 0);
		assertTrue(parse.matches("Label"));
		assertFalse(parse.matches("ThisShouldNotMatchAnythingElse"));
	}
}
