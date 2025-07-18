package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class MutexLockTest extends Common {

  @Test
  void testConstructionButRemainsUnset() {
    assertUnset(new MutexLock());
    assertUnset(new MutexLock(null));
    assertUnset(MutexLock._of());
  }

  private void assertUnset(MutexLock mutexLock) {
    assertNotNull(mutexLock);
    assertUnset.accept(mutexLock);
    assertTrue.accept(mutexLock._empty());
    assertUnset.accept(mutexLock._string());
    assertUnset.accept(mutexLock._hashcode());
  }

  @Test
  void testUnsetItemConstruction() {
    //It is possible to hold unset items in a MutexLock - because you may wish to set them later.
    assertSet(new MutexLock(new String()), false);
    assertSet(MutexLock._of(new String()), false);
  }

  @Test
  void testSetItemConstruction() {
    assertSet(new MutexLock(String._of("Steve")), true);
    assertSet(MutexLock._of(String._of("Stephen")), true);
  }

  private void assertSet(MutexLock mutexLock, boolean expectItemToBeSet) {
    assertNotNull(mutexLock);
    assertSet.accept(mutexLock);
    //Which the item in the MutexLock might be unset, the MutexLock is actually set because it holds the value.
    assertFalse.accept(mutexLock._empty());
    if (expectItemToBeSet) {
      assertSet.accept(mutexLock._string());
      assertSet.accept(mutexLock._hashcode());
    } else {
      assertUnset.accept(mutexLock._string());
      assertUnset.accept(mutexLock._hashcode());
    }
  }

  @Test
  void testEquality() {
    //NEVER do this, add the same object to multiple MutexLocks.
    final var toBeLocked = String._of("Steve");
    final var mutexLock1 = new MutexLock(toBeLocked);
    final var mutexLock2 = MutexLock._of(toBeLocked);

    assertEquals(mutexLock1, mutexLock1);
    assertEquals(mutexLock2, mutexLock2);

    //Now while the item in the two mutex locks is the same
    assertNotEquals(mutexLock1, mutexLock2);
    assertNotEquals(mutexLock2, mutexLock1);

    assertUnset.accept(mutexLock1._eq(String._of("Steve")));
    assertUnset.accept(mutexLock1._eq(new MutexLock()));

    assertEquals(Integer._of(0), mutexLock1._cmp(mutexLock1));
    assertUnset.accept(mutexLock1._cmp(String._of("Steve")));
    assertUnset.accept(mutexLock1._cmp(new MutexLock()));
  }

  @Test
  void testNullKeyEntryAttempt() {
    final var mutexLock = new MutexLock(String._of("Steve"));
    assertNotNull(mutexLock);

    assertUnset.accept(mutexLock.enter(null));
    assertUnset.accept(mutexLock.tryEnter(null));
  }

  @Test
  void testUnsetMutexLockEntryAttempt() {
    final var mutexLock = new MutexLock();
    assertNotNull(mutexLock);

    assertUnset.accept(mutexLock.enter(new MutexKey() {
    }));
    assertUnset.accept(mutexLock.tryEnter(new MutexKey() {
    }));

  }

  @Test
  void testMutexKeyThrowsException() {

    final var lockedItem = String._of("Steve");
    final var mutexKey = new MutexKey() {
      @Override
      public void _call(final Any t) {
        throw new Exception(String._of("A test from withing the Mutex Key"));
      }
    };
    assertNotNull(mutexKey);

    //Now set up the mutex lock and access it with the key.
    final var mutexLock = new MutexLock(lockedItem);
    //Check that the call to mutex key was successful, in this case an exception was encountered.
    //As I don't really want to encourage the use of exceptions - just false is returned.
    assertFalse.accept(mutexLock.enter(mutexKey));
    assertFalse.accept(mutexLock.tryEnter(mutexKey));
  }

  @Test
  void testEntry() {
    //This is the item that will be added to the mutexLock, also we need to check this is the item we get access to,
    final var lockedItem = String._of("Steve");

    //Variable to check if access was actually triggered.
    final var accessed = new AtomicBoolean(false);

    //The key we will use to access the mutexLock.
    //In you implementation here we check the value passed in is correct and also record the fact it was called.
    final var mutexKey = new MutexKey() {
      @Override
      public void _call(final Any t) {
        assertEquals(lockedItem, t);
        accessed.set(true);
      }
    };

    //Now set up the mutex lock and access it with the key.
    final var mutexLock = new MutexLock(lockedItem);
    //Check this reports that it did call the mutex key.
    assertTrue.accept(mutexLock.enter(mutexKey));

    //Now assert access was actually triggered.
    assertTrue(accessed.get());

    //Now use the tryEntry - with no blocking Thread.
    //First just reset accessed. - maybe should be a separate test, but would be a bit bloated.
    accessed.set(false);
    assertFalse(accessed.get());

    //Again check it reports it did access the key.
    assertTrue.accept(mutexLock.tryEnter(mutexKey));
    //Now assert access was actually triggered.
    assertTrue(accessed.get());

  }

  @Test
  void testTryEnterWithLockContention() throws InterruptedException {
    //This test validates the critical missing scenario: tryEnter() when lock is held by another thread
    //It tests the return Boolean._of(false) path on line 92 of MutexLock.java that was never executed
    
    final var lockedItem = String._of("Steve");
    final var mutexLock = new MutexLock(lockedItem);
    
    //Thread synchronization
    final var threadAReady = new CountDownLatch(1);
    final var threadAHoldsLock = new CountDownLatch(1);
    final var threadBCanProceed = new CountDownLatch(1);
    
    //Result tracking
    final var threadASuccess = new AtomicBoolean(false);
    final var threadBResult = new AtomicBoolean(true); // Start as true, should become false
    final var threadBKeyCalled = new AtomicBoolean(false);
    
    //Thread A: Will hold the lock for a controlled duration
    final var threadAKey = new MutexKey() {
      @Override
      public void _call(final Any t) {
        assertEquals(lockedItem, t);
        threadAHoldsLock.countDown(); // Signal that Thread A now holds the lock
        try {
          // Wait for Thread B to attempt tryEnter
          assertTrue(threadBCanProceed.await(5, TimeUnit.SECONDS));
          // Hold the lock briefly to ensure Thread B encounters contention
          Thread.sleep(100);
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
        }
      }
    };
    
    //Thread B: Will attempt tryEnter while Thread A holds the lock  
    final var threadBKey = new MutexKey() {
      @Override
      public void _call(final Any t) {
        threadBKeyCalled.set(true); // This should NEVER be called under contention
      }
    };
    
    //Start Thread A (will block on enter)
    final var threadA = new Thread(() -> {
      threadAReady.countDown();
      final var result = mutexLock.enter(threadAKey);
      threadASuccess.set(result != null && result.isSet && result.state);
    });
    
    //Start Thread B (will attempt tryEnter while Thread A holds lock)
    final var threadB = new Thread(() -> {
      try {
        // Wait for Thread A to be ready
        assertTrue(threadAReady.await(5, TimeUnit.SECONDS));
        // Wait for Thread A to actually hold the lock
        assertTrue(threadAHoldsLock.await(5, TimeUnit.SECONDS));
        
        // Now attempt tryEnter - this should return false immediately
        final var result = mutexLock.tryEnter(threadBKey);
        threadBResult.set(result != null && result.isSet && result.state);
        
        // Signal that Thread B has completed its tryEnter attempt
        threadBCanProceed.countDown();
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
    });
    
    //Execute the test
    threadA.start();
    threadB.start();
    
    //Wait for both threads to complete
    threadA.join(10000); // 10 second timeout
    threadB.join(10000);
    
    //Verify results
    assertTrue(threadASuccess.get(), "Thread A should have successfully entered the lock");
    assertFalse(threadBResult.get(), "Thread B should have received false from tryEnter (lock contention)");
    assertFalse(threadBKeyCalled.get(), "Thread B's MutexKey should NOT have been called when lock was contended");
    
    //Verify threads completed properly
    assertFalse(threadA.isAlive(), "Thread A should have completed");
    assertFalse(threadB.isAlive(), "Thread B should have completed");
  }
}
