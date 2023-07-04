package question;

import xmlparser.XmlParser;
import xmlparser.annotations.XmlName;

public class Question2 {

    public static final String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<ns0:InputRequest xmlns:ns0=\"OW\">\n" +
                    "    <ns0:FMan>\n" +
                    "        <ns0:CInc>\n" +
                    "            <ns0:Act>Sample</ns0:Act>\n" +
                    "            <ns0:Op>Sample</ns0:Op>\n" +
                    "            <ns0:Int>AppInt</ns0:Int>\n" +
                    "        </ns0:CInc>\n" +
                    "    </ns0:FMan>\n" +
                    "</ns0:InputRequest>";

    public static void main(final String... args) {
        final XmlParser parser = new XmlParser();
        final Cint c = parser.fromXml(xml, "ns0:InputRequest/ns0:FMan/ns0:CInc", Cint.class);
        System.out.println(c.act + " - " + c.op + " - " + c.Int);
    }

    @XmlName("ns0:CInc")
    public class Cint {
        @XmlName("ns0:Act")
        public String act;
        @XmlName("ns0:Op")
        public String op;
        @XmlName("ns0:Int")
        public String Int;
    }

}
