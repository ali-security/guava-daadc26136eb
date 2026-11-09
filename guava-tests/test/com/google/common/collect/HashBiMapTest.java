/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link HashBiMap}.
 *
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
public class HashBiMapTest extends TestCase {

  public static final class HashBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      BiMap<String, String> result = HashBiMap.create();
      for (Entry<String, String> entry : entries) {
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }
  }

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        BiMapTestSuiteBuilder.using(new HashBiMapGenerator())
            .named("HashBiMap")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTest(
        BiMapTestSuiteBuilder.using(
                new TestStringBiMapGenerator() {
                  @Override
                  protected BiMap<String, String> create(Entry<String, String>[] entries) {
                    HashBiMap<String, String> result = HashBiMap.create();
                    result.convertToHashFloodingResistantImplementation();
                    for (Entry<String, String> entry : entries) {
                      result.put(entry.getKey(), entry.getValue());
                    }
                    return result;
                  }
                })
            .named("HashBiMap with flooding resistance")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTestSuite(HashBiMapTest.class);
    return suite;
  }

  public void testMapConstructor() {
    /* Test with non-empty Map. */
    Map<String, String> map =
        ImmutableMap.of(
            "canada", "dollar",
            "chile", "peso",
            "switzerland", "franc");
    HashBiMap<String, String> bimap = HashBiMap.create(map);
    assertEquals("dollar", bimap.get("canada"));
    assertEquals("canada", bimap.inverse().get("dollar"));
  }

  private static final int N = 1000;

  public void testBashIt() throws Exception {
    BiMap<Integer, Integer> bimap = HashBiMap.create(N);
    BiMap<Integer, Integer> inverse = bimap.inverse();

    for (int i = 0; i < N; i++) {
      assertNull(bimap.put(2 * i, 2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i + 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      int oldValue = bimap.get(2 * i);
      assertEquals(2 * i + 1, (int) bimap.put(2 * i, oldValue - 2));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i - 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i - 1));
    }
    Set<Entry<Integer, Integer>> entries = bimap.entrySet();
    for (Entry<Integer, Integer> entry : entries) {
      entry.setValue(entry.getValue() + 2 * N);
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * N + 2 * i - 1, (int) bimap.get(2 * i));
    }
  }

  private static <K extends @Nullable Object, V extends @Nullable Object>
      ImmutableList<HashBiMap<K, V>> createBiMaps() {
    HashBiMap<K, V> flooded = HashBiMap.create();
    flooded.convertToHashFloodingResistantImplementation();
    return ImmutableList.of(HashBiMap.create(), flooded);
  }

  public void testBiMapEntrySetIteratorRemove() {
    for (BiMap<Integer, String> map : HashBiMapTest.<Integer, String>createBiMaps()) {
      map.put(1, "one");
      Set<Entry<Integer, String>> entries = map.entrySet();
      Iterator<Entry<Integer, String>> iterator = entries.iterator();
      Entry<Integer, String> entry = iterator.next();
      entry.setValue("two"); // changes the iterator's current entry value
      assertThat(map.get(1)).isEqualTo("two");
      assertEquals(Integer.valueOf(1), map.inverse().get("two"));
      iterator.remove(); // removes the updated entry
      assertTrue(map.isEmpty());
    }
  }

  public void testInsertionOrder() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);
      assertThat(map.entrySet())
          .containsExactly(
              immutableEntry("foo", 1), immutableEntry("bar", 2), immutableEntry("quux", 3))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveFirst() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.remove("foo");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("bar", 2), immutableEntry("quux", 3))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveMiddle() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.remove("bar");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("foo", 1), immutableEntry("quux", 3))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveLast() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.remove("quux");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("foo", 1), immutableEntry("bar", 2))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterForcePut() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.forcePut("quux", 1);
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("bar", 2), immutableEntry("quux", 1))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(2, "bar"), immutableEntry(1, "quux"))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterInverseForcePut() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.inverse().forcePut(1, "quux");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("bar", 2), immutableEntry("quux", 1))
          .inOrder();
    }
  }

  public void testInverseInsertionOrderAfterInverse() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("bar", 2);
      map.put("quux", 1);

      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(2, "bar"), immutableEntry(1, "quux"))
          .inOrder();
    }
  }

  public void testInverseInsertionOrderAfterInverseForcePut() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.inverse().forcePut(1, "quux");
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(2, "bar"), immutableEntry(1, "quux"))
          .inOrder();
    }
  }

  public void testInverseInsertionOrderAfterInverseForcePutPresentKey() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);
      map.put("nab", 4);

      map.inverse().forcePut(4, "bar");
      assertThat(map.entrySet())
          .containsExactly(
              immutableEntry("foo", 1), immutableEntry("bar", 4), immutableEntry("quux", 3))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(
              immutableEntry(1, "foo"), immutableEntry(4, "bar"), immutableEntry(3, "quux"))
          .inOrder();
    }
  }

  public void testInverseEntrySetValueNewKey() {
    for (BiMap<Integer, String> map : HashBiMapTest.<Integer, String>createBiMaps()) {
      map.put(1, "a");
      map.put(2, "b");
      Iterator<Entry<String, Integer>> inverseEntryItr = map.inverse().entrySet().iterator();
      Entry<String, Integer> entry = inverseEntryItr.next();
      entry.setValue(3);
      assertEquals(immutableEntry("b", 2), inverseEntryItr.next());
      assertFalse(inverseEntryItr.hasNext());
      assertThat(map.entrySet())
          .containsExactly(immutableEntry(2, "b"), immutableEntry(3, "a"))
          .inOrder();
    }
  }

  public void testEntrySetSetValueTriggersFlooding() {
    HashBiMap<Integer, Object> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put(i, "orig-" + i);
    }
    assertThat(map.isUsingHashFloodingResistance()).isFalse();

    for (Iterator<Entry<Integer, Object>> it = map.entrySet().iterator(); it.hasNext(); ) {
      Entry<Integer, Object> entry = it.next();
      int key = entry.getKey();
      assertThat(entry.setValue(new ConstantHashObject(key))).isEqualTo("orig-" + key);
      if (key == 15) {
        it.remove();
        assertThrows(NullPointerException.class, () -> entry.setValue("after-remove"));
      }
    }
    assertThat(map.isUsingHashFloodingResistance()).isTrue();

    ImmutableBiMap.Builder<Integer, Object> expectedBuilder = ImmutableBiMap.builder();
    for (int i = 0; i < 20; i++) {
      if (i != 15) {
        expectedBuilder.put(i, new ConstantHashObject(i));
      }
    }
    ImmutableBiMap<Integer, Object> expected = expectedBuilder.buildOrThrow();
    assertThat(map.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
    assertThat(map.inverse().entrySet())
        .containsExactlyElementsIn(expected.inverse().entrySet())
        .inOrder();
  }

  public void testInverseEntrySetSetValueTriggersFlooding() {
    HashBiMap<Object, Integer> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put("orig-" + i, i);
    }
    assertThat(map.isUsingHashFloodingResistance()).isFalse();

    for (Iterator<Entry<Integer, Object>> it = map.inverse().entrySet().iterator();
        it.hasNext(); ) {
      Entry<Integer, Object> entry = it.next();
      int value = entry.getKey();
      assertThat(entry.setValue(new ConstantHashObject(value))).isEqualTo("orig-" + value);
    }
    assertThat(map.isUsingHashFloodingResistance()).isTrue();

    ImmutableBiMap.Builder<Object, Integer> expectedBuilder = ImmutableBiMap.builder();
    for (int i = 0; i < 20; i++) {
      expectedBuilder.put(new ConstantHashObject(i), i);
    }
    ImmutableBiMap<Object, Integer> expected = expectedBuilder.buildOrThrow();
    assertThat(map.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
    assertThat(map.inverse().entrySet())
        .containsExactlyElementsIn(expected.inverse().entrySet())
        .inOrder();
  }

  private static final class ConstantHashObject {
    final int id;

    ConstantHashObject(int id) {
      this.id = id;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof ConstantHashObject && this.id == ((ConstantHashObject) obj).id;
    }
  }
}
