/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.profilers.memory.adapters;

import com.android.tools.adtui.model.Range;
import com.android.tools.perflib.heap.Instance;
import com.android.tools.perflib.heap.Type;
import com.android.tools.profiler.proto.MemoryProfiler.HeapDumpInfo;
import com.android.tools.profiler.proto.MemoryServiceGrpc;
import com.android.tools.profilers.*;
import com.android.tools.profilers.memory.FakeMemoryService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.android.tools.profilers.memory.adapters.ValueObject.ValueType.*;
import static org.junit.Assert.assertEquals;

public class HeapDumpInstanceObjectTest {
  private static final String MOCK_CLASS = "MockClass";

  @Rule public final FakeGrpcChannel myGrpcChannel = new FakeGrpcChannel("MemoryNavigationTestGrpc", new FakeMemoryService());

  private FakeHeapDumpCaptureObject myCaptureObject;
  private StudioProfilers myProfilers;

  @Before
  public void setup() {
    FakeIdeProfilerServices profilerServices = new FakeIdeProfilerServices();
    myProfilers = new StudioProfilers(myGrpcChannel.getClient(), profilerServices);
    myCaptureObject = new FakeHeapDumpCaptureObject(myProfilers.getClient().getMemoryClient());
  }

  @After
  public void tearDown() throws Exception {
    myProfilers.stop();
  }

  /**
   * Tests that FieldObjects are generated correctly based on a Hprof ClassInstance object.
   */
  @Test
  public void testExtractFieldsWithClassInstance() throws Exception {
    MockClassInstance classInstance = new MockClassInstance(-1, 0, "MockClass2");
    MockClassObj classObj = new MockClassObj(-1, "MockClass3", 0);
    MockClassInstance stringInstance = new MockClassInstance(-1, 0, "java.lang.String");

    MockClassInstance targetInstance = new MockClassInstance(-1, 0, "MockClass1");
    targetInstance.addFieldValue(Type.OBJECT, "objectTest", classInstance);
    targetInstance.addFieldValue(Type.BOOLEAN, "boolTest", true);
    targetInstance.addFieldValue(Type.CHAR, "charTest", 'a');
    targetInstance.addFieldValue(Type.FLOAT, "floatTest", new Float(1f));
    targetInstance.addFieldValue(Type.DOUBLE, "doubleTest", new Double(2.0));
    targetInstance.addFieldValue(Type.BYTE, "byteTest", new Byte((byte)1));
    targetInstance.addFieldValue(Type.SHORT, "shortTest", new Short((short)3));
    targetInstance.addFieldValue(Type.INT, "intTest", new Integer(4));
    targetInstance.addFieldValue(Type.LONG, "longTest", new Integer(5));
    targetInstance.addFieldValue(Type.OBJECT, "classTest", classObj);
    targetInstance.addFieldValue(Type.OBJECT, "stringTest", stringInstance);
    targetInstance.addFieldValue(Type.OBJECT, "nullTest", null);

    myCaptureObject.addInstance(classInstance, new HeapDumpInstanceObject(
      myCaptureObject, null, classInstance, myCaptureObject.getClassDb().registerClass(0, "MockClass2"), OBJECT));
    myCaptureObject.addInstance(classObj, new HeapDumpInstanceObject(
      myCaptureObject, null, classObj, myCaptureObject.getClassDb().registerClass(0, "MockClass3"), CLASS));
    myCaptureObject.addInstance(stringInstance, new HeapDumpInstanceObject(
      myCaptureObject, null, stringInstance, myCaptureObject.getClassDb().registerClass(0, "java.lang.String"), STRING));
    myCaptureObject.addInstance(targetInstance, new HeapDumpInstanceObject(
      myCaptureObject, null, targetInstance, myCaptureObject.getClassDb().registerClass(0, "MockClass1"), OBJECT));

    List<FieldObject> fields = myCaptureObject.getInstance(targetInstance).getFields();
    assertEquals(12, fields.size());
    assertEquals("objectTest", fields.get(0).getFieldName());
    assertEquals(OBJECT, fields.get(0).getValueType());
    assertEquals("boolTest", fields.get(1).getFieldName());
    assertEquals(BOOLEAN, fields.get(1).getValueType());
    assertEquals("charTest", fields.get(2).getFieldName());
    assertEquals(CHAR, fields.get(2).getValueType());
    assertEquals("floatTest", fields.get(3).getFieldName());
    assertEquals(FLOAT, fields.get(3).getValueType());
    assertEquals("doubleTest", fields.get(4).getFieldName());
    assertEquals(DOUBLE, fields.get(4).getValueType());
    assertEquals("byteTest", fields.get(5).getFieldName());
    assertEquals(BYTE, fields.get(5).getValueType());
    assertEquals("shortTest", fields.get(6).getFieldName());
    assertEquals(SHORT, fields.get(6).getValueType());
    assertEquals("intTest", fields.get(7).getFieldName());
    assertEquals(INT, fields.get(7).getValueType());
    assertEquals("longTest", fields.get(8).getFieldName());
    assertEquals(LONG, fields.get(8).getValueType());
    assertEquals("classTest", fields.get(9).getFieldName());
    assertEquals(CLASS, fields.get(9).getValueType());
    assertEquals("stringTest", fields.get(10).getFieldName());
    assertEquals(STRING, fields.get(10).getValueType());
    assertEquals("nullTest", fields.get(11).getFieldName());
    assertEquals(NULL, fields.get(11).getValueType());
  }

