package com.yukami.backpacktab.client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Tree of the backpacks that are "open" in inception mode: a root backpack, the backpacks stored inside it, the
 * backpacks stored inside those, and so on.
 * <p>
 * Only the chain from the root to the {@link #current() current} node is expanded: the children of every node on
 * that chain are visible, the children of any other node are closed. Moving back to a parent therefore closes
 * everything below it that is not on the way to the new current node, while the parent's own children stay open.
 * Tabs are laid out depth first, so a node's children directly follow it.
 */
final class InceptionTree<T> {
    static final class Node<T> {
        private final T value;
        private final Node<T> parent;
        private final int slot;
        private List<Node<T>> children = List.of();

        private Node(T value, Node<T> parent, int slot) {
            this.value = value;
            this.parent = parent;
            this.slot = slot;
        }

        T value() {
            return value;
        }

        Node<T> parent() {
            return parent;
        }

        /** Slot of this backpack inside its parent, or -1 for the root. */
        int slot() {
            return slot;
        }

        int depth() {
            return parent == null ? 0 : parent.depth() + 1;
        }

        /** Slots to follow from the root to reach this node (empty for the root). */
        List<Integer> path() {
            List<Integer> path = new ArrayList<>();
            for (Node<T> node = this; node.parent != null; node = node.parent) {
                path.add(0, node.slot);
            }
            return List.copyOf(path);
        }

        List<Node<T>> children() {
            return children;
        }
    }

    record Entry<T>(int slot, T value) {}

    private final Node<T> root;
    private Node<T> current;

    InceptionTree(T rootValue) {
        root = new Node<>(rootValue, null, -1);
        current = root;
    }

    Node<T> root() {
        return root;
    }

    Node<T> current() {
        return current;
    }

    Node<T> child(Node<T> parent, int slot) {
        for (Node<T> child : parent.children) {
            if (child.slot == slot) return child;
        }
        return null;
    }

    /** Adds a single child (used when the current backpack is not yet known to its parent's snapshot). */
    Node<T> addChild(Node<T> parent, int slot, T value) {
        Node<T> node = new Node<>(value, parent, slot);
        List<Node<T>> children = new ArrayList<>(parent.children);
        children.add(node);
        children.sort((a, b) -> Integer.compare(a.slot, b.slot));
        parent.children = children;
        return node;
    }

    /** Makes the node current and closes the children of every node that is not on the way from the root to it. */
    void setCurrent(Node<T> node) {
        current = node;
        Set<Node<T>> chain = new HashSet<>();
        for (Node<T> n = node; n != null; n = n.parent) chain.add(n);
        prune(root, chain);
    }

    private void prune(Node<T> node, Set<Node<T>> chain) {
        if (!chain.contains(node)) {
            node.children = List.of();
            return;
        }
        node.children.forEach(child -> prune(child, chain));
    }

    /**
     * Replaces the children of a node with a fresh snapshot. Existing children (and their subtrees) are kept when
     * their slot and contents are unchanged.
     *
     * @return whether anything changed
     */
    boolean setChildren(Node<T> node, List<Entry<T>> entries, BiPredicate<T, T> sameValue) {
        List<Node<T>> updated = new ArrayList<>(entries.size());
        boolean changed = entries.size() != node.children.size();
        for (int i = 0; i < entries.size(); i++) {
            Entry<T> entry = entries.get(i);
            Node<T> existing = child(node, entry.slot());
            if (existing != null && sameValue.test(existing.value, entry.value())) {
                updated.add(existing);
                changed |= node.children.get(i) != existing;
            } else {
                updated.add(new Node<>(entry.value(), node, entry.slot()));
                changed = true;
            }
        }
        if (changed) {
            node.children = updated;
        }
        return changed;
    }

    /** All visible nodes below the root in tab order (depth first). */
    List<Node<T>> visibleDescendants() {
        Set<Node<T>> chain = new HashSet<>();
        for (Node<T> n = current; n != null; n = n.parent) chain.add(n);
        List<Node<T>> visible = new ArrayList<>();
        collect(root, chain, visible);
        return visible;
    }

    private void collect(Node<T> node, Set<Node<T>> chain, List<Node<T>> out) {
        if (!chain.contains(node)) return;
        for (Node<T> child : node.children) {
            out.add(child);
            collect(child, chain, out);
        }
    }

    boolean isCurrent(List<Integer> path) {
        return current.path().equals(path);
    }
}
