package com.github.cloudyrock.mongock.decorator;

import com.github.cloudyrock.mongock.decorator.util.MongockDecoratorBase;
import com.mongodb.bulk.BulkWriteResult;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;

import java.util.List;

public interface BulkOperationsDecorator extends BulkOperations, MongockDecoratorBase<BulkOperations> {


  @Override
  default BulkOperations insert(Object documents) {
    return getInvoker().invoke(() -> getImpl().insert(documents));
  }

  @Override
  default BulkOperations insert(List<?> documents) {
    return getInvoker().invoke(() -> getImpl().insert(documents));
  }

  @Override
  default BulkOperations updateOne(Query query, Update update) {
    return getInvoker().invoke(() -> getImpl().updateOne(query, update));
  }

  @Override
  default BulkOperations updateOne(List<Pair<Query, Update>> updates) {
    return getInvoker().invoke(() -> getImpl().updateOne(updates));
  }

  @Override
  default BulkOperations updateMulti(Query query, Update update) {
    return getInvoker().invoke(() -> getImpl().updateMulti(query, update));
  }

  @Override
  default BulkOperations updateMulti(List<Pair<Query, Update>> updates) {
    return getInvoker().invoke(() -> getImpl().updateMulti(updates));
  }

  @Override
  default BulkOperations upsert(Query query, Update update) {
    return getInvoker().invoke(() -> getImpl().upsert(query, update));
  }

  @Override
  default BulkOperations upsert(List<Pair<Query, Update>> updates) {
    return getInvoker().invoke(() -> getImpl().upsert(updates));
  }

  @Override
  default BulkOperations remove(Query remove) {
    return getInvoker().invoke(() -> getImpl().remove(remove));
  }

  @Override
  default BulkOperations remove(List<Query> removes) {
    return getInvoker().invoke(() -> getImpl().remove(removes));
  }

  @Override
  default BulkWriteResult execute() {
    return getInvoker().invoke(() -> getImpl().execute());
  }
}
