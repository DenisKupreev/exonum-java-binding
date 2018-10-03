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
 */

package com.exonum.binding.common.proofs.list;

import static com.exonum.binding.common.proofs.list.ListProofUtils.generateRightLeaningProofTree;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.exonum.binding.common.hash.HashCode;
import org.junit.jupiter.api.Test;

class ListProofStructureValidatorTest {

  private static final String V1 = "v1";
  private static final String V2 = "v2";
  private static final String V3 = "v3";
  private static final String V4 = "v4";

  private static final HashCode H1 = HashCode.fromString("a1");
  private static final HashCode H2 = HashCode.fromString("a2");
  private static final HashCode H3 = HashCode.fromString("a3");

  private ListProofStructureValidator validator;

  @Test
  void visit_SingletonListProof() {
    ListProofNode root = ListProofUtils.leafOf(V1);

    validator = createListProofStructureValidator();
    root.accept(validator);

    assertThat(validator.check(), is(ListProofStatus.VALID));
  }

  @Test
  void visit_FullProof2elements() {
    ListProofElement left = ListProofUtils.leafOf(V1);
    ListProofElement right = ListProofUtils.leafOf(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.VALID));
  }

  @Test
  void visit_FullProof4elements() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(
            ListProofUtils.leafOf(V1),
            ListProofUtils.leafOf(V2)
        ),
        new ListProofBranch(
            ListProofUtils.leafOf(V3),
            ListProofUtils.leafOf(V4)
        )
    );

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.VALID));
  }

  @Test
  void visit_IllegalProofOfSingletonTree() {
    ListProofElement left = ListProofUtils.leafOf(V1);

    // A proof for a list of size 1 must not contain branch nodes.
    ListProofBranch root = new ListProofBranch(left, null);

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.VALID));
  }

  @Test
  void visit_ProofLeftValue() {
    ListProofNode left = ListProofUtils.leafOf(V1);
    ListProofNode right = new ListProofHashNode(H2);
    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.VALID));
  }

  @Test
  void visit_ProofRightValue() {
    ListProofNode left = new ListProofHashNode(H1);
    ListProofNode right = ListProofUtils.leafOf(V2);
    ListProofBranch root = new ListProofBranch(left, right);

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.VALID));
  }

  @Test
  void visit_InvalidTreeHasNoElements() {
    ListProofNode left = new ListProofHashNode(H1);
    ListProofBranch root = new ListProofBranch(left, null);

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.INVALID_TREE_NO_ELEMENTS));
  }

  @Test
  void visit_UnbalancedInTheRightSubTree() {
    ListProofBranch root = new ListProofBranch(
        new ListProofBranch(ListProofUtils.leafOf(V1),
            new ListProofHashNode(H2)),
        ListProofUtils.leafOf(V3) // <-- A value at the wrong depth.
    );

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.INVALID_NODE_DEPTH));
  }

  @Test
  void visit_UnbalancedInTheLeftSubTree() {
    ListProofBranch root = new ListProofBranch(
        ListProofUtils.leafOf(V1), // <-- A value at the wrong depth.
        new ListProofBranch(ListProofUtils.leafOf(V2),
            new ListProofHashNode(H3))
    );

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.INVALID_NODE_DEPTH));
  }

  @Test
  void visit_UnbalancedElementNodeTooDeep() {
    int depth = ListProofStructureValidator.MAX_NODE_DEPTH + 1;
    ListProofNode root = generateRightLeaningProofTree(depth, ListProofUtils.leafOf(V1));

    validator = createListProofStructureValidator();
    root.accept(validator);

    assertThat(validator.check(), is(ListProofStatus.INVALID_ELEMENT_NODE_DEPTH));
  }

  @Test
  void visit_UnbalancedHashNodeTooDeep() {
    int depth = ListProofStructureValidator.MAX_NODE_DEPTH + 1;
    ListProofNode root = generateRightLeaningProofTree(depth, new ListProofHashNode(H2));

    validator = createListProofStructureValidator();
    root.accept(validator);

    assertThat(validator.check(), is(ListProofStatus.INVALID_HASH_NODE_DEPTH));
  }

  @Test
  void visit_UnbalancedHashNodesOnlyLeafs() {
    ListProofBranch root = new ListProofBranch(
        ListProofUtils.leafOf(V1),
        new ListProofBranch(
            new ListProofHashNode(H1), // <-- left leaf is hash node
            new ListProofHashNode(H2)  // <-- right leaf is hash node
        )
    );

    validator = createListProofStructureValidator();
    validator.visit(root);

    assertThat(validator.check(), is(ListProofStatus.INVALID_HASH_NODES_COUNT));
  }

  private ListProofStructureValidator createListProofStructureValidator() {
    return new ListProofStructureValidator();
  }

}
