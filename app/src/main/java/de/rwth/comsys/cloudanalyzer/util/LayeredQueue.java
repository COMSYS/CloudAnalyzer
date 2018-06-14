package de.rwth.comsys.cloudanalyzer.util;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

public class LayeredQueue<L extends Comparable<L>, E>
{

    private TreeMap<L, Queue<E>> map;
    private int size;

    public LayeredQueue()
    {
        size = 0;
        map = new TreeMap<>();
    }

    public E poll()
    {
        Entry<L, Queue<E>> e = map.lastEntry();
        E res = null;
        if (e != null)
        {
            res = e.getValue().poll();
            if (e.getValue().isEmpty())
                map.remove(e.getKey());
            size--;
        }
        return res;
    }

    public void add(E elem, L layer)
    {
        Queue<E> l = map.get(layer);
        if (l == null)
        {
            l = new LinkedList<>();
            map.put(layer, l);
        }
        l.add(elem);
        size++;
    }

    public int size()
    {
        return size;
    }

    public int size(L layer)
    {
        Queue<E> l = map.get(layer);
        return l == null ? 0 : l.size();
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    @Override
    public String toString()
    {
        return map.toString();
    }
}
