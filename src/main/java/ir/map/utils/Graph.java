package ir.map.utils;

import ir.map.domain.Tuple;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Graph {
    private final Map<Long, Set<Long>> adjacencyList;

    public Graph() {
        this.adjacencyList = new HashMap<>();
    }

    public void addEdge(Long vertex1, Long vertex2) {
        if(!adjacencyList.containsKey(vertex1))
            adjacencyList.put(vertex1, new HashSet<>());
        adjacencyList.get(vertex1).add(vertex2);
        if(!adjacencyList.containsKey(vertex2))
            adjacencyList.put(vertex2, new HashSet<>());
        adjacencyList.get(vertex2).add(vertex1);
    }

    public void dfs(Long vertex, Map<Long, Boolean> visited) {
        visited.put(vertex, true);
        adjacencyList.get(vertex).forEach(otherVertex -> {
            if(visited.get(otherVertex) == null)
                dfs(otherVertex, visited);
        });
    }

    public int numberOfConnectedComponents() {
        Map<Long, Boolean> visited = new HashMap<>();
        AtomicInteger connectedComponents = new AtomicInteger();
        adjacencyList.forEach((vertex, adjacentVertices) -> {
            if(visited.get(vertex) == null) {
                dfs(vertex, visited);
                connectedComponents.getAndIncrement();
            }
        });
        return connectedComponents.get();
    }

    public List<Long> largestCommunity() {
        AtomicReference<List<Long>> utilizatorList = new AtomicReference<>(new ArrayList<>());
        AtomicInteger maxUsers = new AtomicInteger();
        Map<Long, Boolean> visited = new HashMap<>();
        adjacencyList.forEach((vertex, adjacentVertices) -> {
            if (visited.get(vertex) == null) {
                Map<Long, Boolean> oldVisited = Map.copyOf(visited);
                dfs(vertex, visited);
                AtomicInteger countUsers = new AtomicInteger();
                visited.forEach((vertex1, booleanValue) -> {
                    if(!booleanValue.equals(oldVisited.get(vertex1)))
                        countUsers.getAndIncrement();
                });
                if (countUsers.get() > maxUsers.get()) {
                    maxUsers.set(countUsers.get());
                    utilizatorList.set(new ArrayList<>());
                    visited.forEach((vertex1, booleanValue) -> {
                        if(!booleanValue.equals(oldVisited.get(vertex1)))
                            utilizatorList.get().add(vertex1);
                    });
                }
            }
        });
        return utilizatorList.get();
    }

    public List<Tuple<Long, Long>> listOfEdges() {
        List<Tuple<Long, Long>> edges = new ArrayList<>();
        adjacencyList.forEach((vertex, adjacentVertices) -> {
            adjacentVertices.forEach(otherVertex -> {
                edges.add(new Tuple<>(vertex, otherVertex));
            });
        });
        return edges;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "edges=" + listOfEdges() +
                '}';
    }
}
