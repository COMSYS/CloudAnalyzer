package de.rwth.comsys.cloudanalyzer.util;

import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class IpRangeSearchTree<N>
{

    private static Logger logger = Logger.getLogger(IpRangeSearchTree.class.getName());
    private N value;
    private IpRangeSearchTree<N> node0;
    private IpRangeSearchTree<N> node1;

    public IpRangeSearchTree()
    {
        node0 = null;
        node1 = null;
        value = null;
    }

    public N getValue()
    {
        return value;
    }

    public void setValue(N value)
    {
        this.value = value;
    }

    public void getValues(List<N> values, byte[] addr, int begin, int end)
    {
        if (value != null)
        {
            values.add(value);
        }
        if (begin >= end || (node0 == null && node1 == null))
        {
            return;
        }

        int a = begin / 8;
        if (a >= addr.length)
            return;
        byte b = addr[a];
        b = (byte) ((b & 0xFF) >>> (7 - (begin % 8)));
        IpRangeSearchTree<N> node = ((b & 0b1) == 0b1 ? node1 : node0);
        if (node != null)
        {
            node.getValues(values, addr, begin + 1, end);
        }
    }

    public N addValue(N value, byte[] addr, int begin, int end)
    {
        if (begin >= end)
        {
            if (this.value != null)
            {
                try
                {
                    logger.d("Ip range was already added: " + InetAddress.getByAddress(addr).getHostAddress() + "/" + end);
                }
                catch (UnknownHostException e)
                {
                }
            }
            N old = this.value;
            this.value = value;
            return old;
        }

        int a = begin / 8;
        if (a >= addr.length)
            throw new RuntimeException("IpRangeSearchTree: Could not insert data. Value: " + value + ", end: " + end);
        byte b = addr[a];
        b >>= 7 - (begin % 8);
        IpRangeSearchTree<N> node;
        if ((b & 0b1) == 0b1)
        {
            if (node1 == null)
            {
                node1 = new IpRangeSearchTree<>();
            }
            node = node1;
        }
        else
        {
            if (node0 == null)
            {
                node0 = new IpRangeSearchTree<>();
            }
            node = node0;
        }
        return node.addValue(value, addr, begin + 1, end);
    }
}
