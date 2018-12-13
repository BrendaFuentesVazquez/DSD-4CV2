package com.distpaint.mensajes;

import java.awt.Color;

public class PropiedadesColor {

    public int alfa;
    public int rojo;
    public int verde;
    public int azul;

    public PropiedadesColor() {
    }

    public PropiedadesColor(int alfa, int rojo, int verde, int azul) {
        this.alfa = alfa;
        this.rojo = rojo;
        this.verde = verde;
        this.azul = azul;
    }

    @Override
    public String toString() {
        return String.format("Color(a=%d r=%d g=%d b=%d)",
                this.alfa, this.rojo, this.verde, this.azul);
    }

    public Color getColor() {
        return new Color(this.rojo, this.verde, this.azul, this.alfa);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.alfa;
        hash = 71 * hash + this.rojo;
        hash = 71 * hash + this.verde;
        hash = 71 * hash + this.azul;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PropiedadesColor other = (PropiedadesColor) obj;
        if (this.alfa != other.alfa) {
            return false;
        }
        if (this.rojo != other.rojo) {
            return false;
        }
        if (this.verde != other.verde) {
            return false;
        }
        if (this.azul != other.azul) {
            return false;
        }
        return true;
    }

    public static final PropiedadesColor COLOR_TRANSPARENTE = new PropiedadesColor(0, 0, 0, 0);
    public static final PropiedadesColor COLOR_BLANCO = new PropiedadesColor(255, 255, 255, 255);
    public static final PropiedadesColor COLOR_NEGRO = new PropiedadesColor(255, 0, 0, 0);
}
