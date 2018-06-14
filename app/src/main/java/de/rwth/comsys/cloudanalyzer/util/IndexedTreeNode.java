package de.rwth.comsys.cloudanalyzer.util;

import java.util.LinkedList;
import java.util.List;

public class IndexedTreeNode<K, V>
{
    private K key;
    private V value;

    private List<IndexedTreeNode<K, V>> successors;
    private IndexedTreeNode<K, V> predecessor;

    public IndexedTreeNode(K key, V value, IndexedTreeNode<K, V> predecessor)
    {
        this.key = key;
        this.value = value;
        this.predecessor = predecessor;
        this.successors = new LinkedList<>();
    }

    public K getKey()
    {
        return key;
    }

    public V getValue()
    {
        return value;
    }

    public IndexedTreeNode<K, V> getPredecessor()
    {
        return predecessor;
    }

    public void setPredecessor(IndexedTreeNode<K, V> predecessor)
    {
        this.predecessor = predecessor;
    }

    public List<IndexedTreeNode<K, V>> getSuccessors()
    {
        return successors;
    }

    public void setSuccessors(List<IndexedTreeNode<K, V>> successors)
    {
        this.successors = successors;
    }

    public boolean isParentOf(IndexedTreeNode<K, V> node)
    {
        if (node == null)
            return false;
        else
            return key.equals(node.key) || isParentOf(node.predecessor);
    }
}
