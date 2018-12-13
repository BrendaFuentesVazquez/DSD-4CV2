package com.distpaint;

import com.distpaint.mensajes.PropiedadesCapa;
import com.distpaint.mensajes.ActividadUsuario;
import com.distpaint.mensajes.BorrarCapa;
import com.distpaint.mensajes.PropiedadesColor;
import com.distpaint.mensajes.FragmentoBuffer;
import com.distpaint.mensajes.IntercambiarCapa;
import com.distpaint.mensajes.MensajeTexto;
import com.distpaint.mensajes.MoverCapa;
import com.distpaint.mensajes.RedimensionarCapa;
import com.distpaint.mensajes.Registrador;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Servidor extends Listener {

    private static final Logger LOG = Logger.getLogger(Servidor.class.getCanonicalName());

    public static final int LARGO_PREDETERMINADO = 300;
    public static final int ALTO_PREDETERMINADO = 350;

    private Server servidor;
    private final int puertoTCP;
    private final int puertoUDP;
    private final ConcurrentHashMap<String, Connection> usuarios;
    private final ArrayList<Capa> capas;

    public Servidor(int puertoTCP, int puertoUDP) {
        this.puertoTCP = puertoTCP;
        this.puertoUDP = puertoUDP;
        this.usuarios = new ConcurrentHashMap<>();
        this.capas = new ArrayList<>();

        int largo = LARGO_PREDETERMINADO;
        int alto = ALTO_PREDETERMINADO;

        BufferedImage image = new BufferedImage(largo, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, largo, alto);
        graphics.dispose();

        PropiedadesCapa props = new PropiedadesCapa();
        props.objetivo = 0;
        props.version = 0;
        props.largo = largo;
        props.alto = alto;
        props.origenX = 0;
        props.origenY = 0;
        props.colorFondo = PropiedadesColor.COLOR_BLANCO;

        this.capas.add(new Capa(0, props, image));
    }

    @Override
    public void disconnected(Connection connection) {
        super.disconnected(connection);
        this.usuarios.forEach((String u, Connection c) -> {
            if (c.getID() == connection.getID()) {
                this.usuarios.remove(u);
                this.servidor.sendToAllTCP(new ActividadUsuario(u, false));
            }
        });
    }

    private void publicarCambiosEnBuffer(Connection conexion, Capa capa, int objetivo) {
        int largo = capa.propiedades.largo;
        int alto = capa.propiedades.alto;

        int longitud = FragmentoBuffer.LONGITUD_FRAGMENTO;
        int totalFragmentosX = largo / longitud;
        totalFragmentosX = totalFragmentosX > 0 ? totalFragmentosX : 1;

        int totalFragmentosY = alto / longitud;
        totalFragmentosY = totalFragmentosY > 0 ? totalFragmentosY : 1;

        for (int i = 0; i < totalFragmentosX; i++) {
            for (int j = 0; j < totalFragmentosY; j++) {
                int origenFragmentoX = i * longitud;
                int origenFragmentoY = j * longitud;

                int[] pixeles = new int[longitud * longitud];
                pixeles = capa.buffer.getRGB(origenFragmentoX,
                        origenFragmentoY, longitud, longitud,
                        pixeles, 0, longitud);

                FragmentoBuffer fb = new FragmentoBuffer(++capa.version,
                        origenFragmentoX, origenFragmentoY, pixeles,
                        longitud, longitud);
                fb.objetivo = objetivo;
                if (conexion != null) {
                    conexion.sendTCP(fb);
                } else {
                    servidor.sendToAllTCP(fb);
                }
            }
        }
    }

    @Override
    public void received(Connection conexion, Object object) {
        super.received(conexion, object);
        if (object instanceof ActividadUsuario) {
            ActividadUsuario registro = (ActividadUsuario) object;
            this.usuarios.put(registro.usuario, conexion);
            this.servidor.sendToAllTCP(registro);
            LOG.info(String.format("Nuevo usuario: %s, conexion: %d, remoto: %s",
                    registro.usuario, conexion.getID(), conexion.getRemoteAddressTCP()));

            for (int i = 0; i < capas.size(); i++) {
                Capa capa = capas.get(i);
                conexion.sendTCP(capa.propiedades);
                publicarCambiosEnBuffer(conexion, capa, i);
            }
        }

        if (object instanceof MoverCapa) {
            MoverCapa movimiento = (MoverCapa) object;
            if (this.capas.size() > movimiento.objetivo) {
                Capa capa = this.capas.get(movimiento.objetivo);
                capa.propiedades.setOrigenX(movimiento.posicionX);
                capa.propiedades.setOrigenY(movimiento.posicionY);
                this.servidor.sendToAllTCP(movimiento);
            }
        }

        if (object instanceof IntercambiarCapa) {
            IntercambiarCapa intercambio = (IntercambiarCapa) object;
            if (capas.size() > intercambio.objetivoOrigen && capas.size() > intercambio.objetivoDestino) {
                Capa origen = this.capas.get(intercambio.objetivoOrigen);
                origen.propiedades.objetivo = intercambio.objetivoDestino;

                Capa destino = this.capas.get(intercambio.objetivoDestino);
                destino.propiedades.objetivo = intercambio.objetivoOrigen;

                this.capas.set(intercambio.objetivoDestino, origen);
                this.capas.set(intercambio.objetivoOrigen, destino);
                this.servidor.sendToAllTCP(intercambio);
            }
        }

        if (object instanceof PropiedadesCapa) {
            PropiedadesCapa props = (PropiedadesCapa) object;
            if (this.capas.size() > props.objetivo) {
                Capa capa = this.capas.get(props.objetivo);
                if (props.version > capa.version) {
                    try {
                        capa.semaforo.acquire();
                        capa.propiedades = props;
                        capa.version = props.version;
                        Graphics graphics = capa.buffer.getGraphics();
                        graphics.setColor(props.colorFondo.getColor());
                        graphics.fillRect(0, 0, props.largo, props.alto);
                        graphics.dispose();
                        capa.semaforo.release();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                System.out.println("Nueva capa: " + props.objetivo);
                BufferedImage buffer = new BufferedImage(props.largo, props.alto, BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = buffer.getGraphics();
                graphics.setColor(props.colorFondo.getColor());
                graphics.fillRect(0, 0, props.largo, props.alto);
                graphics.dispose();
                this.capas.add(new Capa(0, props, buffer));
            }

            LOG.log(Level.INFO, "Total de capas: {0}", this.capas.size());
            this.servidor.sendToAllTCP(props);
        }

        if (object instanceof MensajeTexto) {
            MensajeTexto mensaje = (MensajeTexto) object;
            LOG.log(Level.INFO, "Reenviando a todos: {0}", mensaje);
            this.servidor.sendToAllTCP(mensaje);
        }

        if (object instanceof FragmentoBuffer) {
            FragmentoBuffer fb = (FragmentoBuffer) object;
            if (capas.size() > fb.objetivo) {
                Capa editando = capas.get(fb.objetivo);
                editando.buffer.setRGB(fb.origenX, fb.origenY, fb.largo,
                        fb.alto, fb.pixeles, 0, fb.largo);
            }
            this.servidor.sendToAllTCP(fb);
        }

        if (object instanceof BorrarCapa) {
            BorrarCapa borrar = (BorrarCapa) object;
            if (borrar.todas) {
                capas.clear();
            } else if (capas.size() > borrar.objetivo) {
                capas.remove(borrar.objetivo);
                for (int i = 0; i < capas.size(); i++) {
                    Capa capa = capas.get(i);
                    capa.propiedades.objetivo = i;
                }
            }

            this.servidor.sendToAllTCP(borrar);
        }

        if (object instanceof RedimensionarCapa) {
            RedimensionarCapa redim = (RedimensionarCapa) object;
            if (capas.size() > redim.objetivo) {
                Capa capa = capas.get(redim.objetivo);
                Image redimImg = capa.buffer.getScaledInstance(redim.nuevoLargo, redim.nuevoAlto, Image.SCALE_FAST);
                BufferedImage redimBuffer = new BufferedImage(redim.nuevoLargo, redim.nuevoAlto, BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = redimBuffer.getGraphics();
                graphics.drawImage(redimImg, 0, 0, null);
                graphics.dispose();

                capa.buffer = redimBuffer;
                capa.propiedades.largo = redim.nuevoLargo;
                capa.propiedades.alto = redim.nuevoAlto;
                this.servidor.sendToAllTCP(redim);
            }
        }

    }

    public void escuchar() {
        try {
            servidor = new Server(65535, 65535);
            Registrador.registrarClases(servidor.getKryo());

            servidor.start();
            servidor.bind(puertoTCP, puertoUDP);
            servidor.addListener(this);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private static final class Capa {

        public int version;
        public PropiedadesCapa propiedades;
        public BufferedImage buffer;
        public final Semaphore semaforo;

        public Capa(int version, PropiedadesCapa lienzo, BufferedImage buffer) {
            this.version = version;
            this.propiedades = lienzo;
            this.buffer = buffer;
            this.semaforo = new Semaphore(1);
        }

    }

    public static void main(String[] args) {
        if (args.length > 1 && "--help".equals(args[1])) {
            System.out.println(String.format("Usage: %s [tcp port] [udp port]", args[0]));
        }

        Integer puertoTCP = args.length > 1 ? Integer.parseInt(args[1]) : 54555;
        Integer puertoUDP = args.length > 2 ? Integer.parseInt(args[2]) : 54777;
        new Servidor(puertoTCP, puertoUDP).escuchar();
    }
}
