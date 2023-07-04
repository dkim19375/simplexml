package unittests;

import org.junit.Test;
import xmlparser.error.InvalidXPath;
import xmlparser.xpath.Predicate;
import xmlparser.xpath.Segment;
import xmlparser.xpath.TextSegment;
import xmlparser.xpath.XPathExpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XPathParseTest {

    @Test
    public void testEmpty() {
        XPathExpression xpe = XPathExpression.newXPath("");
        assertNotNull(xpe);
        assertEquals(1, xpe.segments.size());
        Segment seg = xpe.segments.get(0);
        assertNotNull(seg);
        assertEquals("", seg.elementName);
    }

    @Test
    public void testSingleElement() {
        XPathExpression xpe = XPathExpression.newXPath("a");
        assertNotNull(xpe);
        assertEquals(1, xpe.segments.size());
        Segment seg = xpe.segments.get(0);
        assertNotNull(seg);
        assertEquals("a", seg.elementName);
    }

    @Test
    public void testTwoElements() {
        XPathExpression xpe = XPathExpression.newXPath("foo/bar");
        assertNotNull(xpe);
        assertEquals(2, xpe.segments.size());
        {
            Segment seg = xpe.segments.get(0);
            assertNotNull(seg);
            assertEquals("foo", seg.elementName);
        }
        {
            Segment seg = xpe.segments.get(1);
            assertNotNull(seg);
            assertEquals("bar", seg.elementName);
        }
    }

    @Test
    public void testElementPredicate() {
        XPathExpression xpe = XPathExpression.newXPath("foo[a=0]");
        assertNotNull(xpe);
        assertEquals(1, xpe.segments.size());
        {
            Segment seg = xpe.segments.get(0);
            assertNotNull(seg);
            assertEquals("foo", seg.elementName);
            assertEquals(1, seg.predicates.size());
            for (Predicate p : seg.predicates) {
                assertNotNull(p);
            }
        }
    }

    @Test
    public void testSinglePredicate() {
        XPathExpression xpe = XPathExpression.newXPath("[a=0]");
        assertNotNull(xpe);
        assertEquals(1, xpe.segments.size());
        Segment seg = xpe.segments.get(0);
        assertNotNull(seg);
        assertEquals("", seg.elementName);
        assertEquals(1, seg.predicates.size());
        for (Predicate p : seg.predicates) {
            assertNotNull(p);
        }
    }

    @Test
    public void testTwoPredicates() {
        XPathExpression xpe = XPathExpression.newXPath("[a=0][b=1]");
        assertNotNull(xpe);
        assertEquals(1, xpe.segments.size());
        Segment seg = xpe.segments.get(0);
        assertNotNull(seg);
        assertEquals("", seg.elementName);
        assertEquals(2, seg.predicates.size());
        for (Predicate p : seg.predicates) {
            assertNotNull(p);
        }
    }

    @Test
    public void testTwoPredicatesSpace() {
        XPathExpression xpe = XPathExpression.newXPath("[a=0] [b=1]");
        assertNotNull(xpe);
        assertEquals(1, xpe.segments.size());
        Segment seg = xpe.segments.get(0);
        assertNotNull(seg);
        assertEquals("", seg.elementName);
        assertEquals(2, seg.predicates.size());
        for (Predicate p : seg.predicates) {
            assertNotNull(p);
        }
    }

    @Test
    public void testTextSegment1() {
        XPathExpression xpe = XPathExpression.newXPath("text()");
        assertNotNull(xpe);
        assertEquals(1, xpe.segments.size());
        Segment seg = xpe.segments.get(0);
        assertNotNull(seg);
        assertTrue(seg instanceof TextSegment);
    }

    @Test
    public void testTextSegment2() {
        XPathExpression xpe = XPathExpression.newXPath("foo/text()");
        assertNotNull(xpe);
        assertEquals(2, xpe.segments.size());
        Segment seg = xpe.segments.get(1);
        assertNotNull(seg);
        assertTrue(seg instanceof TextSegment);
    }

    @Test(expected = InvalidXPath.class)
    public void testNull() {
        XPathExpression.newXPath(null);
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformed1() {
        XPathExpression.newXPath("[");
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformed2() {
        XPathExpression.newXPath("a[");
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformed3() {
        XPathExpression.newXPath("[]");
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformed4() {
        XPathExpression.newXPath("a]");
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformed5() {
        XPathExpression.newXPath("abc[a=1]][b=0]");
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformed6() {
        XPathExpression.newXPath("ab]cd[foo=0]");
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformedEquality1() {
        XPathExpression.newXPath("a[=]");
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformedEquality2() {
        XPathExpression.newXPath("a[=c]");
        fail("Should throw an exception");
    }

    @Test(expected = InvalidXPath.class)
    public void testMalformedEquality3() {
        XPathExpression.newXPath("a[c=]");
        fail("Should throw an exception");
    }

}
