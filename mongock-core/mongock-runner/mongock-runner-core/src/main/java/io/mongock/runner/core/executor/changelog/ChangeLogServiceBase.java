package io.mongock.runner.core.executor.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.exception.MongockException;
import io.mongock.driver.api.common.Validable;
import io.mongock.runner.core.annotation.AnnotationProcessor;
import io.mongock.runner.core.annotation.LegacyAnnotationProcessor;
import io.mongock.runner.core.internal.ChangeLogItem;
import io.mongock.runner.core.internal.ChangeSetItem;
import io.mongock.utils.CollectionUtils;
import io.mongock.utils.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.reflections.Reflections;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;


/**
 * Utilities to deal with reflections and annotations
 *
 * @since 27/07/2014
 */
public abstract class ChangeLogServiceBase<CHANGELOG extends ChangeLogItem<CHANGESET>, CHANGESET extends ChangeSetItem> implements Validable {


  private final LegacyAnnotationProcessor<CHANGESET> legacyAnnotationProcessor;
  private final AnnotationProcessor annotationProcessor;
  protected Function<AnnotatedElement, Boolean> profileFilter;
  private Function<Class<?>, Object> changeLogInstantiator;
  private List<String> changeLogsBasePackageList = Collections.emptyList();
  private List<Class<?>> changeLogsBaseClassList = Collections.emptyList();
  private ArtifactVersion startSystemVersion = new DefaultArtifactVersion("0");
  private ArtifactVersion endSystemVersion = new DefaultArtifactVersion(String.valueOf(Integer.MAX_VALUE));
  private String defaultMigrationAuthor = null;

  public ChangeLogServiceBase(AnnotationProcessor annotationProcessor, LegacyAnnotationProcessor<CHANGESET> legacyAnnotationProcessor) {
    this.legacyAnnotationProcessor = legacyAnnotationProcessor;
    this.annotationProcessor = annotationProcessor;
  }

  protected LegacyAnnotationProcessor<CHANGESET> getLegacyAnnotationProcessor() {
    return legacyAnnotationProcessor;
  }

  protected AnnotationProcessor getAnnotationProcessor() {
    return annotationProcessor;
  }

  protected List<String> getChangeLogsBasePackageList() {
    return changeLogsBasePackageList;
  }

  public void setChangeLogsBasePackageList(List<String> changeLogsBasePackageList) {
    this.changeLogsBasePackageList = changeLogsBasePackageList;
  }

  protected List<Class<?>> getChangeLogsBaseClassList() {
    return changeLogsBaseClassList;
  }

  public void setChangeLogsBaseClassList(List<Class<?>> changeLogsBaseClassList) {
    this.changeLogsBaseClassList = changeLogsBaseClassList;
  }

  protected ArtifactVersion getStartSystemVersion() {
    return startSystemVersion;
  }

  public void setStartSystemVersion(String startSystemVersion) {
    this.startSystemVersion = new DefaultArtifactVersion(startSystemVersion);
  }

  protected ArtifactVersion getEndSystemVersion() {
    return endSystemVersion;
  }

  public void setEndSystemVersion(String endSystemVersion) {
    this.endSystemVersion = new DefaultArtifactVersion(endSystemVersion);
  }

  protected Function<AnnotatedElement, Boolean> getProfileFilter() {
    return profileFilter;
  }

  public void setProfileFilter(Function<AnnotatedElement, Boolean> profileFilter) {
    this.profileFilter = profileFilter;
  }

  protected Optional<Function<Class<?>, Object>> getChangeLogInstantiator() {
    return Optional.ofNullable(changeLogInstantiator);
  }

  public String getDefaultMigrationAuthor() {
    return defaultMigrationAuthor;
  }

  public void setDefaultMigrationAuthor(String defaultMigrationAuthor) {
    this.defaultMigrationAuthor = defaultMigrationAuthor;
  }

  @Override
  public void runValidation() throws MongockException {
    if (
        (CollectionUtils.isNullEmpty(changeLogsBasePackageList) || !changeLogsBasePackageList.stream().allMatch(StringUtils::hasText))
            && CollectionUtils.isNullEmpty(changeLogsBaseClassList)) {
      throw new MongockException("Scan package for changeLogs is not set: use appropriate setter");
    }
  }

