package com.distpaint.mensajes;

public class FragmentoBuffer {

    public static final int BUFFER_PRINCIPAL = 0;
    public static final int LONGITUD_FRAGMENTO = 4;

    public long version;
    public int origenX;
    public int origenY;
    public int[] pixeles;
    public int largo;
    public int alto;
    public int objetivo;

    public FragmentoBuffer() {
    }

    public FragmentoBuffer(long version, int origenX, int origenY, int[] pixeles, int largo, int alto) {
        this.version = version;
        this.origenX = origenX;
        this.origenY = origenY;
        this.pixeles = pixeles;
        this.largo = largo;
        this.alto = alto;
        this.objetivo = 0;
    }

    public FragmentoBuffer(long version, int origenX, int origenY, int[] pixeles, int largo, int alto, int objetivo) {
        this.version = version;
        this.origenX = origenX;
        this.origenY = origenY;
        this.pixeles = pixeles;
        this.largo = largo;
        this.alto = alto;
        this.objetivo = objetivo;
    }

    public boolean dirigidoAlBufferPrincipal() {
        return this.objetivo == BUFFER_PRINCIPAL;
    }
}
