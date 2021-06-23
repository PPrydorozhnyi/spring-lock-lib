package com.petro.locklib.locks;

import com.petro.locklib.exceptions.LockAcquireException;
import com.petro.locklib.exceptions.LockingOperationException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LockAspect {

  @Value("${spring.application.name:SERVICE}")
  private String serviceName;

  private final ExpressionParser parser;
  private final LocalVariableTableParameterNameDiscoverer discoverer;
  private final RedissonClient redisson;
  private final StandardEvaluationContext context;

  public LockAspect(RedissonClient redisson,
                    BeanFactory beanFactory) {
    this.redisson = redisson;
    this.context = new StandardEvaluationContext();
    context.setBeanResolver(new BeanFactoryResolver(beanFactory));
    this.parser = new SpelExpressionParser();
    this.discoverer = new LocalVariableTableParameterNameDiscoverer();
  }

  @Pointcut(value = "@annotation(lock)", argNames = "lock")
  public void lockPointcut(Lock lock) {
    // Pointcut does not need any additional code.
  }

  @Around(value = "lockPointcut(lock)", argNames = "pjp,lock")
  public Object lock(ProceedingJoinPoint pjp, Lock lock) throws Throwable {
    final var method = ((MethodSignature) pjp.getSignature()).getMethod();
    Lock annotation = method.getAnnotation(Lock.class);

    List<String> lockParams = getLockParams(annotation, method, pjp.getArgs());
    final var lockKey = LockUtils.getLockKey(lock.type(), lockParams);
    final var rLock = redisson.getLock(lockKey);

    final boolean tryLock;
    if (lock.duration() <= 0) {
      tryLock = rLock.tryLock(lock.waitLock(), TimeUnit.MILLISECONDS);
    } else {
      tryLock = rLock.tryLock(lock.waitLock(), lock.duration(), TimeUnit.MILLISECONDS);
    }

    if (tryLock) {
      try {
        return pjp.proceed();
      } finally {
        rLock.unlock();
      }
    } else {
      throw new LockAcquireException(lockKey, lockParams);
    }
  }

  private List<String> getLockParams(Lock annotation, Method method, Object[] args) {
    List<String> list = new ArrayList<>();
    String[] lockParams = annotation.params();
    String[] paramList = discoverer.getParameterNames(method);

    for (String lockParam : lockParams) {

      if (paramList != null && paramList.length != 0) {
        for (var len = 0; len < paramList.length; len++) {
          context.setVariable(paramList[len], args[len]);
        }
      }

      Exception ex = null;
      Object value = null;

      try {
        final var expression = parser.parseExpression(lockParam);
        value = expression.getValue(context);
      } catch (Exception e) {
        ex = e;
      }

      if (value == null) {
        throw new LockingOperationException("Wrong SpEL expression produces exception or null "
           + "value", ex);
      }

      list.add(value.toString());
    }
    return list;
  }

}
