package com.battleship.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections; 
import java.util.List;
import java.awt.Point; 

public class Ship implements Serializable {
    private static final long serialVersionUID = 1L; 
    private int size;
    private List<Point> coordinates; 
    private List<Point> hits;       
    private boolean isSunk;

    public Ship(int size) {
        this.size = size;
        this.coordinates = new ArrayList<>();
        this.hits = new ArrayList<>();
        this.isSunk = false;
    }

    public int getSize() {
        return size;
    }

    public List<Point> getCoordinates() {
        
        return Collections.unmodifiableList(coordinates);
    }

    public void addCoordinate(int x, int y) {
        this.coordinates.add(new Point(x, y));
    }

    
    public void setCoordinates(List<Point> coordinates) {
        this.coordinates.clear();
        if (coordinates != null) {
            for (Point p : coordinates) {
                this.coordinates.add(new Point(p.x, p.y)); 
            }
        }
    }

    
    public boolean hasCoordinate(int x, int y) {
        for (Point p : coordinates) {
            if (p.x == x && p.y == y) {
                return true;
            }
        }
        return false;
    }

    
    public boolean registerHit(int x, int y) {
        Point hitPoint = new Point(x,y);
        if (hasCoordinate(x, y) && !hits.contains(hitPoint)) {
            hits.add(hitPoint);
            if (hits.size() >= size) { 
                isSunk = true;
            }
            return true; 
        }
        return false; 
    }

    public boolean isSunk() {
        return isSunk;
    }

    
    public List<Point> getHits() {
        return Collections.unmodifiableList(hits); // Listenin değiştirilemez bir görünümünü döndür
    }

    @Override
    public String toString() {
        return "Ship{size=" + size + ", coordinates=" + coordinates.size() + " points, hits=" + hits.size() + ", sunk=" + isSunk + "}";
    }
}
