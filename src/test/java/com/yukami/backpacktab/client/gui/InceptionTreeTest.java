package com.yukami.backpacktab.client.gui;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class InceptionTreeTest {
    private static final java.util.function.BiPredicate<String, String> SAME = String::equals;

    private static List<InceptionTree.Entry<String>> entries(String prefix, int... slots) {
        return java.util.Arrays.stream(slots).mapToObj(s -> new InceptionTree.Entry<>(s, prefix + s)).toList();
    }

    private static List<String> visible(InceptionTree<String> tree) {
        return tree.visibleDescendants().stream().map(InceptionTree.Node::value).collect(Collectors.toList());
    }

    @Test
    public void rootShowsItsChildren() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1, 4), SAME);
        assertEquals(List.of("c1", "c4"), visible(tree));
    }

    @Test
    public void currentChildShowsItsOwnChildrenRightAfterIt() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1, 4), SAME);
        var c1 = tree.child(tree.root(), 1);
        tree.setCurrent(c1);
        tree.setChildren(c1, entries("g", 0, 2), SAME);
        assertEquals(List.of("c1", "g0", "g2", "c4"), visible(tree));
        assertEquals(List.of(1), c1.path());
        assertEquals(List.of(1, 2), tree.child(c1, 2).path());
    }

    @Test
    public void returningToParentClosesGrandchildrenButKeepsChildren() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1, 4), SAME);
        var c1 = tree.child(tree.root(), 1);
        tree.setCurrent(c1);
        tree.setChildren(c1, entries("g", 0), SAME);

        tree.setCurrent(tree.root());
        assertEquals(List.of("c1", "c4"), visible(tree));
        assertTrue(c1.children().isEmpty());
    }

    @Test
    public void movingToASiblingClosesTheLeftChildrenSubtree() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1, 4), SAME);
        var c1 = tree.child(tree.root(), 1);
        tree.setCurrent(c1);
        tree.setChildren(c1, entries("g", 0), SAME);

        tree.setCurrent(tree.child(tree.root(), 4));
        assertEquals(List.of("c1", "c4"), visible(tree));
    }

    @Test
    public void unchangedSnapshotKeepsNodesAndReportsNoChange() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1, 4), SAME);
        var c1 = tree.child(tree.root(), 1);
        assertFalse(tree.setChildren(tree.root(), entries("c", 1, 4), SAME));
        assertSame(c1, tree.child(tree.root(), 1));
    }

    @Test
    public void removedChildDisappears() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1, 4), SAME);
        assertTrue(tree.setChildren(tree.root(), entries("c", 4), SAME));
        assertEquals(List.of("c4"), visible(tree));
    }

    @Test
    public void changedContentsReplaceTheNode() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1), SAME);
        var old = tree.child(tree.root(), 1);
        assertTrue(tree.setChildren(tree.root(), List.of(new InceptionTree.Entry<>(1, "other")), SAME));
        assertNotSame(old, tree.child(tree.root(), 1));
    }

    @Test
    public void placeholderChildIsKeptInSlotOrder() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1, 9), SAME);
        tree.addChild(tree.root(), 4, "c4");
        assertEquals(List.of("c1", "c4", "c9"), visible(tree));
    }

    @Test
    public void isCurrentComparesPaths() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 3), SAME);
        assertTrue(tree.isCurrent(List.of()));
        tree.setCurrent(tree.child(tree.root(), 3));
        assertTrue(tree.isCurrent(List.of(3)));
        assertFalse(tree.isCurrent(List.of()));
    }

    @Test
    public void recursiveNestingIsSupported() {
        InceptionTree<String> tree = new InceptionTree<>("root");
        tree.setChildren(tree.root(), entries("c", 1, 2), SAME);
        var c1 = tree.child(tree.root(), 1);
        tree.setCurrent(c1);
        tree.setChildren(c1, entries("g", 5), SAME);
        var g5 = tree.child(c1, 5);
        tree.setCurrent(g5);
        tree.setChildren(g5, entries("h", 7), SAME);
        assertEquals(List.of("c1", "g5", "h7", "c2"), visible(tree));

        tree.setCurrent(c1);
        assertEquals(List.of("c1", "g5", "c2"), visible(tree));
    }
}
