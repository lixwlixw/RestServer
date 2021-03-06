package com.asiainfo.ocmanager.auth;

import javax.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Yulishan on 2017/6/14.
 * Authentication annotation
 * For now, this interface contains some misleads
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Deprecated
public @interface PageAuth {
  String requiredPermission() default "";
}
