package com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.driver;


import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.SpringData3DriverTestAdapterImpl;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.SpringDataMongoV3Driver;
import com.github.cloudyrock.mongock.driver.mongodb.test.template.MongoDriverITestBase;
import com.github.cloudyrock.mongock.driver.mongodb.test.template.util.MongoDBDriverTestAdapter;
import org.springframework.data.mongodb.core.MongoTemplate;

public class SpringDataMongo3DriverITest extends MongoDriverITestBase {

  @Override
  protected SpringDataMongoV3Driver getDriverWithTransactionDisabled() {
    SpringDataMongoV3Driver driver = SpringDataMongoV3Driver.withDefaultLock(getMongoTemplate());
    driver.setChangeLogCollectionName(CHANGELOG_COLLECTION_NAME);
    return driver;
  }

  @Override
  protected MongoDBDriverTestAdapter getAdapter(String collectionName) {
    return new SpringData3DriverTestAdapterImpl(getDataBase().getCollection(collectionName));
  }

  private MongoTemplate getMongoTemplate() {
    return new MongoTemplate(this.getMongoClient(), this.getDataBase().getName());
  }
}
