package com.distpaint.mensajes;

public class MensajeTexto {

    public String usuario;
    public String mensaje;

    public MensajeTexto() {
    }

    public MensajeTexto(String usuario, String mensaje) {
        this.usuario = usuario;
        this.mensaje = mensaje;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", usuario, mensaje);
    }
}
