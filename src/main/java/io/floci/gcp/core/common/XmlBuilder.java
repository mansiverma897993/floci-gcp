package io.floci.gcp.core.common;

/**
 * Fluent, allocation-efficient XML builder backed by a plain {@link StringBuilder}.
 * Used by GCS for bucket listing and multipart upload XML responses.
 */
public final class XmlBuilder {

    private final StringBuilder sb = new StringBuilder();

    public XmlBuilder start(String element, String xmlns) {
        sb.append('<').append(element);
        if (xmlns != null) {
            sb.append(" xmlns=\"").append(xmlns).append('"');
        }
        sb.append('>');
        return this;
    }

    public XmlBuilder start(String element) {
        return start(element, null);
    }

    public XmlBuilder end(String element) {
        sb.append("</").append(element).append('>');
        return this;
    }

    public XmlBuilder elem(String name, String value) {
        if (value == null) {
            return this;
        }
        sb.append('<').append(name).append('>')
          .append(escape(value))
          .append("</").append(name).append('>');
        return this;
    }

    public XmlBuilder elem(String name, long value) {
        return elem(name, String.valueOf(value));
    }

    public XmlBuilder elem(String name, boolean value) {
        return elem(name, String.valueOf(value));
    }

    public XmlBuilder raw(String fragment) {
        if (fragment != null) {
            sb.append(fragment);
        }
        return this;
    }

    public String build() {
        return sb.toString();
    }

    public static String escape(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder out = null;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            String replacement = switch (c) {
                case '&'  -> "&amp;";
                case '<'  -> "&lt;";
                case '>'  -> "&gt;";
                case '"'  -> "&quot;";
                case '\'' -> "&apos;";
                default   -> null;
            };
            if (replacement != null) {
                if (out == null) {
                    out = new StringBuilder(len + 8);
                    out.append(s, 0, i);
                }
                out.append(replacement);
            } else if (out != null) {
                out.append(c);
            }
        }
        return out != null ? out.toString() : s;
    }
}
