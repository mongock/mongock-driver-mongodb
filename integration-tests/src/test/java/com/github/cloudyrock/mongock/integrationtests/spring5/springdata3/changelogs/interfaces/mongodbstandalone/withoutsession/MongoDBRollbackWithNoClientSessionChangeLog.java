package com.github.cloudyrock.mongock.integrationtests.spring5.springdata3.changelogs.interfaces.mongodbstandalone.withoutsession;

import com.github.cloudyrock.mongock.integrationtests.spring5.springdata3.client.Client;
import com.github.cloudyrock.mongock.interfaces.ChangeLog;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MongoDBRollbackWithNoClientSessionChangeLog implements ChangeLog {
  public static final String COLLECTION_NAME = MongoDBRollbackWithNoClientSessionChangeLog.class.getSimpleName() + "Collection";
  private final MongoDatabase db;

  public MongoDBRollbackWithNoClientSessionChangeLog(MongoDatabase db) {
    this.db = db;
  }
  @Override
  public String geId() {
    return getClass().getSimpleName();
  }
  @Override
  public String getAuthor() {
    return "mongock_test";
  }

  @Override
  public String getOrder() {
    return "1";
  }

  @Override
  public boolean isFailFast() {
    return true;
  }

  @Override
  public String getSystemVersion() {
    return "1";
  }

  @Override
  public void changeSet() {

    CodecRegistry pojoCodecRegistry = org.bson.codecs.configuration.CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), org.bson.codecs.configuration.CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    MongoCollection<Client> clientCollection = db.withCodecRegistry(pojoCodecRegistry).getCollection(MongoDBRollbackWithNoClientSessionChangeLog.COLLECTION_NAME, Client.class);
    List<Client> clients = IntStream.range(0, 10)
        .mapToObj(i -> new Client("name-" + i, "email-" + i, "phone" + i, "country" + i))
        .collect(Collectors.toList());
    clientCollection.insertMany(clients);
    throw new RuntimeException("Expected exception in changeLog[Before]");
  }

  @Override
  public void before() {
  }

  @Override
  public void rollbackBefore() {

  }

  @Override
  public void rollback() {

  }
}