  public SortedSet<CHANGELOG> fetchChangeLogs() {
    TreeSet<CHANGELOG> changeLogs = mergeChangeLogClassesAndPackages()
        .stream()
        .filter(changeLogClass -> this.profileFilter != null ? this.profileFilter.apply(changeLogClass) : true)
        .map(this::buildChangeLogObject)
        .collect(Collectors.toCollection(() -> new TreeSet<>(new ChangeLogComparator<>())));
    validateDuplications(changeLogs);
    return changeLogs;
  }

  private void validateDuplications(Set<CHANGELOG> changeLogs) {
    ThrowableHashSet allChangeSets = new ThrowableHashSet();
    changeLogs.stream()
        .map(CHANGELOG::getAllChangeItems)
        .flatMap(List::stream)
        .forEach(allChangeSets::addAndThrow);
  }


  private Set<Class<?>> mergeChangeLogClassesAndPackages() {
    //the following check is needed because reflection library will bring the entire classpath in case the changeLogsBasePackageList is empty
    final Stream<Class<?>> scannedPackageStream = changeLogsBasePackageList != null && !changeLogsBasePackageList.isEmpty()
        ? Stream.concat(
        new Reflections(changeLogsBasePackageList).getTypesAnnotatedWith(ChangeLog.class).stream(),
        new Reflections(changeLogsBasePackageList).getTypesAnnotatedWith(ChangeUnit.class).stream())
        : Stream.empty();
    return Stream.concat(changeLogsBaseClassList.stream(), scannedPackageStream).collect(Collectors.toSet());
  }

  protected List<CHANGESET> fetchChangeSetMethodsSorted(Class<?> type) throws MongockException {
    List<CHANGESET> changeSets = getChangeSetWithCompanionMethods(asList(type.getDeclaredMethods()));
    changeSets.sort(new ChangeSetComparator());
    return changeSets;
  }


  private List<CHANGESET> getChangeSetWithCompanionMethods(List<Method> allMethods) throws MongockException {
    Set<String> changeSetIdsAlreadyProcessed = new HashSet<>();
    Consumer<String> addIfNotDuplicatedOrException = changeSetId -> {
      if (changeSetIdsAlreadyProcessed.contains(changeSetId)) {
        throw new MongockException(String.format("Duplicated changeset id found: '%s'", changeSetId));
      }
      changeSetIdsAlreadyProcessed.add(changeSetId);
    };
    return allMethods.stream()
        .filter(legacyAnnotationProcessor::isMethodAnnotatedAsChange)
        .map(changeSetMethod -> legacyAnnotationProcessor.getChangePerformerItem(changeSetMethod, null))
        .peek(changeSetItem -> addIfNotDuplicatedOrException.accept(changeSetItem.getId()))
        .filter(this::isChangeSetWithinSystemVersionRange)
        .collect(Collectors.toList());
  }

  //todo Create a SystemVersionChecker
  private boolean isChangeSetWithinSystemVersionRange(CHANGESET changeSetAnn) {
    String versionString = changeSetAnn.getSystemVersion();
    return isWithinVersion(versionString);
  }

  private boolean isWithinVersion(String versionString) {
    ArtifactVersion version = new DefaultArtifactVersion(versionString);
    return version.compareTo(startSystemVersion) >= 0 && version.compareTo(endSystemVersion) <= 0;
  }

  private CHANGELOG buildChangeLogObject(Class<?> changeLogClass) {
    try {
      return !changeLogClass.isAnnotationPresent(ChangeUnit.class)
          ? buildChangeLogInstanceFromLegacy(changeLogClass)
          : buildChangeLogInstance(changeLogClass);
    } catch (MongockException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new MongockException(ex);
    }
  }

  protected abstract CHANGELOG buildChangeLogInstance(Class<?> changeLogClass) throws MongockException;

  protected abstract CHANGELOG buildChangeLogInstanceFromLegacy(Class<?> changeLogClass) throws MongockException;






  protected List<CHANGESET> fetchListOfChangeSetsFromClass(Class<?> type) {
    return getAllChanges(type)
        .filter(changeSetItem -> getLegacyAnnotationProcessor().isChangeSet(changeSetItem.getMethod()))
        .collect(Collectors.toList());
  }


  private Stream<CHANGESET> getAllChanges(Class<?> type) {
    return fetchChangeSetMethodsSorted(type)
        .stream()
        .filter(changeSet -> this.profileFilter != null ? this.profileFilter.apply(changeSet.getMethod()) : true);
  }

  private class ThrowableHashSet extends HashSet<ChangeSetItem> {
    public void addAndThrow(ChangeSetItem e) {
      if (!add(e)) throw new MongockException("Change with id[%s] duplicated", e.getId());
    }
  }
}
