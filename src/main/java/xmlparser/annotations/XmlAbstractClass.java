package xmlparser.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used for deserializing fields of abstract classes
 */
@Retention(RUNTIME)
public @interface XmlAbstractClass {

    String attribute() default "class";

    String tag() default "";

    TypeMap[] types();

    @interface TypeMap {
        String name();

        Class<?> type();
    }
}
