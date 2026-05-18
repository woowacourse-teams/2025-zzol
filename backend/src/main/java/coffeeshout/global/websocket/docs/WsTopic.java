package coffeeshout.global.websocket.docs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WsTopics.class)
public @interface WsTopic {

    String path();

    Class<?> payload();

    Class<?> generic() default Void.class;

    String description() default "";
}
