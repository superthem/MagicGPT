package com.magicvector.ai.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MagicBook {

    /**
     * Book's name
     */
    @NotNull
    String name();

}
