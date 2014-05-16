/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.wizard;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Used by {@link DynamicWizard}, {@link DynamicWizardPath}, and {@link DynamicWizardStep} to store their state.
 * Each state store is part of an ancestry chain of state stores. Each level of the chain has a scope associated with it.
 * The ancestry chain must be ordered in value order of scopes, that is the scope's ordinal value must increase as the chain
 * of stores is traversed upwards from child to parent. Each stored value is associated with a scope, and values are only stored
 * in a store of the same scope as the value. If a given store is asked to store a value of a different scope, it will pass the value
 * up the chain until a store of appropriate scope is found. When searching for a value, each store will first search in its own value
 * map before inquiring up the chain, so keys of a lower scope will "shadow" the same keys which have higher scopes.
 *
 * The store can have a listener associated with it which will be notified of each update of the store.
 * The update will contain the key associated with the change as well as the scope of the change.
 * The store also allows for pulling change notifications rather than these pushed notifications via the
 * getRecentUpdates() and clearRecentUpdates() functions.
 */
public class ScopedStateStore {
  // Map of the current state
  private Map<Key, Object> myState = Maps.newHashMap();
  // Set of changed key/scope pairs which have been modified since the last call to clearRecentUpdates()
  private Set<Key> myRecentlyUpdated = Sets.newHashSet();
  private Scope myScope;
  @Nullable private ScopedStoreListener myListener;
  @Nullable private ScopedStateStore myParent;

  public interface ScopedStoreListener {
    <T> void invokeUpdate(@Nullable Key<T> changedKey);
  }

  public ScopedStateStore(@NotNull Scope scope, @Nullable ScopedStateStore parent, @Nullable ScopedStoreListener listener) {
    myScope = scope;
    myListener = listener;
    if (myParent != null && myScope.isGreaterThan(myParent.myScope)) {
      throw new IllegalArgumentException("Attempted to add store of scope " + myScope.toString() +
                                         " as child of lesser scope " + myParent.myScope.toString());
    }
    myParent = parent;
  }

  /**
   * Clients of the store can use this method if they are only interested in the value and not
   * the scope of their returned value.
   * @return the value of a pair returned from a call to get.
   */
  @Nullable
  public static <T> T unwrap(Pair<T, Key<T>> scopedValue) {
    return scopedValue.first;
  }

  /**
   * Get a value from our state store attempt to cast to the given type.
   * Will first check this state for the matching value, and if
   * not found, will query the parent scope.
   * If the object returned is not-null, but cannot be cast to the required type, an exception is thrown.
   * @param key the unique id for the value to retrieve.
   * @return a pair where the first object is the requested value and the second is the scoped key of that value.
   *         will return Pair<null, null> if no value exists in the state for the given key.
   */
  @SuppressWarnings("unchecked")
  @NotNull
  public <T> Pair<T, Key<T>> get(@NotNull Key<T> key) {
    if (myScope.equals(key.scope) && myState.containsKey(key)) {
      T value = (T)myState.get(key);
      return new Pair<T, Key<T>>(value, createKey(key.name, key.expectedClass));
    } else if (myParent != null) {
      return myParent.get(key);
    } else {
      //noinspection ConstantConditions
      return new Pair<T, Key<T>>(null, null);
    }
  }

  /**
   * Store a value in the state for the given key. If the given scope matches this state's scope, it will be stored
   * in this state store. If the given scope is larger than this store's scope, it will be delegated to the parent
   * scope if possible.
   * @param key the unique id for the value to store.
   * @param value the value to store.
   * @return true iff the state changed as a result of this operation
   */
  public <T> boolean put(@NotNull Key<T> key, @Nullable T value) {
    boolean stateChanged;
    key = new Key<T>(key.name, key.scope, key.expectedClass);
    if (myScope.isGreaterThan(key.scope)) {
      throw new IllegalArgumentException("Attempted to store a value of scope " + key.scope.name() + " in greater scope of " + myScope.name());
    } else if (myScope.equals(key.scope)) {
      stateChanged = !myState.containsKey(key) || !equals(myState.get(key), value);
      myState.put(key, value);
    } else if (key.scope.isGreaterThan(myScope) && myParent != null) {
      stateChanged = myParent.put(key, value);
    } else {
      throw new IllegalArgumentException("Attempted to store a value of scope " + key.scope.toString() + " in lesser scope of "
                                          + myScope.toString() + " which does not have a parent of the proper scope");
    }
    if (stateChanged) {
      myRecentlyUpdated.add(key);
      if (myListener != null) {
        myListener.invokeUpdate(key);
      }
    }
    return stateChanged;
  }

