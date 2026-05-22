package io.floci.gcp.core.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XmlBuilderTest {

    @Test
    void buildSimpleElement() {
        String xml = new XmlBuilder()
                .start("Root")
                .elem("Name", "hello")
                .end("Root")
                .build();
        assertEquals("<Root><Name>hello</Name></Root>", xml);
    }

    @Test
    void startWithXmlns() {
        String xml = new XmlBuilder()
                .start("ListBucketResult", "http://s3.amazonaws.com/doc/2006-03-01/")
                .end("ListBucketResult")
                .build();
        assertTrue(xml.contains("xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\""));
    }

    @Test
    void elemNullValueIsSkipped() {
        String xml = new XmlBuilder()
                .start("R")
                .elem("Present", "yes")
                .elem("Missing", (String) null)
                .end("R")
                .build();
        assertFalse(xml.contains("Missing"));
        assertTrue(xml.contains("<Present>yes</Present>"));
    }

    @Test
    void elemLong() {
        String xml = new XmlBuilder().elem("Size", 1024L).build();
        assertEquals("<Size>1024</Size>", xml);
    }

    @Test
    void elemBoolean() {
        String xml = new XmlBuilder().elem("Truncated", false).build();
        assertEquals("<Truncated>false</Truncated>", xml);
    }

    @Test
    void rawFragment() {
        String xml = new XmlBuilder()
                .start("R")
                .raw("<Inner>x</Inner>")
                .end("R")
                .build();
        assertEquals("<R><Inner>x</Inner></R>", xml);
    }

    @Test
    void rawNullIsIgnored() {
        String xml = new XmlBuilder().raw(null).build();
        assertEquals("", xml);
    }

    @Test
    void escapeAmpersand() {
        assertEquals("a&amp;b", XmlBuilder.escape("a&b"));
    }

    @Test
    void escapeLessThan() {
        assertEquals("a&lt;b", XmlBuilder.escape("a<b"));
    }

    @Test
    void escapeGreaterThan() {
        assertEquals("a&gt;b", XmlBuilder.escape("a>b"));
    }

    @Test
    void escapeDoubleQuote() {
        assertEquals("a&quot;b", XmlBuilder.escape("a\"b"));
    }

    @Test
    void escapeSingleQuote() {
        assertEquals("a&apos;b", XmlBuilder.escape("a'b"));
    }

    @Test
    void escapeNoSpecialCharsReturnsOriginal() {
        String s = "hello world 123";
        assertSame(s, XmlBuilder.escape(s));
    }

    @Test
    void escapeNullReturnsEmpty() {
        assertEquals("", XmlBuilder.escape(null));
    }

    @Test
    void escapeEmptyReturnsEmpty() {
        assertEquals("", XmlBuilder.escape(""));
    }

    @Test
    void escapeMixedSpecialChars() {
        assertEquals("&lt;a&gt;&amp;&quot;b&apos;", XmlBuilder.escape("<a>&\"b'"));
    }

    @Test
    void elemEscapesValue() {
        String xml = new XmlBuilder().elem("Key", "a&b<c>d").build();
        assertEquals("<Key>a&amp;b&lt;c&gt;d</Key>", xml);
    }
}
