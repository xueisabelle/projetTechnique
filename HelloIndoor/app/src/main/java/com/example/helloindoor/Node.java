package com.example.helloindoor;

import java.util.Comparator;

public class Node implements Comparator<com.example.helloindoor.Node> {

    public static final int NODE_COST = 1;
    public int node;
    public int cost;

    public Node()
    {
    }

    public Node(int node, int cost)
    {
        this.node = node;
        this.cost = cost;
    }

    @Override
    public int compare(com.example.helloindoor.Node node1, com.example.helloindoor.Node node2)
    {
        if (node1.cost < node2.cost)
            return -1;
        if (node1.cost > node2.cost)
            return 1;
        return 0;
    }
}
