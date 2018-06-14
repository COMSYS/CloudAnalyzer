package de.rwth.comsys.cloudanalyzer.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class IndexedTree<K, V>
{

    private HashMap<K, IndexedTreeNode<K, V>> map;
    private IndexedTreeNode<K, V> root;
    private Comparator<IndexedTreeNode<K, V>> comparator;

    public IndexedTree(Comparator<IndexedTreeNode<K, V>> comparator)
    {
        this.map = new HashMap<>();
        this.root = null;
        this.comparator = comparator;
    }

    public IndexedTreeNode<K, V> getNode(K key)
    {
        return map.get(key);
    }

    public IndexedTreeNode<K, V> getRoot()
    {
        return root;
    }

    public IndexedTreeNode<K, V> insert(K key, V value, IndexedTreeNode<K, V> predecessor)
    {
        IndexedTreeNode<K, V> node = new IndexedTreeNode<>(key, value, predecessor);
        map.put(key, node);
        if (root == null)
        {
            root = node;
        }
        else
        {
            addSorted(predecessor.getSuccessors(), node);
        }
        return node;
    }

    public IndexedTreeNode<K, V> remove(K key)
    {
        IndexedTreeNode<K, V> node = map.remove(key);
        if (node != null)
        {
            node.getPredecessor().getSuccessors().remove(node);
            for (IndexedTreeNode<K, V> n : node.getSuccessors())
            {
                n.setPredecessor(node.getPredecessor());
                addSorted(node.getPredecessor().getSuccessors(), n);
            }
        }
        return node;
    }

    private void addSorted(List<IndexedTreeNode<K, V>> list, IndexedTreeNode<K, V> node)
    {
        if (comparator != null)
        {
            ListIterator<IndexedTreeNode<K, V>> it = list.listIterator();
            while (it.hasNext())
            {
                if (comparator.compare(node, it.next()) < 0)
                {
                    it.previous();
                    break;
                }
            }
            it.add(node);
        }
        else
        {
            list.add(node);
        }
    }

    public int getRelation(IndexedTreeNode<K, V> node1, IndexedTreeNode<K, V> node2)
    {
        if (node1.isParentOf(node2))
            return 1;
        else if (node2.isParentOf(node1))
            return -1;
        else
            return 0;
    }

}
