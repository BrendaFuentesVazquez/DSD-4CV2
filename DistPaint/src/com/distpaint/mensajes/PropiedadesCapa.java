package com.distpaint.mensajes;

import java.awt.Rectangle;
import java.io.Serializable;

public class PropiedadesCapa {

    public int version;
    public int largo;
    public int alto;
    public int origenX;
    public int origenY;
    public int objetivo;
    public String nombre;
    public PropiedadesColor colorFondo;

    public PropiedadesCapa() {
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getLargo() {
        return largo;
    }

    public void setLargo(int largo) {
        this.largo = largo;
    }

    public int getAlto() {
        return alto;
    }

    public void setAlto(int alto) {
        this.alto = alto;
    }

    public int getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(int objetivo) {
        this.objetivo = objetivo;
    }

    public PropiedadesColor getColorFondo() {
        return colorFondo;
    }

    public void setColorFondo(PropiedadesColor colorFondo) {
        this.colorFondo = colorFondo;
    }

    public void setOrigenX(int origenX) {
        this.origenX = origenX;
    }

    public void setOrigenY(int origenY) {
        this.origenY = origenY;
    }

    public int getOrigenX() {
        return origenX;
    }

    public int getOrigenY() {
        return origenY;
    }

    public Rectangle getRectangle() {
        return new Rectangle(origenX, origenY, largo, alto);
    }

    @Override
    public String toString() {
        return "PropiedadesCapa{" + "version=" + version + ", largo=" + largo + 
                ", alto=" + alto + ", origenX=" + origenX + ", origenY=" + origenY + 
                ", objetivo=" + objetivo + ", nombre=" + nombre + 
                ", colorFondo=" + colorFondo + '}';
    }

    
    
    public String getNombre() {
        if (objetivo == 0) {
            return "Fondo";
        }
        
        if (nombre == null || "".equals(nombre)) {
            return "Capa #" + objetivo;
        }
        
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

}
