package com.distpaint;

import com.distpaint.mensajes.BorrarCapa;
import com.distpaint.mensajes.PropiedadesCapa;
import com.distpaint.mensajes.FragmentoBuffer;
import com.distpaint.mensajes.IntercambiarCapa;
import com.distpaint.mensajes.MoverCapa;
import com.distpaint.mensajes.PropiedadesColor;
import com.distpaint.mensajes.RedimensionarCapa;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

public class LienzoCompartido extends JComponent implements MouseListener, MouseMotionListener {

    private int capaSeleccionada = -1;
    private final List<Capa> capas;

    public static final int HERRAMIENTA_NINGUNA = -1;
    public static final int HERRAMIENTA_SELECCIONAR = 0;
    public static final int HERRAMIENTA_LAPIZ = 1;
    public static final int HERRAMIENTA_TEXTO = 2;
    public static final int HERRAMIENTA_CIRCULO = 3;
    public static final int HERRAMIENTA_TRIANGULO = 4;
    public static final int HERRAMIENTA_RECTANGULO = 5;
    public static final int HERRAMIENTA_IMAGEN = 6;
    public static final int HERRAMIENTA_POLIGONO = 7;
    public static final int HERRAMIENTA_MOVER = 8;
    private int herramientaActual = HERRAMIENTA_NINGUNA;

    private Point puntoInicial = null;
    private Point puntoActual = null;
    private Point origenInicial = null;
    private Color colorRelleno = Color.BLACK;
    private Color colorBorde = Color.WHITE;
    private int magnitudBorde = 1;
    private boolean rellenar = true;
    private Image imagenAImportar = null;
    private String nombreImagenAImportar = "";

    private final JButton botonMover; // Pa que quede ya
    private Client cliente;
    private CapaListener eventosCapas;
    private ConcurrentHashMap<Integer, Capa> pendientesDePublicar;

    public LienzoCompartido(JButton botonMover) {
        initComponents();
        addMouseListener(this);
        addMouseMotionListener(this);

        this.capas = Collections.synchronizedList(new ArrayList<>());
        this.botonMover = botonMover;
        this.pendientesDePublicar = new ConcurrentHashMap<>();
    }

