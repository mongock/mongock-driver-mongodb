package io.mongock.driver.mongodb.springdata.v2.decorator.operation.executable.update;

import io.mongock.driver.api.lock.guard.decorator.Invokable;
import io.mongock.driver.mongodb.springdata.v2.decorator.operation.executable.update.impl.FindAndReplaceWithOptionsDecoratorImpl;
import org.springframework.data.mongodb.core.ExecutableUpdateOperation;

public interface FindAndReplaceWithProjectionDecorator<T>  extends Invokable, ExecutableUpdateOperation.FindAndReplaceWithProjection<T>, FindAndReplaceWithOptionsDecorator<T> {

  ExecutableUpdateOperation.FindAndReplaceWithProjection<T> getImpl();

  @Override
  default  <R> ExecutableUpdateOperation.FindAndReplaceWithOptions<R> as(Class<R> resultType) {
    return new FindAndReplaceWithOptionsDecoratorImpl<>(getInvoker().invoke(()-> getImpl().as(resultType)), getInvoker());
  }
}