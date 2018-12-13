package com.distpaint.mensajes;

import com.esotericsoftware.kryo.Kryo;

public class Registrador {

    public static void registrarClases(Kryo kryo) {
        kryo.register(ActividadUsuario.class);
        kryo.register(PropiedadesCapa.class);
        kryo.register(PropiedadesColor.class);
        kryo.register(FragmentoBuffer.class);
        kryo.register(MoverCapa.class);
        kryo.register(BorrarCapa.class);
        kryo.register(IntercambiarCapa.class);
        kryo.register(RedimensionarCapa.class);
        kryo.register(int[].class);
        kryo.register(MensajeTexto.class);
    }
}

