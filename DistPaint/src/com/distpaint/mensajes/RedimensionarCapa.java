package com.distpaint.mensajes;

public class RedimensionarCapa {

    public int objetivo;
    public int nuevoLargo;
    public int nuevoAlto;

    public RedimensionarCapa() {
    }

    public RedimensionarCapa(int objetivo, int nuevoX, int nuevoY) {
        this.objetivo = objetivo;
        this.nuevoLargo = nuevoX;
        this.nuevoAlto = nuevoY;
    }

}
