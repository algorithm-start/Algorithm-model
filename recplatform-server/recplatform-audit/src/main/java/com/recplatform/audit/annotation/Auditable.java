package com.recplatform.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that should be audited.
 * Applied to controller methods to automatically log audit entries.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The action being performed, e.g., "CREATE", "UPDATE", "DELETE".
     */
    String action();

    /**
     * The resource being acted upon, e.g., "solver_problem", "data_source".
     */
    String resource();

    /**
     * SpEL expression to extract detail from method parameters.
     * Example: "#request.name" or "#id"
     */
    String detail() default "";
}
