package com.petro.locklib.locks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Lock {

  /**
   * Type of lock.
   *
   * <p>"Player", "Department".
   */
  String type();

  /**
   * Optional property. Uses input params by default. Parameters are using to construct identifier.
   * Spring Expression Language (SpEL) supported.
   *
   * <p>Better to use reasonable number of parameters.
   *
   * <p>params = {"#request.deptId","#request.playerID", @someCoolBean.coolMethod()}</p>
   * <p>params = {"#c.list.![b.id]","#id", "#potatoId, 2 > 1 ? 'a' : 'b'"}</p>
   */
  String[] params() default {};

  /**
   * Duration of lock. In milliseconds.
   */
  int duration() default -1;

  int waitLock() default 300;

}
