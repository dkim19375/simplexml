package xmlparser.parsing;

public interface ObjectSerializer {
    static ObjectSerializer defaultSerializer() {
        return Object::toString;
    }

    String convert(Object value);
}

