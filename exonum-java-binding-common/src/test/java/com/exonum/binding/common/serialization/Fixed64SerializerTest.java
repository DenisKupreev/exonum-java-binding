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

import static com.exonum.binding.common.serialization.StandardSerializersTest.invalidBytesValueTest;
import static com.exonum.binding.common.serialization.StandardSerializersTest.roundTripTest;

import com.exonum.binding.test.Bytes;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class Fixed64SerializerTest {

  private Serializer<Long> serializer = Fixed64Serializer.INSTANCE;

  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE})
  void roundTrip(Long value) {
    roundTripTest(value, serializer);
  }

  @ParameterizedTest
  @MethodSource("invalidLongs")
  void deserializeInvalidValue(byte[] value) {
    invalidBytesValueTest(value, serializer);
  }

  private static Stream<byte[]> invalidLongs() {
    return Stream.of(
        Bytes.bytes(),
        Bytes.bytes((byte) 0),
        Bytes.bytes(1, 2, 3),
        Bytes.bytes(1, 2, 3, 4),
        Bytes.bytes(1, 2, 3, 4, 5, 6, 7),
        Bytes.bytes(1, 2, 3, 4, 5, 6, 7, 8, 9),
        Bytes.bytes("str")
    );
  }

}
