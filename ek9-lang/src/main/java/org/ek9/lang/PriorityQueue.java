package org.ek9.lang;

import java.util.Objects;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Represents the PriorityQueue type in EK9.
 * <p>
 * A form of List that can be ordered and can also be finite. Very useful for keeping
 * a list of items in ordered form. If the size is not set then all items added to the queue will remain
 * in the queue. If no ordering is specified then the order is random when accessed.
 * </p>
 * <p>
 * Once the size is set then number of items in the list will remain below or equal to that size.
 * Once again if no ordering is specified then random elements are removed.
 * </p>
 * <p>
 * The real value of this collection is when both size and ordering are specified. Then a finite number
 * of elements are retained but in the priority order specified.
 * </p>
 * <p>
 * The main driver for this class is to be able to create an ordered list of finite size.
 * There is a general need in IT to have finite lists where the least most important is trimmed off.
 * </p>
 * <p>
 * That's what this class is all about. But I also want the resulting 'list()' to be in the
 * appropriate order when viewing and processing. So the PriorityQueue is a sort of
 * 'transient' working, ordered, finite set. When you want to actually process the items in the
 * list then extract them via 'list()' or 'iterator()'.
 * </p>
 * <p>
 * The same values can be added multiple times and these will be kept in priority order.
 * So, if you remove one of those items, the rest (with the same value will remain).
 * </p>
 * <p>
 * See <a href="https://www.ek9.io/collectionTypes.html#priorityQueue">EK9 PriorityQueue</a>.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    PriorityQueue of type T as open""")
public class PriorityQueue extends BuiltinType {

  // Internal Java priority queue for storage
  java.util.PriorityQueue<Any> state;

  // Optional comparator for custom ordering
  //provides a default comparator to remove dependency on
  //Ek9 type having to implement comparable interface
  //(JDK Java code throws exceptions if cannot cast to comparable!)
  Comparator comparator = new Comparator();

  // Optional size limit for finite queue
  //if unset then there is no max size.
  Integer maxSize = new Integer();

  @Ek9Constructor("""
      PriorityQueue() as pure""")
  public PriorityQueue() {
    set();
    initializeQueue();
  }

  @Ek9Constructor("""
      PriorityQueue() as pure
        -> arg0 as T""")
  public PriorityQueue(Any arg0) {
    this();
    if (canProcess(arg0)) {
      addWithSizeLimit(arg0);
    }
  }

  @Ek9Method("""
      withComparator() as pure
        -> comparator as Comparator of T
        <- rtn as PriorityQueue of T?""")
  public PriorityQueue withComparator(Comparator comparator) {
    final var rtn = _new();
    if (canProcess(comparator)) {
      rtn.comparator = comparator;
      //make sure there is a copy taken.
      rtn.maxSize._copy(this.maxSize);
      rtn.initializeQueue();

      for (Any item : this.state) {
        rtn.addWithSizeLimit(item);
      }

    }
    return rtn;
  }

  @Ek9Method("""
      withSize() as pure
        -> size as Integer
        <- rtn as PriorityQueue of T?""")
  public PriorityQueue withSize(Integer size) {
    final var rtn = _new();

    //If the size is not viable, leave size as is in rtn - as in unlimited.
    if (canProcess(size) && size.state > 0) {
      rtn.maxSize._copy(size);
    }

    rtn.comparator = this.comparator;
    rtn.initializeQueue();

    for (Any item : this.state) {
      rtn.addWithSizeLimit(item);
    }

    return rtn;
  }

  @Ek9Method("""
      list() as pure
        <- rtn as List of T?""")
  public List list() {

    //Iterator on java priority queue is not ordered.
    //You have to do a destructive read, so copy and then use.
    java.util.PriorityQueue<Any> extraction = new java.util.PriorityQueue<>(getJavaComparator());

    final var result = new List();
    //Make a copy of it. Then use poll to extract in order.
    extraction.addAll(state);
    final var numToExtract = extraction.size();

    for (int i = 0; i < numToExtract; i++) {
      result._addAss(extraction.poll());
    }

    //Note, I like the priority queue to come out in highest priority first.
    //This makes most sense to me.
    return result.reverse();
  }

  @Ek9Method("""
      iterator() as pure
        <- rtn as Iterator of T?""")
  public Iterator iterator() {
    return list().iterator();
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof PriorityQueue asPriorityQueue) {
      return _eq(asPriorityQueue);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as PriorityQueue of T
        <- rtn as Boolean?""")
  public Boolean _eq(PriorityQueue arg) {
    if (canProcess(arg)) {
      //While quite expensive this seems to be the only way to compare java priority queues.
      final var l1 = list();
      final var l2 = arg.list();
      if (l1.isSet && l2.isSet) {
        final var result = Objects.equals(l1.state, l2.state);
        return Boolean._of(result);
      }
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as PriorityQueue of T
        <- rtn as Boolean?""")
  public Boolean _neq(PriorityQueue arg) {
    if (canProcess(arg)) {
      return _eq(arg)._negate();
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as T
        <- rtn as PriorityQueue of T?""")
  public PriorityQueue _add(Any arg) {
    final var rtn = _new();
    rtn.copySettings(this);
    rtn.initializeQueue();

    rtn._addAss(this);
    rtn._addAss(arg);

    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as List of T
        <- rtn as PriorityQueue of T?""")
  public PriorityQueue _add(List arg) {
    final var rtn = _new();
    rtn.copySettings(this);
    rtn.initializeQueue();

    rtn._addAss(this);
    rtn._addAss(arg);

    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as PriorityQueue of T
        <- rtn as PriorityQueue of T?""")
  public PriorityQueue _add(PriorityQueue arg) {
    final var rtn = _new();
    rtn.copySettings(this);
    rtn.initializeQueue();

    rtn._addAss(this);
    rtn._addAss(arg);

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as T
        <- rtn as PriorityQueue of T?""")
  public PriorityQueue _sub(Any arg) {
    final var rtn = _new();
    rtn.copySettings(this);
    rtn.initializeQueue();

    rtn._addAss(this);
    rtn._subAss(arg);

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as List of T
        <- rtn as PriorityQueue of T?""")
  public PriorityQueue _sub(List arg) {
    final var rtn = _new();
    rtn.copySettings(this);
    rtn.initializeQueue();

    rtn._addAss(this);
    rtn._subAss(arg);

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as PriorityQueue of T
        <- rtn as PriorityQueue of T?""")
  public PriorityQueue _sub(PriorityQueue arg) {
    final var rtn = _new();
    rtn.copySettings(this);
    rtn.initializeQueue();

    rtn._addAss(this);
    rtn._subAss(arg);

    return rtn;
  }

  @Ek9Operator("""
      operator +=
        -> arg as T""")
  public void _addAss(Any arg) {
    if (canProcess(arg)) {
      addWithSizeLimit(arg);
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as List of T""")
  public void _addAss(List arg) {
    if (canProcess(arg)) {
      for (Any item : arg.state) {
        addWithSizeLimit(item);
      }
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as PriorityQueue of T""")
  public void _addAss(PriorityQueue arg) {
    if (canProcess(arg)) {
      for (Any item : arg.state) {
        addWithSizeLimit(item);
      }
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as T""")
  public void _subAss(Any arg) {
    if (canProcess(arg)) {
      removeIfPresent(arg);
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as List of T""")
  public void _subAss(List arg) {
    if (canProcess(arg)) {
      for (Any item : arg.state) {
        removeIfPresent(item);
      }
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as PriorityQueue of T""")
  public void _subAss(PriorityQueue arg) {
    //Just in case user tries to remove self from this
    //pull out a list copy and then remove each item.
    if (canProcess(arg)) {
      for (Any item : arg.list().state) {
        removeIfPresent(item);
      }
    }
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    return list()._json();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    return list()._string();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    return Integer._of(Objects.hashCode(list().state));
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    return Boolean._of(state.isEmpty());
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {

    return Integer._of(state.size());

  }

  @Ek9Operator("""
      operator contains as pure
        -> arg as T
        <- rtn as Boolean?""")
  public Boolean _contains(Any arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.contains(arg));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as PriorityQueue of T""")
  public void _merge(PriorityQueue arg) {
    if (canProcess(arg)) {
      // Merge elements from arg into this queue
      for (Any item : arg.state) {
        addWithSizeLimit(item);
      }
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as PriorityQueue of T""")
  public void _replace(PriorityQueue arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as PriorityQueue of T""")
  public void _copy(PriorityQueue arg) {
    if (canProcess(arg)) {
      copySettings(arg);
      initializeQueue();
      for (Any item : arg.state) {
        addWithSizeLimit(item);
      }
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as T""")
  public void _pipe(Any arg) {
    if (arg != null) {
      addWithSizeLimit(arg);
    }
  }

  // Start of utility methods

  @Override
  protected PriorityQueue _new() {
    return new PriorityQueue();
  }

  private void initializeQueue() {
    state = new java.util.PriorityQueue<>(getJavaComparator());
  }

  private java.util.Comparator<Any> getJavaComparator() {
    return (a, b) -> (int) comparator._call(a, b).state;
  }

  private void copySettings(PriorityQueue source) {
    this.comparator = source.comparator;
    this.maxSize._copy(source.maxSize);
  }

  private void removeIfPresent(Any item) {
    if (!canProcess(item)) {
      return;
    }
    state.remove(item);
  }

  private void addWithSizeLimit(Any item) {

    if (!canProcess(item)) {
      return;
    }

    //Always add, but then check size and remove lowest if needs be.
    state.offer(item);
    if (maxSize.isSet && state.size() > maxSize.state) {
      state.remove();
    }
  }

  // Factory methods
  public static PriorityQueue _of() {
    return new PriorityQueue();
  }

  public static PriorityQueue _of(Any item) {
    return new PriorityQueue(item);
  }

  public static PriorityQueue _of(java.util.Collection<Any> items) {
    final var queue = new PriorityQueue();
    if (items != null) {
      for (Any item : items) {
        queue.addWithSizeLimit(item);
      }
    }
    return queue;
  }
}