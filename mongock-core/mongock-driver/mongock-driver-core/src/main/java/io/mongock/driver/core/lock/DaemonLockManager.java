package io.mongock.driver.core.lock;

import io.mongock.driver.api.lock.LockCheckException;
import io.mongock.driver.api.lock.LockManager;
import io.mongock.utils.TimeService;
import io.mongock.utils.annotation.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * <p>This class is responsible of managing the lock at high level. It provides 3 main methods which are
 * for acquiring, ensuring(doesn't acquires the lock, just refresh the expiration time if required) and
 * releasing the lock.</p>
 *
 * <p>Note that this class is not threadSafe and it's not intended to be used by multiple executors. Each of them
 * should acquire a new lock(different key).</p>
 *
 * <p>life cycle:
 * - acquire
 * - ensure x N
 * - release
 * </p>
 **/
@NotThreadSafe
public class DaemonLockManager extends Thread implements LockManager {

  //static constants
  private static final Logger logger = LoggerFactory.getLogger(DaemonLockManager.class);

  private static final String GOING_TO_SLEEP_MSG = "Mongock is going to sleep to wait for the lock:  {} ms({} minutes)";
  private static final String EXPIRATION_ARG_ERROR_MSG = "Lock expiration period must be greater than %d ms";
  private static final String MAX_TRIES_ERROR_TEMPLATE = "Quit trying lock after %s millis due to LockPersistenceException: \n\tcurrent lock:  %s\n\tnew lock: %s\n\tacquireLockQuery: %s\n\tdb error detail: %s";
  private static final String LOCK_HELD_BY_OTHER_PROCESS = "Lock held by other process. Cannot ensure lock.\n\tcurrent lock:  %s\n\tnew lock: %s\n\tacquireLockQuery: %s\n\tdb error detail: %s";

  private static final long MIN_LOCK_ACQUIRED_FOR_MILLIS = 3 * 1000L;// 3 seconds
  private static final long DEFAULT_LOCK_ACQUIRED_FOR_MILLIS = 60 * 1000L;// 1 minute

  private static final long DEFAULT_QUIT_TRY_AFTER_MILLIS = 3 * DEFAULT_LOCK_ACQUIRED_FOR_MILLIS;//3 times default min acquired

  private static final double LOCK_REFRESH_MARGIN_PERCENTAGE = 0.33;// 30%
  private static final long MIN_LOCK_REFRESH_MARGIN_MILLIS = 1000L;// 1 second
  private static final long DEFAULT_LOCK_REFRESH_MARGIN_MILLIS = (long) (DEFAULT_LOCK_ACQUIRED_FOR_MILLIS * LOCK_REFRESH_MARGIN_PERCENTAGE);

  private static final long MINIMUM_WAITING_TO_TRY_AGAIN = 500L;//Half a second
  private static final long DEFAULT_TRY_FREQUENCY_MILLIS = 1000L;// ! second
  private static final String DEFAULT_KEY = "DEFAULT_KEY";


  private final CountDownLatch releaseSemaphore = new CountDownLatch(1);

  private final boolean daemonActive;

  /**
   * Moment when will mandatory to acquire the lock again.
   */
  private volatile Date lockExpiresAt = null;

  /**
   * The instant the acquisition has shouldStopTryingAt
   */
  private volatile Instant shouldStopTryingAt;

  /**
   * flag to release the lock asap and stop ensuring the lock
   */
  private volatile boolean releaseStarted = false;


  //injections
  private final LockRepository repository;
  private final TimeService timeService;
  /**
   * Owner of the lock
   */
  private final String owner;

  /**
   * <p>The period of time for which the lock will be owned.</p>
   */
  private final long lockAcquiredForMillis;

  /**
   * <p>Milliseconds after which it will try to acquire the lock again<p/>
   */
  private final long lockTryFrequencyMillis;

  /**
   * <p>The margin in which the lock should be refresh to avoid losing it</p>
   */
  private final long lockRefreshMarginMillis;
  /**
   * <p>Maximum time it will wait for the lock in total.</p>
   */
  private final long lockQuitTryingAfterMillis;


  public static DefaultLockManagerBuilder builder() {
    return new DefaultLockManagerBuilder();
  }

  /**
   * @param repository                lockRepository to persist the lock
   * @param timeService               time service
   * @param lockAcquiredForMillis     number of millis that the lock will be initially acquired for
   * @param lockQuitTryingAfterMillis number of seconds it will try to acquire the lock with no success
   * @param lockTryFrequencyMillis    how often(in millis) it will try to acquire the lock
   * @param lockRefreshMarginMillis   margin(in millis) after which the lock need to be refreshed
   * @param owner                     owner of the lock
   */
  private DaemonLockManager(LockRepository repository,
                            TimeService timeService,
                            long lockAcquiredForMillis,
                            long lockQuitTryingAfterMillis,
                            long lockTryFrequencyMillis,
                            long lockRefreshMarginMillis,
                            String owner,
                            boolean daemonActive) {
    this.repository = repository;
    this.timeService = timeService;
    this.lockAcquiredForMillis = lockAcquiredForMillis;
    this.lockQuitTryingAfterMillis = lockQuitTryingAfterMillis;
    this.lockTryFrequencyMillis = lockTryFrequencyMillis;
    this.lockRefreshMarginMillis = lockRefreshMarginMillis;
    this.owner = owner;
    this.daemonActive = daemonActive;
    setDaemon(true);
  }


  @Override
  public String getDefaultKey() {
    return DEFAULT_KEY;
  }

  @Override
  public void acquireLockDefault() throws LockCheckException {
    initialize();
    boolean keepLooping = true;
    do {
      try {
        logger.info("Mongock trying to acquire the lock");
        Date newLockExpiresAt = timeService.currentDatePlusMillis(lockAcquiredForMillis);
        repository.insertUpdate(new LockEntry(getDefaultKey(), LockStatus.LOCK_HELD.name(), owner, newLockExpiresAt));
        logger.info("Mongock acquired the lock until: {}", newLockExpiresAt);
        updateStatus(newLockExpiresAt);
        keepLooping = false;
        startThreadIfApplies();
      } catch (LockPersistenceException ex) {
        handleLockException(true, ex);
      }
    } while (keepLooping);
  }

  @Override
  public void ensureLockDefault() throws LockCheckException {
    if (releaseStarted) {
      throw new LockCheckException("Lock cannot be ensured after being cancelled");
    }
    ensureLockInternal();
  }

  /**
   * It will loop to ensure the lock, until it's cancelled .
   * It's the only place the lock is directly released. This ensure the race condition which tries to ensure the lock
   * after being released, as the order it's ensured: Although it can request the lock ensure after the flag cancelled
   * is active, it will eventually release the lock after it and won't ensure it afterward.
   * <p>
   * Be aware that the other external place where the lock is ensure/acquire will throw an exception after the flag
   * cancelled is active.
   */
  @Override
  public void run() {
    logger.info("Starting mongock lock daemon...");
    while (!releaseStarted) {
      try {
        logger.debug("Mongock lock daemon ensuring lock");
        ensureLockInternal();
        reposeIfRequired();
      } catch (LockCheckException e) {
        logger.warn("Error ensuring the lock from daemon: {}", e.getMessage());
      } catch (Exception e) {
        logger.warn("Generic error from daemon: {}", e.getMessage());
      }
    }
    if (releaseStarted) {
      logger.info("Cancelling mongock lock daemon: Releasing lock(if taken)...");
      releaseSemaphore.countDown();
    }
    logger.info("Cancelled mongock lock daemon");
  }

  /**
   * This process aims to ensure the release is performed ensuring the acquire/ensure won't be executed again.
   *
   * As mentioned in the class's description this class is not thread safe so, except from the internal daemon, this
   * method is assumed not to be called in parallel with acquire/ensure.
   *
   * To resolve the the parallelism with the internal daemon, we only block the release, as it's executed once at the end
   * of the process. The rest of the methods are non-blocking.
   *
   * Process:
   * - It makes the flag cancelled to true, making the run method aware it should be stop asap, counting down the latch
   * - It waits a considerable time to allow the run method to count down the latch
   * - When the latch is down, it releases the lock
   * - If the time elapses before the count down is reached, meaning the daemon could potentially try to ensure the lock
   * after it is released and produce a race condition, we opt for the pessimistic solution: Leave the lock as it is
   * in the database and make the other processes to wait until is expired.
   */
  @Override
  public void releaseLockDefault() {
    logger.info("Cancelling mongock lock daemon...");
    releaseStarted = true;
    boolean countReached = false;
    if (daemonActive) {
      try {
        long daemonTimeForResting = getDaemonTimeForResting();
        long timeout = daemonTimeForResting > 0 ? daemonTimeForResting : lockAcquiredForMillis;
        //todo change back to debug
        logger.info("Lock waiting {}ms for the daemon to count down the latch", timeout);
        countReached = releaseSemaphore.await(timeout, TimeUnit.MILLISECONDS);
        releaseLockInternal();
      } catch (InterruptedException e) {
        logger.warn("Error in lock release semaphore", e);
      }
      if(!countReached) {
        logger.warn("Lock wasn't released due to waiting time elapsed before the count reached zero");
      }
    } else {
      releaseLockInternal();
    }
  }


  @Override
  public void close() {
    releaseLockDefault();
  }

  @Override
  public boolean isReleaseStarted() {
    return releaseStarted;
  }

  private void releaseLockInternal() {
    try {
      logger.info("Mongock releasing the lock");
      repository.removeByKeyAndOwner(getDefaultKey(), this.getOwner());
      lockExpiresAt = null;
      shouldStopTryingAt = Instant.now();//this makes the ensureLock to fail
      logger.info("Mongock released the lock");
    } catch (Exception ex) {
      logger.warn("Error removing the lock from database", ex);
    }

  }

  @Override
  public long getLockTryFrequency() {
    return lockTryFrequencyMillis;
  }

  @Override
  public String getOwner() {
    return owner;
  }

  @Override
  public boolean isLockHeld() {
    return lockExpiresAt != null && timeService.currentTime().compareTo(lockExpiresAt) < 1;
  }

  private void ensureLockInternal() {
    boolean keepLooping = true;
    do {
      if (needsRefreshLock()) {
        try {
          logger.info("Mongock trying to refresh the lock");
          Date lockExpiresAtTemp = timeService.currentDatePlusMillis(lockAcquiredForMillis);
          LockEntry lockEntry = new LockEntry(getDefaultKey(), LockStatus.LOCK_HELD.name(), owner, lockExpiresAtTemp);
          repository.updateIfSameOwner(lockEntry);
          updateStatus(lockExpiresAtTemp);
          logger.info("Mongock refreshed the lock until: {}", lockExpiresAtTemp);
          keepLooping = false;
        } catch (LockPersistenceException ex) {
          handleLockException(false, ex);
        }
      } else {
        keepLooping = false;
      }
    } while (keepLooping);
  }

  private void reposeIfRequired() {
    try {
      long reposingTime = getDaemonTimeForResting();
      logger.info("Mongock lock daemon reposing time 1ms as reposing time negative: {}ms", reposingTime);
      sleep(reposingTime);

    } catch (InterruptedException ex) {
      logger.warn("Interrupted exception ignored");
    }
  }

  private long getDaemonTimeForResting() {
    long timeForResting;
    if (lockExpiresAt != null) {
      long currentTime = timeService.currentTime().getTime();
      long expiresAtTime = lockExpiresAt.getTime();
      timeForResting = expiresAtTime - currentTime - lockRefreshMarginMillis;
      //todo change back to debug
      logger.info("Mongock lock daemon initial time for resting[expiresAt: {}, currentTime: {}]: {}ms", timeForResting, expiresAtTime, currentTime);
    } else {
      timeForResting = lockAcquiredForMillis - lockRefreshMarginMillis;
      //todo change back to debug
      logger.info("Mongock lock daemon initial time for resting[lockAcquiredForMillis: {}, lockRefreshMarginMillis: {}]: {}ms", timeForResting, lockAcquiredForMillis, lockRefreshMarginMillis);
    }

    if(timeForResting <= 0) {
      //todo change back to debug
      logger.info("Mongock lock daemon reposing time 1ms as reposing time negative: {}ms", timeForResting);
      return 1L;
    } else if(timeForResting > lockAcquiredForMillis) {
      //todo change back to debug
      logger.info("Mongock lock daemon reposing time back to lockAcquiredForMillis[{}ms] as to reposing time to high: {}ms", lockAcquiredForMillis, timeForResting);
      return lockAcquiredForMillis;
    } else {
      //todo change back to debug
      logger.info("Mongock lock daemon reposing time: {}ms", timeForResting);
      return timeForResting;
    }
  }


  private void handleLockException(boolean acquiringLock, LockPersistenceException ex) {
    LockEntry currentLock = repository.findByKey(getDefaultKey());

    if (isAcquisitionTimerOver()) {
      updateStatus(null);
      throw new LockCheckException(String.format(
          MAX_TRIES_ERROR_TEMPLATE,
          lockQuitTryingAfterMillis,
          currentLock != null ? currentLock.toString() : "none",
          ex.getNewLockEntity(),
          ex.getAcquireLockQuery(),
          ex.getDbErrorDetail()));
    }

    if (isLockOwnedByOtherProcess(currentLock)) {
      Date currentLockExpiresAt = currentLock.getExpiresAt();
      logger.warn("Lock is taken by other process until: {}", currentLockExpiresAt);
      if (!acquiringLock) {
        throw new LockCheckException(String.format(
            LOCK_HELD_BY_OTHER_PROCESS,
            currentLock,
            ex.getNewLockEntity(),
            ex.getAcquireLockQuery(),
            ex.getDbErrorDetail()));
      }

      waitForLock(currentLockExpiresAt);
    }

  }

  private boolean isLockOwnedByOtherProcess(LockEntry currentLock) {
    return currentLock != null && !currentLock.isOwner(owner);
  }

  private void waitForLock(Date expiresAtMillis) {
    Date current = timeService.currentTime();
    long currentLockWillExpireInMillis = expiresAtMillis.getTime() - current.getTime();
    long sleepingMillis = lockTryFrequencyMillis;
    if (lockTryFrequencyMillis > currentLockWillExpireInMillis) {
      logger.info("The configured time frequency[{} millis] is higher than the current lock's expiration", lockTryFrequencyMillis);
      sleepingMillis = currentLockWillExpireInMillis > MINIMUM_WAITING_TO_TRY_AGAIN ? currentLockWillExpireInMillis : MINIMUM_WAITING_TO_TRY_AGAIN;
    }
    logger.info("Mongock will try to acquire the lock in {} mills", sleepingMillis);


    try {
      logger.info(GOING_TO_SLEEP_MSG, sleepingMillis, timeService.millisToMinutes(sleepingMillis));
      Thread.sleep(sleepingMillis);
    } catch (InterruptedException ex) {
      logger.error("ERROR acquiring the lock", ex);
      Thread.currentThread().interrupt();
    }
  }


  private boolean needsRefreshLock() {

    if (this.lockExpiresAt == null) {
      return true;
    }
    Date currentTime = timeService.currentTime();
    Date expirationWithMargin = new Date(this.lockExpiresAt.getTime() - lockRefreshMarginMillis);
    return currentTime.compareTo(expirationWithMargin) >= 0;
  }

  private void updateStatus(Date lockExpiresAt) {
    this.lockExpiresAt = lockExpiresAt;
    finishAcquisitionTimer();
  }

  /**
   * idempotent operation only called from builder that
   * - Starts the acquisition timer
   * - Initializes and run the lock daemon.
   */
  private synchronized void initialize() {
    if (releaseStarted) {
      throw new LockCheckException("Lock cannot be acquired after being cancelled");
    }
    if (shouldStopTryingAt == null) {
      shouldStopTryingAt = timeService.nowPlusMillis(lockQuitTryingAfterMillis);
    }
  }

  private synchronized void startThreadIfApplies() {
    if (daemonActive) {
      super.start();
    }
  }

  public synchronized void start() {
    throw new UnsupportedOperationException("Start method not supported");
  }

  /**
   * idempotent operation to start the acquisition timer
   */
  private void finishAcquisitionTimer() {
    shouldStopTryingAt = null;
  }


  private boolean isAcquisitionTimerOver() {
    return shouldStopTryingAt == null || timeService.isPast(shouldStopTryingAt);
  }


  public static class DefaultLockManagerBuilder {

    private LockRepository lockRepository;
    private long lockAcquiredForMillis = DEFAULT_LOCK_ACQUIRED_FOR_MILLIS;
    private long lockTryFrequencyMillis = DEFAULT_TRY_FREQUENCY_MILLIS;
    private long lockRefreshMarginMillis = DEFAULT_LOCK_REFRESH_MARGIN_MILLIS;
    private long lockQuitTryingAfterMillis = DEFAULT_QUIT_TRY_AFTER_MILLIS;
    private String owner = UUID.randomUUID().toString();
    private TimeService timeService;
    private boolean background = true;

    public DefaultLockManagerBuilder() {
    }

    /**
     * <p>If the flag 'waitForLog' is set, indicates the maximum time it will wait for the lock in total.</p>
     *
     * @param millis max waiting time for lock. Must be greater than 0
     * @return LockChecker object for fluent interface
     */
    public DefaultLockManagerBuilder setLockQuitTryingAfterMillis(long millis) {
      if (millis <= 0) {
        throw new IllegalArgumentException("Lock-quit-trying-after must be grater than 0 ");
      }
      lockQuitTryingAfterMillis = millis;
      return this;
    }

    /**
     * <p>Updates the maximum number of tries to acquire the lock, if the flag 'waitForLog' is set </p>
     * <p>Default 1</p>
     *
     * @param millis number of tries
     * @return LockChecker object for fluent interface
     */
    public DefaultLockManagerBuilder setLockTryFrequencyMillis(long millis) {
      if (millis < MINIMUM_WAITING_TO_TRY_AGAIN) {
        throw new IllegalArgumentException(String.format("Lock-try-frequency must be grater than %d", MINIMUM_WAITING_TO_TRY_AGAIN));
      }
      lockTryFrequencyMillis = millis;
      return this;
    }

    /**
     * <p>Indicates the number of milliseconds the lock will be acquired for</p>
     * <p>Minimum 3 seconds</p>
     *
     * @param millis milliseconds the lock will be acquired for
     * @return LockChecker object for fluent interface
     */
    public DefaultLockManagerBuilder setLockAcquiredForMillis(long millis) {
      if (millis < MIN_LOCK_ACQUIRED_FOR_MILLIS) {
        throw new IllegalArgumentException(String.format(EXPIRATION_ARG_ERROR_MSG, MIN_LOCK_ACQUIRED_FOR_MILLIS));
      }
      lockAcquiredForMillis = millis;
      long marginTemp = (long) (lockAcquiredForMillis * LOCK_REFRESH_MARGIN_PERCENTAGE);
      lockRefreshMarginMillis = Math.max(marginTemp, MIN_LOCK_REFRESH_MARGIN_MILLIS);
      return this;
    }

    public DefaultLockManagerBuilder setLockRepository(LockRepository lockRepository) {
      this.lockRepository = lockRepository;
      return this;
    }

    public DefaultLockManagerBuilder setTimeService(TimeService timeService) {
      this.timeService = timeService;
      return this;
    }

    public DefaultLockManagerBuilder setLockRefreshMarginMillis(long lockRefreshMarginMillis) {
      this.lockRefreshMarginMillis = lockRefreshMarginMillis;
      return this;
    }

    public DefaultLockManagerBuilder setOwner(String owner) {
      this.owner = owner;
      return this;
    }

    public DefaultLockManagerBuilder disableBackGround() {
      background = false;
      return this;
    }

    public DaemonLockManager build() {
      return new DaemonLockManager(
          lockRepository,
          timeService != null ? timeService : new TimeService(),
          lockAcquiredForMillis,
          lockQuitTryingAfterMillis,
          lockTryFrequencyMillis,
          lockRefreshMarginMillis,
          owner,
          background);
    }


  }
}