  private static boolean equals(@Nullable Object o, @Nullable Object o2) {
    if (o == null && o2 == null) {
      return true;
    } else if (o != null) {
      return o.equals(o2);
    } else {
      return false;
    }
  }

  /**
   * Store a set of values into the store with the given scope according to the rules laid out in
   * {@link #put}
   */
  public <T> void putAll(@NotNull Map<Key<T>, T> map) {
    for (Key<T> key : map.keySet()) {
      put(key, map.get(key));
    }
  }

  /**
   * Remove the value in the state for the given key. If the given scope matches this state's scope, it will be removed
   * in this state store. If the given scope is larger than this store's scope, it will be delegated to the parent
   * scope if possible.
   * @param key the unique id for the value to store.
   * @return true iff the remove operation caused the state of this store to change (ie something was actually removed)
   */
  public <T> boolean remove(@NotNull Key<T> key) {
    boolean stateChanged;
    if (myScope.isGreaterThan(key.scope)) {
      throw new IllegalArgumentException("Attempted to remove a value of scope " + key.scope +
                                         " from greater scope of " + myScope.name());
    } else if (myScope.equals(key.scope)) {
      stateChanged = myState.containsKey(key);
      myState.remove(key);
    } else if (key.scope.isGreaterThan(myScope) && myParent != null) {
      stateChanged = myParent.remove(key);
    } else {
      throw new IllegalArgumentException("Attempted to remove a value of scope " + key.scope + " from lesser scope of "
                                         + myScope.toString() + " which does not have a parent of the proper scope");
    }
    if (stateChanged) {
      myRecentlyUpdated.add(key);
      if (myListener != null) {
        myListener.invokeUpdate(key);
      }
    }
    return stateChanged;
  }

  /**
   * @return a single map of the values "visible" from this store, that is the values contained in this store as well as
   * all values from the ancestor chain that are not overridden by values set at this scope level.
   */
  public Map<String, Object> flatten() {
    Map<String, Object> toReturn;
    if (myParent != null) {
      toReturn = myParent.flatten();
    } else {
      toReturn = Maps.newHashMapWithExpectedSize(myState.size());
    }
    for (Key key : myState.keySet()) {
      toReturn.put(key.name, myState.get(key));
    }
    return toReturn;
  }

  /**
   * Get the map of keys and scopes representing changes to the store since the last call to clearRecentUpdates()
   * @return a non-null, possibly empty map of keys to scopes
   */
  public Set<Key> getRecentUpdates() {
    return myRecentlyUpdated;
  }

  /**
   * Notify the store that the client is done with with current record of modifications and that they can be deleted.
   */
  public void clearRecentUpdates() {
    myRecentlyUpdated.clear();
  }

  /**
   * Get a key to allow storage in the state store.
   */
  public static <T> Key<T> createKey(@NotNull String name, @NotNull Scope scope, @NotNull Class<T> clazz) {
    return new Key<T>(name, scope, clazz);
  }

  /**
   * Get a key to allow storage in the state store. The created key will be scoped at the same level as this state store.
   */
  public <T> Key<T> createKey(@NotNull String name, @NotNull Class<T> clazz) {
    return createKey(name, myScope, clazz);
  }

  public static class Key<T> {
    @NotNull final public Class<T> expectedClass;
    @NotNull final public String name;
    @NotNull final public Scope scope;

    private Key(@NotNull String name, @NotNull Scope scope, @NotNull Class<T> clazz) {
      expectedClass = clazz;
      this.name = name;
      this.scope = scope;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Key key = (Key)o;

      if (!expectedClass.equals(key.expectedClass)) return false;
      if (!name.equals(key.name)) return false;
      if (scope != key.scope) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = expectedClass.hashCode();
      result = 31 * result + name.hashCode();
      result = 31 * result + scope.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Key{" +
             "expectedClass=" + expectedClass +
             ", name='" + name + '\'' +
             ", scope=" + scope +
             '}';
    }
  }

  public enum Scope {
    STEP,
    PATH,
    WIZARD;

    public boolean isGreaterThan(@Nullable Scope other) {
      if (other == null) {
        return false;
      }
      return this.ordinal() > other.ordinal();
    }
  }
}
