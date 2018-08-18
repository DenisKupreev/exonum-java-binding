package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.storage.proofs.DbKeyFunnel.dbKeyFunnel;
import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.Bytes.createPrefixed;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UncheckedFlatMapProofTest {

  private static final byte[] VALUE = "testValue".getBytes();
  private static final HashFunction HASH_FUNCTION = Hashing.defaultHashFunction();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void mapProofShouldBeValid() {
    byte[] firstKey = createPrefixed(bytes(0b001101), DbKey.KEY_SIZE);
    byte[] valueKey = createPrefixed(bytes(0b011101), DbKey.KEY_SIZE);
    byte[] thirdKey = createPrefixed(bytes(0b1111101), DbKey.KEY_SIZE);

    DbKey valueDbKey = DbKey.newLeafKey(valueKey);
    MapProofEntryLeaf leaf = createLeafMapEntry(valueDbKey.getKeySlice(), VALUE);
    List<MapProofEntry> entries = Arrays.asList(
        createBranchMapEntry(firstKey),
        leaf,
        createBranchMapEntry(thirdKey)
    );
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(entries, Collections.singleton(valueKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(entries, equalTo(uncheckedFlatMapProof.getProofList()));

    CheckedMapProofEntry expectedEntry = new CheckedMapProofEntry(valueDbKey.getKeySlice(), VALUE);
    assertThat(checkedMapProof.getEntries(), equalTo(Collections.singleton(expectedEntry)));
    assertTrue(checkedMapProof.containsKey(valueDbKey.getKeySlice()));
    assertThat(VALUE, equalTo(checkedMapProof.get(valueDbKey.getKeySlice())));
  }

  @Test
  public void mapProofWithSeveralLeafsShouldBeValid() {
    byte[] firstValueKey = createPrefixed(bytes(0b1010_1100), DbKey.KEY_SIZE);
    byte[] secondValueKey = createPrefixed(bytes(0b0110_1100), DbKey.KEY_SIZE);
    byte[] thirdBranchKey = createPrefixed(bytes(0b0000_0010), DbKey.KEY_SIZE);
    byte[] fourthValueKey = createPrefixed(bytes(0b1011_0001), DbKey.KEY_SIZE);

    byte[] secondValue = "second".getBytes();
    byte[] fourthValue = "fourth".getBytes();
    List<MapProofEntry> entries = Arrays.asList(
        createLeafMapEntry(firstValueKey, VALUE),
        createLeafMapEntry(secondValueKey, secondValue),
        createBranchMapEntry(thirdBranchKey),
        createLeafMapEntry(fourthValueKey, fourthValue)
    );

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(
            entries, new HashSet<>(Arrays.asList(firstValueKey, secondValueKey, fourthValueKey)));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(entries, equalTo(uncheckedFlatMapProof.getProofList()));

    List<CheckedMapProofEntry> expectedEntriesList = Arrays.asList(
        new CheckedMapProofEntry(firstValueKey, VALUE),
        new CheckedMapProofEntry(secondValueKey, secondValue),
        new CheckedMapProofEntry(fourthValueKey, fourthValue)
    );

    Set<CheckedMapProofEntry> actualCheckedEntriesList = checkedMapProof.getEntries();

    assertThat(
        expectedEntriesList,
        containsInAnyOrder(
            actualCheckedEntriesList.toArray()));

    assertTrue(checkedMapProof.containsKey(firstValueKey));
    assertThat(VALUE, equalTo(checkedMapProof.get(firstValueKey)));
    assertThat(secondValue, equalTo(checkedMapProof.get(secondValueKey)));
    assertThat(fourthValue, equalTo(checkedMapProof.get(fourthValueKey)));

    // If a user checks for key that wasn't required, an IllegalArgumentException is thrown
    expectedException.expect(IllegalArgumentException.class);
    checkedMapProof.containsKey("not required key".getBytes());
  }

  @Test
  public void mapProofWithOneElementShouldBeValid() {
    byte[] key = createPrefixed(bytes(0b10), DbKey.KEY_SIZE);
    DbKey dbKey = DbKey.newLeafKey(key);
    HashCode expectedRootHash = HASH_FUNCTION
        .newHasher()
        .putObject(dbKey, dbKeyFunnel())
        .putObject(HASH_FUNCTION.hashBytes(VALUE), hashCodeFunnel())
        .hash();
    List<MapProofEntry> entries =
        Collections.singletonList(createLeafMapEntry(dbKey.getKeySlice(), VALUE));
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(entries, Collections.singleton(key));
    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();

    assertThat(expectedRootHash, equalTo(checkedMapProof.getRootHash()));

    CheckedMapProofEntry expectedEntry = new CheckedMapProofEntry(dbKey.getKeySlice(), VALUE);
    assertThat(checkedMapProof.getEntries(), equalTo(Collections.singleton(expectedEntry)));
    assertTrue(checkedMapProof.containsKey(dbKey.getKeySlice()));
    assertThat(VALUE, equalTo(checkedMapProof.get(dbKey.getKeySlice())));
  }

  @Test
  public void mapProofWithEqualEntriesOrderShouldBeInvalid() {
    byte[] firstKey = createPrefixed(bytes(0b001101), DbKey.KEY_SIZE);
    byte[] secondKey = createPrefixed(bytes(0b001101), DbKey.KEY_SIZE);
    List<MapProofEntry> entries = Arrays.asList(
        createBranchMapEntry(firstKey),
        createBranchMapEntry(secondKey)
    );
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(entries, Collections.emptySet());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.DUPLICATE_PATH));
  }

  @Test
  public void mapProofWithWrongEntriesOrderShouldBeInvalid() {
    byte[] firstKey = createPrefixed(bytes(0b011101), DbKey.KEY_SIZE);
    byte[] valueKey = createPrefixed(bytes(0b001101), DbKey.KEY_SIZE);
    List<MapProofEntry> entries = Arrays.asList(
        createBranchMapEntry(firstKey),
        createLeafMapEntry(valueKey, VALUE)
    );
    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(entries, Collections.singleton(valueKey));

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.INVALID_ORDER));
  }

  @Test
  public void mapProofWithoutEntriesShouldBeValid() {
    List entries = Collections.emptyList();

    UncheckedMapProof uncheckedFlatMapProof =
        new UncheckedFlatMapProof(entries, Collections.emptySet());

    CheckedMapProof checkedMapProof = uncheckedFlatMapProof.check();
    assertThat(checkedMapProof.getStatus(), equalTo(ProofStatus.CORRECT));
  }

  private static MapProofEntryBranch createBranchMapEntry(byte[] key) {
    byte[] prefixedKey = createPrefixed(key, DbKey.KEY_SIZE);
    return new MapProofEntryBranch(
        DbKey.newBranchKey(prefixedKey, key.length),
        HashCode.fromBytes(key));
  }

  private static MapProofEntryLeaf createLeafMapEntry(byte[] key, byte[] value) {
    byte[] prefixedKey = createPrefixed(key, DbKey.KEY_SIZE);
    return new MapProofEntryLeaf(
        DbKey.newLeafKey(prefixedKey),
        value,
        HASH_FUNCTION);
  }
}