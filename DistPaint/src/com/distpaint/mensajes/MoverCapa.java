package com.distpaint.mensajes;

public class MoverCapa {

    public long version;
    public int objetivo;
    public int posicionX;
    public int posicionY;

    public MoverCapa() {
    }

    public MoverCapa(long version, int objetivo, int posicionX, int posicionY) {
        this.version = version;
        this.objetivo = objetivo;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
    }

}
