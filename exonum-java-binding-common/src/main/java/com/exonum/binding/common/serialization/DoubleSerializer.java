/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.exonum.binding.common.serialization;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

enum DoubleSerializer implements Serializer<Double> {
  INSTANCE;

  @Override
  public byte[] toBytes(Double value) {
    return ByteBuffer.allocate(Double.BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putDouble(value)
        .array();
  }

  @Override
  public Double fromBytes(byte[] serializedValue) {
    checkArgument(serializedValue.length == Double.BYTES,
        "Expected an array of size %s, but was %s", Double.BYTES, serializedValue.length);

    return ByteBuffer.wrap(serializedValue)
        .order(ByteOrder.LITTLE_ENDIAN)
        .getDouble();
  }

}
