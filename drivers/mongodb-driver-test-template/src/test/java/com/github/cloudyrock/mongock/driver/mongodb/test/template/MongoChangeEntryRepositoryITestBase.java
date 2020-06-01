package com.github.cloudyrock.mongock.driver.mongodb.test.template;

import com.github.cloudyrock.mongock.driver.mongodb.test.template.util.IntegrationTestBase;
import com.mongodb.client.model.IndexOptions;
import io.changock.driver.api.entry.ChangeState;
import io.changock.driver.core.entry.ChangeEntryRepository;
import io.changock.migration.api.exception.ChangockException;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.util.Date;
import java.util.regex.Matcher;


public abstract class MongoChangeEntryRepositoryITestBase extends IntegrationTestBase {

  protected ChangeEntryRepository repository;

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void shouldThrowException_WhenNoIndexCreation_IfIndexNoPreviouslyCreated() throws ChangockException {
    exceptionRule.expect(ChangockException.class);
    exceptionRule.expectMessage("Index creation not allowed, but not created or wrongly created for collection");
    initializeRepository(false);
  }


  @Test
  public void shouldBeOk_WhenNoIndexCreation_IfIndexAlreadyCreated() throws ChangockException {
    getAdapter().createIndex(getIndexDocument(new String[]{"executionId", "author", "changeId"}), new IndexOptions().unique(true));
    initializeRepository(false);
  }

  @Test
  public void shouldReturnFalse_whenHasNotBeenExecuted_IfThereIsWithSameIdAndAuthorAndStateNull() {
    initializeRepository(true);
    String changeId = "changeId";
    String author = "author";
    String executionId = "executionId";
    createAndInsertChangeEntry(true, null, changeId, author, executionId);
    Assert.assertEquals("pre-requisite: changeEntry should be added", 1,
        getAdapter().countDocuments(new Document().append("changeId", changeId).append("author", author)));

    Assert.assertTrue(repository.isAlreadyExecuted(changeId, author));
  }

  @Test
  public void shouldReturnFalse_whenHasNotBeenExecuted_IfThereIsWithSameIdAndAuthorAndNoState() {
    initializeRepository(true);
    String changeId = "changeId";
    String author = "author";
    String executionId = "executionId";
    createAndInsertChangeEntry(false, null, changeId, author, executionId);
    Assert.assertEquals("pre-requisite: changeEntry should be added", 1,
        getAdapter().countDocuments(new Document().append("changeId", changeId).append("author", author)));

    Assert.assertTrue(repository.isAlreadyExecuted(changeId, author));
  }


  @Test
  public void shouldReturnFalse_whenHasNotBeenExecuted_IfThereIsWithSameIdAndAuthorAndStateEXECUTED() {
    initializeRepository(true);
    String changeId = "changeId";
    String author = "author";
    String executionId = "executionId";
    createAndInsertChangeEntry(true, ChangeState.EXECUTED.toString(), changeId, author, executionId);
    Assert.assertEquals("pre-requisite: changeEntry should be added", 1,
        getAdapter().countDocuments(new Document().append("changeId", changeId).append("author", author)));

    Assert.assertTrue(repository.isAlreadyExecuted(changeId, author));
  }

  @Test
  public void shouldReturnTrue_whenHasNotBeenExecuted_IfThereIsWithSameIdAndAuthorAndStateIGNORED() {
    initializeRepository(true);
    String changeId = "changeId";
    String author = "author";
    String executionId = "executionId";
    createAndInsertChangeEntry(true, ChangeState.IGNORED.toString(), changeId, author, executionId);
    Assert.assertEquals("pre-requisite: changeEntry should be added", 1,
        getAdapter().countDocuments(new Document().append("changeId", changeId).append("author", author)));

    Assert.assertFalse(repository.isAlreadyExecuted(changeId, author));
  }

  @Test
  public void shouldReturnTrue_whenHasNotBeenExecuted_IfThereIsWithSameIdAndAuthorAndStateFAILED() {
    initializeRepository(true);
    String changeId = "changeId";
    String author = "author";
    String executionId = "executionId";
    createAndInsertChangeEntry(true, ChangeState.FAILED.toString(), changeId, author, executionId);
    Assert.assertEquals("pre-requisite: changeEntry should be added", 1,
        getAdapter().countDocuments(new Document().append("changeId", changeId).append("author", author)));

    Assert.assertFalse(repository.isAlreadyExecuted(changeId, author));
  }


  private void createAndInsertChangeEntry(boolean withState, String state, String changeId, String author, String executionId) {
    initializeRepository(true);
    Document existingEntry = new Document()
        .append("executionId", executionId)
        .append("changeId", changeId)
        .append("author", author)
        .append("timestamp", Date.from(Instant.now()))
        .append("changeLogClass", "anyClass")
        .append("changeSetMethod", "anyMethod")
        .append("metadata", null);
    if (withState) {
      existingEntry = existingEntry.append("state", state);
    }
    getAdapter().insertOne(existingEntry);
  }

  protected abstract void initializeRepository(boolean indexCreation);

  protected Document getIndexDocument(String[] uniqueFields) {
    final Document indexDocument = new Document();
    for (String field : uniqueFields) {
      indexDocument.append(field, 1);
    }
    return indexDocument;
  }
}