  /**
   * Tests that FieldObjects are generated correctly based on a Hprof ArrayInstance object.
   */
  @Test
  public void testExtractFieldsWithArrayInstance() throws Exception {
    MockClassInstance element0 = new MockClassInstance(-1, 0, MOCK_CLASS);
    MockClassInstance element1 = new MockClassInstance(-1, 0, MOCK_CLASS);
    MockClassInstance element2 = new MockClassInstance(-1, 0, MOCK_CLASS);
    MockArrayInstance arrayInstance = new MockArrayInstance(-1, Type.OBJECT, 3, 0);
    arrayInstance.setValue(0, element0);
    arrayInstance.setValue(1, element1);
    arrayInstance.setValue(2, element2);
    myCaptureObject.addInstance(element0, new HeapDumpInstanceObject(
      myCaptureObject, null, element0, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), OBJECT));
    myCaptureObject.addInstance(element1, new HeapDumpInstanceObject(
      myCaptureObject, null, element1, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), OBJECT));
    myCaptureObject.addInstance(element2, new HeapDumpInstanceObject(
      myCaptureObject, null, element2, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), OBJECT));
    myCaptureObject.addInstance(arrayInstance, new HeapDumpInstanceObject(
      myCaptureObject, null, arrayInstance, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), ARRAY));

    List<FieldObject> fields = myCaptureObject.getInstance(arrayInstance).getFields();
    assertEquals(3, fields.size());
    assertEquals("0", fields.get(0).getFieldName());
    assertEquals(OBJECT, fields.get(0).getValueType());
    assertEquals("1", fields.get(1).getFieldName());
    assertEquals(OBJECT, fields.get(1).getValueType());
    assertEquals("2", fields.get(2).getFieldName());
    assertEquals(OBJECT, fields.get(2).getValueType());
  }

  /**
   * Tests that FieldObjects are generated correctly based on a Hprof ClassObj object.
   */
  @Test
  public void testExtractFieldsWithClassObj() throws Exception {
    MockClassInstance classInstance = new MockClassInstance(-1, 0, MOCK_CLASS);

    MockClassObj classObj = new MockClassObj(-1, "testClass", 0);
    classObj.addStaticField(Type.OBJECT, "staticObj", classInstance);
    classObj.addStaticField(Type.BOOLEAN, "staticBool", true);
    classObj.addStaticField(Type.CHAR, "staticChar", 'a');
    classObj.addStaticField(Type.FLOAT, "staticFloat", new Float(1f));
    classObj.addStaticField(Type.DOUBLE, "staticDouble", new Double(2.0));
    classObj.addStaticField(Type.BYTE, "staticByte", new Byte((byte)1));
    classObj.addStaticField(Type.SHORT, "staticShort", new Short((short)3));
    classObj.addStaticField(Type.INT, "staticInt", new Integer(4));
    classObj.addStaticField(Type.LONG, "staticLong", new Integer(5));

    myCaptureObject.addInstance(classInstance, new HeapDumpInstanceObject(
      myCaptureObject, null, classInstance, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), OBJECT));
    myCaptureObject.addInstance(classObj, new HeapDumpInstanceObject(
      myCaptureObject, null, classObj, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), CLASS));

    List<FieldObject> fields = myCaptureObject.getInstance(classObj).getFields();
    assertEquals(9, fields.size());
    assertEquals("staticObj", fields.get(0).getFieldName());
    assertEquals(OBJECT, fields.get(0).getValueType());
    assertEquals("staticBool", fields.get(1).getFieldName());
    assertEquals(BOOLEAN, fields.get(1).getValueType());
    assertEquals("staticChar", fields.get(2).getFieldName());
    assertEquals(CHAR, fields.get(2).getValueType());
    assertEquals("staticFloat", fields.get(3).getFieldName());
    assertEquals(FLOAT, fields.get(3).getValueType());
    assertEquals("staticDouble", fields.get(4).getFieldName());
    assertEquals(DOUBLE, fields.get(4).getValueType());
    assertEquals("staticByte", fields.get(5).getFieldName());
    assertEquals(BYTE, fields.get(5).getValueType());
    assertEquals("staticShort", fields.get(6).getFieldName());
    assertEquals(SHORT, fields.get(6).getValueType());
    assertEquals("staticInt", fields.get(7).getFieldName());
    assertEquals(INT, fields.get(7).getValueType());
    assertEquals("staticLong", fields.get(8).getFieldName());
    assertEquals(LONG, fields.get(8).getValueType());
  }

  /**
   * Tests that ReferenceObjects are generated correctly based on the hard+soft references of a hprof Instance object.
   * Note that as we cannot directly mock the {@link Instance} object, we use the MockClassInstance class here to allow us
   * to inject the hard + soft references.
   */
  @Test
  public void testExtractReferences() throws Exception {
    MockClassInstance mockInstance = new MockClassInstance(-1, 0, MOCK_CLASS);

    // Set up valid/invalid reference case
    MockClassInstance hardInstanceRef = new MockClassInstance(-1, 3, MOCK_CLASS);
    hardInstanceRef.addFieldValue(Type.OBJECT, "hardInstanceRef", mockInstance);
    hardInstanceRef.addFieldValue(Type.OBJECT, "invalidRef", new Object());

    // Set up multiple case
    MockArrayInstance hardArrayRef = new MockArrayInstance(-1, Type.OBJECT, 3, 2);
    hardArrayRef.setValue(0, new Object());
    hardArrayRef.setValue(1, mockInstance);
    hardArrayRef.setValue(2, mockInstance);

    // Set up different type case
    MockClassObj hardClassRef = new MockClassObj(-1, "hardClassRef", 1);
    hardClassRef.addStaticField(Type.OBJECT, "staticClassRef", mockInstance);
    hardClassRef.addStaticField(Type.BOOLEAN, "invalidBoolRef", false);

    // Set up soft references appear at end
    MockClassInstance softInstanceRef = new MockClassInstance(-1, 0, MOCK_CLASS);
    softInstanceRef.addFieldValue(Type.OBJECT, "softInstanceRef", mockInstance);
    softInstanceRef.addFieldValue(Type.OBJECT, "invalidRef", new Object());

    mockInstance.addHardReference(hardInstanceRef);
    mockInstance.addHardReference(hardArrayRef);
    mockInstance.addHardReference(hardClassRef);
    mockInstance.addSoftReferences(softInstanceRef);

    myCaptureObject.addInstance(hardInstanceRef, new HeapDumpInstanceObject(
      myCaptureObject, null, hardInstanceRef, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), OBJECT));
    myCaptureObject.addInstance(hardArrayRef, new HeapDumpInstanceObject(
      myCaptureObject, null, hardArrayRef, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), ARRAY));
    myCaptureObject.addInstance(hardClassRef, new HeapDumpInstanceObject(
      myCaptureObject, null, hardClassRef, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), CLASS));
    myCaptureObject.addInstance(softInstanceRef, new HeapDumpInstanceObject(
      myCaptureObject, null, softInstanceRef, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), OBJECT));
    myCaptureObject.addInstance(mockInstance, new HeapDumpInstanceObject(
      myCaptureObject, null, mockInstance, myCaptureObject.getClassDb().registerClass(0, MOCK_CLASS), OBJECT));

    // extractReference is expected to return a list of sorted hard references first
    // then sorted soft references.
    List<ReferenceObject> referrers = myCaptureObject.getInstance(mockInstance).extractReferences();
    assertEquals(4, referrers.size());
    // The first object should refer to the hardClassRef which has the shortest distance to root.
    List<String> refs = referrers.get(0).getReferenceFieldNames();
    assertEquals(1, refs.size());
    assertEquals("staticClassRef", refs.get(0));
    // The second object should refer to hardArrayRef with two indices references
    refs = referrers.get(1).getReferenceFieldNames();
    assertEquals(2, refs.size());
    assertEquals("1", refs.get(0));
    assertEquals("2", refs.get(1));
    // The third object should refer to hardInstanceRef
    refs = referrers.get(2).getReferenceFieldNames();
    assertEquals(1, refs.size());
    assertEquals("hardInstanceRef", refs.get(0));
    // The fourth object should refer to softInstanceRef
    refs = referrers.get(3).getReferenceFieldNames();
    assertEquals(1, refs.size());
    assertEquals("softInstanceRef", refs.get(0));
  }

  private static class FakeHeapDumpCaptureObject extends HeapDumpCaptureObject {
    private Map<Instance, HeapDumpInstanceObject> myInstanceObjectMap = new HashMap<>();

    public FakeHeapDumpCaptureObject(@NotNull MemoryServiceGrpc.MemoryServiceBlockingStub client) {
      super(client, null, 0, HeapDumpInfo.newBuilder().setStartTime(0).setEndTime(1).build(), null, new RelativeTimeConverter(0),
            new FakeFeatureTracker());
    }

    public void addInstance(@NotNull Instance instance, @NotNull HeapDumpInstanceObject instanceObject) {
      myInstanceObjectMap.put(instance, instanceObject);
    }

    @NotNull
    public HeapDumpInstanceObject getInstance(@NotNull Instance instance) {
      return myInstanceObjectMap.get(instance);
    }

    @Override
    public boolean load(@Nullable Range queryRange, @Nullable Executor queryJoiner) {
      return true;
    }

    @Nullable
    @Override
    public InstanceObject findInstanceObject(@NotNull Instance instance) {
      return myInstanceObjectMap.get(instance);
    }
  }
}