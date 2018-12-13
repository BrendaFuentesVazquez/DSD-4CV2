package com.distpaint.mensajes;

public class BorrarCapa {

    public long version;
    public int objetivo;
    public boolean todas;

    public BorrarCapa() {
    }

    public BorrarCapa(long version, int objetivo, boolean todas) {
        this.version = version;
        this.objetivo = objetivo;
        this.todas = todas;
    }

}