    public void setCliente(Client cliente) {
        this.cliente = cliente;
        this.cliente.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof BorrarCapa) {
                    BorrarCapa borrar = (BorrarCapa) object;
                    if (borrar.todas) {
                        herramientaActual = HERRAMIENTA_NINGUNA;
                        capaSeleccionada = -1;
                        capas.clear();
                        eventosCapas.todasLasCapasBorradas();
                    } else if (capas.size() > borrar.objetivo) {
                        Capa borrada = capas.remove(borrar.objetivo);
                        for (int i = 0; i < capas.size(); i++) {
                            Capa capa = capas.get(i);
                            capa.propiedades.objetivo = i;
                        }

                        capaSeleccionada = 0;
                        herramientaActual = HERRAMIENTA_NINGUNA;
                        eventosCapas.capaBorrada(borrada);
                    }
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
                        publicarCambiosEnBuffer(capa, capa.propiedades.objetivo);

                        capaSeleccionada = 0;
                        herramientaActual = HERRAMIENTA_NINGUNA;
                    }
                }

                if (object instanceof IntercambiarCapa) {
                    IntercambiarCapa intercambio = (IntercambiarCapa) object;
                    if (capas.size() > intercambio.objetivoOrigen && capas.size() > intercambio.objetivoDestino) {
                        Capa origen = capas.get(intercambio.objetivoOrigen);
                        origen.propiedades.objetivo = intercambio.objetivoDestino;

                        Capa destino = capas.get(intercambio.objetivoDestino);
                        destino.propiedades.objetivo = intercambio.objetivoOrigen;

                        capas.set(intercambio.objetivoDestino, origen);
                        capas.set(intercambio.objetivoOrigen, destino);
                        eventosCapas.capaIntercambiada(origen, destino);
                        herramientaActual = HERRAMIENTA_NINGUNA;
                        capaSeleccionada = 0;
                    }
                }

                if (object instanceof FragmentoBuffer) {
                    FragmentoBuffer fb = (FragmentoBuffer) object;
                    if (capas.size() > fb.objetivo) {
                        Capa editando = capas.get(fb.objetivo);
                        editando.buffer.setRGB(fb.origenX, fb.origenY, fb.largo,
                                fb.alto, fb.pixeles, 0, fb.largo);
                        imageUpdate(editando.buffer, ImageObserver.FRAMEBITS,
                                fb.origenX, fb.origenY, fb.largo, fb.alto);
                    }
                }

                if (object instanceof PropiedadesCapa) {
                    PropiedadesCapa nuevo = (PropiedadesCapa) object;
                    if (capas.size() > nuevo.objetivo) {
                        return;
                    }

                    BufferedImage buffer = new BufferedImage(nuevo.largo, nuevo.alto, BufferedImage.TYPE_INT_ARGB);
                    Graphics graphics = buffer.getGraphics();
                    if (!nuevo.colorFondo.equals(PropiedadesColor.COLOR_TRANSPARENTE)) {
                        graphics.setColor(nuevo.colorFondo.getColor());
                        graphics.fillRect(0, 0, nuevo.largo, nuevo.alto);
                    }

                    Capa capa = new Capa(0, nuevo, buffer);
                    eventosCapas.capaAgregada(capa);
                    capas.add(capa);
                    seleccionarCapa(capas.size() - 1, true);
                    subePendientes(nuevo.objetivo);
                }

                if (object instanceof MoverCapa) {
                    MoverCapa movimiento = (MoverCapa) object;
                    if (capas.size() > movimiento.objetivo) {
                        Capa editando = capas.get(movimiento.objetivo);
                        editando.propiedades.setOrigenX(movimiento.posicionX);
                        editando.propiedades.setOrigenY(movimiento.posicionY);
                        imageUpdate(editando.buffer, ImageObserver.FRAMEBITS,
                                0, 0, editando.propiedades.largo, editando.propiedades.alto);

                    }
                }
            }
        });
    }

    public void subePendientes(Integer objetivo) {
        if (!pendientesDePublicar.containsKey(objetivo)) {
            return;
        }

        Capa capa = pendientesDePublicar.get(objetivo);
        publicarCambiosEnBuffer(capa, objetivo);
    }

    public void setEventosCapas(CapaListener eventosCapas) {
        this.eventosCapas = eventosCapas;
    }

    public void redimensionaCapa(int objetivo, int largo, int alto) {
        this.cliente.sendTCP(new RedimensionarCapa(objetivo, largo, alto));
    }

    public void intercambiarCapa(int origen, int destino) {
        this.cliente.sendTCP(new IntercambiarCapa(origen, destino));
    }

    public void seleccionarCapa(int id, boolean emitir) {
        this.capaSeleccionada = id;
        if (this.capaSeleccionada > 0) {
            botonMover.setVisible(true);
        } else {
            botonMover.setVisible(false);
        }

        Capa capa = getCapaActiva();
        if (capa != null) {
            if (emitir) {
                eventosCapas.capaSeleccionada(getCapaActiva());
            }
        }

        herramientaActual = HERRAMIENTA_NINGUNA;
        setCursor(Cursor.getDefaultCursor());
        revalidate();
        repaint();
    }

    public List<Capa> getCapas() {
        return capas;
    }

    public Capa getCapaPrincipal() {
        if (this.capas.size() > 0) {
            return this.capas.get(0);
        }

        return null;
    }

    public Capa getCapaActiva() {
        if (this.capas.size() > this.capaSeleccionada && this.capaSeleccionada > -1) {
            return this.capas.get(this.capaSeleccionada);
        }

        return null;
    }

    public Color getColorRelleno() {
        return colorRelleno;
    }

    public void setColorRelleno(Color colorRelleno) {
        this.colorRelleno = colorRelleno;
    }

    public Color getColorBorde() {
        return colorBorde;
    }

    public void setColorBorde(Color colorBorde) {
        this.colorBorde = colorBorde;
    }

    public int getMagnitudBorde() {
        return magnitudBorde;
    }

    public void setMagnitudBorde(int magnitudBorde) {
        this.magnitudBorde = magnitudBorde;
    }

    public boolean isRellenar() {
        return rellenar;
    }

    public void setRellenar(boolean rellenar) {
        this.rellenar = rellenar;
    }

    public void setHerramienta(int herramienta) {
        this.herramientaActual = herramienta;
    }

    public void nuevoLienzo(PropiedadesCapa props) {
        BufferedImage buffer = new BufferedImage(props.largo, props.alto, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = buffer.getGraphics();
        graphics.setColor(props.colorFondo.getColor());
        graphics.fillRect(0, 0, props.largo, props.alto);
        graphics.dispose();

        this.cliente.sendTCP(new BorrarCapa(0, -1, true));
    }

    public void nuevaCapaVacia(String nombre, int largo, int alto) {
        BufferedImage buffer = new BufferedImage(largo, alto, BufferedImage.TYPE_INT_ARGB);
        crearCapa(nombre, 0, 0, largo, alto, buffer);
    }

    public void borraCapa(int objetivo) {
        this.cliente.sendTCP(new BorrarCapa(0, objetivo, false));
    }

    public void importarImagen(String nombre, Image imagen) {
        nombreImagenAImportar = nombre;
        imagenAImportar = imagen;
        this.herramientaActual = HERRAMIENTA_IMAGEN;
    }

    private void publicarCambiosEnBuffer(Capa capa, int objetivo) {
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
                this.cliente.sendTCP(fb);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Capa principal = getCapaPrincipal();
        if (principal != null) {
            return new Dimension(principal.propiedades.largo, principal.propiedades.alto);
        }

        return new Dimension(0, 0);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Capa[] dibujar = capas.toArray(new Capa[capas.size()]);
        for (Capa capa : dibujar) {
            g.drawImage(capa.buffer, capa.propiedades.origenX, capa.propiedades.origenY, this);
        }

        Graphics2D g2 = (Graphics2D) g;
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
        g2.setStroke(dashed);

        // Dibujamos el rectangulo temporal
        if (puntoInicial != null && puntoActual != null && herramientaActual != HERRAMIENTA_MOVER) {
            g2.setColor(Color.RED);
            Rectangle rect = new Rectangle(puntoInicial);
            rect.add(puntoActual);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);
        }

        // Dibujamos la capa seleccionada
        Capa capa = getCapaActiva();
        if (capa != null) {
            int largo = capa.propiedades.largo;
            int alto = capa.propiedades.alto;
            int origenX = capa.propiedades.origenX;
            int origenY = capa.propiedades.origenY;
            g2.setColor(Color.BLACK);
            g2.drawRect(origenX, origenY, largo, alto);
        }

        g2.dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 76, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 69, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    public Capa crearCapa(String nombre, int origenX, int origenY, int largo, int alto, BufferedImage buffer) {
        int objetivo = this.capas.size();
        PropiedadesCapa props = new PropiedadesCapa();
        props.nombre = nombre;
        props.largo = largo;
        props.alto = alto;
        props.origenX = origenX;
        props.origenY = origenY;
        props.colorFondo = PropiedadesColor.COLOR_TRANSPARENTE;
        props.objetivo = objetivo;
        this.publicarNuevaCapa(props);

        Capa nueva = new Capa(0, props, buffer);
        this.pendientesDePublicar.put(objetivo, nueva);
        return nueva;
    }

    public void publicarNuevaCapa(PropiedadesCapa props) {
        this.cliente.sendTCP(props);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        switch (herramientaActual) {
            case HERRAMIENTA_MOVER:
                Capa activa = getCapaActiva();
                if (activa != null) {
                    origenInicial = new Point(activa.propiedades.origenX, activa.propiedades.origenY);
                    if (!activa.propiedades.getRectangle().contains(e.getPoint()) && capaSeleccionada > 0) {
                        herramientaActual = HERRAMIENTA_NINGUNA;
                        setCursor(Cursor.getDefaultCursor());
                        seleccionarCapa(0, true);
                    }
                }
            case HERRAMIENTA_RECTANGULO:
            case HERRAMIENTA_CIRCULO:
            case HERRAMIENTA_TRIANGULO:
            case HERRAMIENTA_IMAGEN:
            case HERRAMIENTA_POLIGONO:
                puntoInicial = e.getPoint();
                break;
            case HERRAMIENTA_TEXTO:
                String texto = JOptionPane.showInputDialog("Texto: ");
                if ("".equals(texto) || texto == null) {
                    break;
                }

                if (magnitudBorde <= 5) {
                    JOptionPane.showMessageDialog(this, "El texto es demasiado pequeño, aumente el acho del borde",
                            "Error de tamaño de texto", JOptionPane.WARNING_MESSAGE);
                    break;
                }

                Font fuente = new Font("TimesRoman", Font.PLAIN, this.magnitudBorde);
                FontMetrics metricas = getGraphics().getFontMetrics(fuente);
                Rectangle2D limites = metricas.getStringBounds(texto, getGraphics());

                int largo = (int) Math.ceil(limites.getWidth()) + metricas.getLeading() + 5;
                int alto = (int) Math.ceil(Math.abs(metricas.getMaxAscent()) + Math.abs(metricas.getMaxDescent()));

                BufferedImage buffer = new BufferedImage(largo, alto, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = buffer.createGraphics();
                graphics.setFont(fuente);
                graphics.setColor(colorRelleno);
                graphics.drawString(texto, 0, (int) limites.getHeight() - metricas.getMaxDescent());
                graphics.dispose();

                crearCapa("Texto: \"" + texto + "\"", e.getX(), e.getY(), largo, alto, buffer);
                break;

            case HERRAMIENTA_SELECCIONAR:
                for (int i = capas.size() - 1; i >= 0; i--) {
                    Capa capa = capas.get(i);
                    if (capa.propiedades.getRectangle().contains(e.getPoint())) {
                        seleccionarCapa(i, true);
                        break;
                    }
                }
                break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Capa activa = getCapaActiva();
        Point puntoFinal = e.getPoint();
        int xMax = 0, yMax = 0, xMin = 0, yMin = 0, largo = 0, alto = 0;
        Polygon poligono;
        boolean cambios = false;
        BufferedImage buffer = null;
        Graphics2D graphics = null;
        String figura = "capa";

        if (puntoInicial != null) {
            xMax = puntoFinal.x > puntoInicial.x ? puntoFinal.x : puntoInicial.x;
            yMax = puntoFinal.y > puntoInicial.y ? puntoFinal.y : puntoInicial.y;
            xMin = puntoFinal.x < puntoInicial.x ? puntoFinal.x : puntoInicial.x;
            yMin = puntoFinal.y < puntoInicial.y ? puntoFinal.y : puntoInicial.y;
            largo = xMax - xMin;
            alto = yMax - yMin;

            if (largo > 0 && alto > 0) {
                buffer = new BufferedImage(largo, alto, BufferedImage.TYPE_INT_ARGB);
                graphics = buffer.createGraphics();
            }
        }

        switch (herramientaActual) {
            case HERRAMIENTA_RECTANGULO:
                if (graphics == null) {
                    break;
                }

                if (rellenar) {
                    graphics.setPaint(colorRelleno);
                    graphics.fillRect(0, 0, largo, alto);
                }

                graphics.setPaint(colorBorde);
                graphics.setStroke(new BasicStroke(this.magnitudBorde));
                graphics.drawRect(0, 0, largo, alto);
                cambios = true;
                figura = "Rectángulo";
                break;
            case HERRAMIENTA_CIRCULO:
                if (graphics == null) {
                    break;
                }

                int despl = this.magnitudBorde;
                if (rellenar) {
                    graphics.setPaint(colorRelleno);
                    graphics.fillOval(0 + despl / 2, 0 + despl / 2, largo - despl, alto - despl);
                }

                graphics.setPaint(colorBorde);
                graphics.setStroke(new BasicStroke(despl));
                graphics.drawOval(0 + despl / 2, 0 + despl / 2, largo - despl, alto - despl);
                cambios = true;
                figura = "Circulo";
                break;
            case HERRAMIENTA_TRIANGULO:
                if (graphics == null) {
                    break;
                }

                poligono = new Polygon();
                double theta = (2 * Math.PI / 3);
                for (int i = 0; i < 3; ++i) {
                    double px = Math.cos(theta * i) * largo / 2;
                    double py = Math.sin(theta * i) * alto / 2;

                    int x = (int) px + largo / 2;
                    int y = (int) py + alto / 2;
                    poligono.addPoint(x, y);
                }

                graphics.setPaint(colorBorde);
                graphics.fillPolygon(poligono);

                poligono = new Polygon();
                for (int i = 0; i < 3; ++i) {
                    int x = (int) (Math.cos(theta * i) * (largo / 2 - this.magnitudBorde));
                    int y = (int) (Math.sin(theta * i) * (alto / 2 - this.magnitudBorde));
                    poligono.addPoint(x + largo / 2, y + alto / 2);
                }

                if (rellenar) {
                    graphics.setPaint(colorRelleno);
                    graphics.fillPolygon(poligono);
                }

                cambios = true;
                figura = "Triángulo";
                break;
            case HERRAMIENTA_POLIGONO:
                if (graphics == null) {
                    break;
                }

                Integer lados = Integer.parseInt(JOptionPane.showInputDialog(this, "Ingrese lados:"));
                poligono = new Polygon();
                theta = 2 * Math.PI / lados;
                for (int i = 0; i < lados; ++i) {
                    int x = (int) (Math.cos(theta * i) * (largo / 2));
                    int y = (int) (Math.sin(theta * i) * (alto / 2));
                    poligono.addPoint(x + largo / 2, y + alto / 2);
                }

                graphics.setPaint(colorBorde);
                graphics.fillPolygon(poligono);

                poligono = new Polygon();
                for (int i = 0; i < lados; ++i) {
                    int x = (int) (Math.cos(theta * i) * (largo / 2 - this.magnitudBorde));
                    int y = (int) (Math.sin(theta * i) * (alto / 2 - this.magnitudBorde));
                    poligono.addPoint(x + largo / 2, y + alto / 2);
                }

                if (rellenar) {
                    graphics.setPaint(colorRelleno);
                    graphics.fillPolygon(poligono);
                }

                cambios = true;
                figura = "Polígono regular";
                break;
            case HERRAMIENTA_IMAGEN:
                if (largo <= 0 || alto <= 0) {
                    break;
                }

                Image redimensionada = imagenAImportar.getScaledInstance(largo, alto, Image.SCALE_FAST);
                BufferedImage nuevoBuffer = new BufferedImage(largo, alto, BufferedImage.TYPE_INT_ARGB);
                Graphics graphicsBuffer = nuevoBuffer.getGraphics();
                graphicsBuffer.drawImage(redimensionada, 0, 0, this);
                graphicsBuffer.dispose();
                crearCapa(nombreImagenAImportar, xMin, yMin, largo, alto, nuevoBuffer);

                herramientaActual = HERRAMIENTA_NINGUNA;
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                imagenAImportar = null;
                nombreImagenAImportar = "";
                break;
            case HERRAMIENTA_LAPIZ:
                if (activa != null) {
                    publicarCambiosEnBuffer(activa, activa.propiedades.objetivo);
                }
                break;
            case HERRAMIENTA_MOVER:
                if (activa != null && capaSeleccionada > 0) {
                    int a = puntoInicial.x - origenInicial.x;
                    int b = puntoInicial.y - origenInicial.y;
                    this.cliente.sendTCP(new MoverCapa(++activa.versionMovimiento,
                            capaSeleccionada, e.getX() - a, e.getY() - b));
                }
                break;
        }

        if (graphics != null) {
            graphics.dispose();
        }

        if (cambios) {
            crearCapa(String.format("%s de %dx%d", figura, largo, alto), xMin, yMin, largo, alto, buffer);
        }

        puntoInicial = null;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        puntoActual = e.getPoint();
        Capa activa = getCapaActiva();
        if (activa == null) {
            return;
        }

        Graphics2D graphics = activa.buffer.createGraphics();
        PropiedadesCapa props = activa.propiedades;

        switch (herramientaActual) {
            case HERRAMIENTA_LAPIZ:
                graphics.setColor(colorRelleno);
                graphics.fillOval(e.getX() - props.origenX - magnitudBorde / 2,
                        e.getY() - props.origenY - magnitudBorde / 2, magnitudBorde, magnitudBorde);
                break;
            case HERRAMIENTA_MOVER:
                if (capaSeleccionada > 0) {
                    int a = puntoInicial.x - origenInicial.x;
                    int b = puntoInicial.y - origenInicial.y;

                    props.setOrigenX(e.getX() - a);
                    props.setOrigenY(e.getY() - b);
                }
                break;
        }

        imageUpdate(activa.buffer, ImageObserver.ALLBITS, 0, 0,
                activa.buffer.getWidth(), activa.buffer.getHeight());
        graphics.dispose();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    public static final class Capa {

        public int version;
        public PropiedadesCapa propiedades;
        public BufferedImage buffer;
        public long versionMovimiento;

        public Capa(int version, PropiedadesCapa lienzo, BufferedImage buffer) {
            this.version = version;
            this.propiedades = lienzo;
            this.buffer = buffer;
            this.versionMovimiento = 0;
        }
    }

    public static interface CapaListener {

        public void capaAgregada(Capa capa);

        public void capaSeleccionada(Capa capa);

        public void capaBorrada(Capa capa);

        public void todasLasCapasBorradas();

        public void capaIntercambiada(Capa origen, Capa destino);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
