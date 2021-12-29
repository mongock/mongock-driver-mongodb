package io.mongock.driver.mongodb.sync.v4.driver;

import io.mongock.driver.api.driver.ChangeSetDependency;
import io.mongock.driver.api.driver.Transactional;
import io.mongock.api.exception.MongockException;
import io.mongock.driver.mongodb.sync.v4.repository.MongoSync4ChangeEntryRepository;
import io.mongock.utils.annotation.NotThreadSafe;
import com.mongodb.MongoClientException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.TransactionBody;

import java.util.Optional;
import java.util.Set;

@NotThreadSafe
public abstract class MongoSync4DriverBase extends MongoSync4DriverGeneric {

  private final MongoClient mongoClient;
  protected ClientSession clientSession;

  protected MongoSync4DriverBase(MongoClient mongoClient,
                                 String databaseName,
                                 long lockAcquiredForMillis,
                                 long lockQuitTryingAfterMillis,
                                 long lockTryFrequencyMillis) {
    super(mongoClient.getDatabase(databaseName), lockAcquiredForMillis, lockQuitTryingAfterMillis, lockTryFrequencyMillis);
    this.mongoClient = mongoClient;
  }

  @Override
  public void prepareForExecutionBlock() {
    try {
      clientSession = mongoClient.startSession();
    } catch (MongoClientException ex) {
      throw new MongockException("ERROR starting session. If Mongock is connected to a MongoDB cluster which doesn't support transactions, you must to disable transactions", ex);
    }
  }

  @Override
  public Set<ChangeSetDependency> getDependencies() {
    Set<ChangeSetDependency> dependencies = super.getDependencies();
    if (clientSession != null) {
      ChangeSetDependency clientSessionDependency = new ChangeSetDependency(ClientSession.class, clientSession, false);
      dependencies.remove(clientSessionDependency);
      dependencies.add(clientSessionDependency);
    }

    return dependencies;
  }

  @Override
  public void executeInTransaction(Runnable operation) {

    try {
      getRepository().setClientSession(clientSession);
      clientSession.withTransaction(getTransactionBody(operation), txOptions);
    } catch (Exception ex) {
      throw new MongockException(ex);
    } finally {
      getRepository().clearClientSession();
      clientSession.close();
    }
  }

  private MongoSync4ChangeEntryRepository getRepository() {
    return (MongoSync4ChangeEntryRepository)changeEntryRepository;
  }

  private TransactionBody<String> getTransactionBody(Runnable operation) {
    return () -> {
      operation.run();
      return "Mongock transaction operation";
    };
  }
}
