package com.distpaint.mensajes;

public class ActividadUsuario {

    public String usuario;
    public boolean agregar;

    public ActividadUsuario() {
    }

    public ActividadUsuario(String usuario) {
        this.usuario = usuario;
        this.agregar = true;
    }

    public ActividadUsuario(String usuario, boolean agregar) {
        this.usuario = usuario;
        this.agregar = agregar;
    }
}
