package net.decatron.rucksack.config;

import net.decatron.rucksack.RucksackPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Valores de colocacion de la mochila en la espalda, ajustables en vivo desde
 * el juego con /rucksack ajuste y persistibles a config.yml.
 *
 * Existen porque estos numeros no se pueden deducir: dependen de como se ve el
 * modelo en pantalla, asi que se afinan mirando y no calculando.
 */
public class RenderSettings {

    private static final String PATH = "render.espalda.";

    /** Valores de fabrica, afinados en el juego sobre el modelo de cuero (pose parado). */
    public static final double DEF_SEPARACION = 0.30;
    public static final double DEF_ALTURA     = 1.35;
    public static final double DEF_LATERAL    = 0.00;
    public static final double DEF_ESCALA     = 1.45;
    public static final double DEF_GIRO       = 180.0;
    public static final double DEF_INCLINA    = 0.20;

    /**
     * Ticks de interpolacion del lado del cliente. Debe coincidir con cada cuanto
     * actualizamos (1 tick): con valores mas altos la mochila nunca alcanza su
     * destino y queda permanentemente atrasada — se despega al correr y se traba
     * de costado al girar rapido.
     */
    public static final int DEF_SUAVIZADO = 1;

    /**
     * Cuanto se adelanta la mochila al movimiento del jugador, para compensar que
     * el servidor siempre va un paso atras del cliente. 0 = sin compensacion.
     */
    public static final double DEF_PREDICCION = 1.0;

    private final RucksackPlugin plugin;

    private double separacion;
    private double altura;
    private double lateral;
    private double escala;
    private double giro;
    private double inclina;
    private int    suavizado;
    private double prediccion;

    public RenderSettings(RucksackPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        separacion = cfg.getDouble(PATH + "separacion", DEF_SEPARACION);
        altura     = cfg.getDouble(PATH + "altura",     DEF_ALTURA);
        lateral    = cfg.getDouble(PATH + "lateral",    DEF_LATERAL);
        escala     = cfg.getDouble(PATH + "escala",     DEF_ESCALA);
        giro       = cfg.getDouble(PATH + "giro",       DEF_GIRO);
        inclina    = cfg.getDouble(PATH + "inclinacion", DEF_INCLINA);
        suavizado  = cfg.getInt(PATH + "suavizado",     DEF_SUAVIZADO);
        prediccion = cfg.getDouble(PATH + "prediccion", DEF_PREDICCION);
    }

    public void save() {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set(PATH + "separacion",  round(separacion));
        cfg.set(PATH + "altura",      round(altura));
        cfg.set(PATH + "lateral",     round(lateral));
        cfg.set(PATH + "escala",      round(escala));
        cfg.set(PATH + "giro",        round(giro));
        cfg.set(PATH + "inclinacion", round(inclina));
        cfg.set(PATH + "suavizado",   suavizado);
        cfg.set(PATH + "prediccion",  round(prediccion));
        plugin.saveConfig();
    }

    public void reset() {
        separacion = DEF_SEPARACION;
        altura     = DEF_ALTURA;
        lateral    = DEF_LATERAL;
        escala     = DEF_ESCALA;
        giro       = DEF_GIRO;
        inclina    = DEF_INCLINA;
        suavizado  = DEF_SUAVIZADO;
        prediccion = DEF_PREDICCION;
    }

    /** Linea unica lista para copiar y pegar, para fijar estos valores como default. */
    public String toOneLine() {
        return String.format(
                "separacion=%.3f altura=%.3f lateral=%.3f escala=%.3f giro=%.1f inclinacion=%.1f "
                        + "suavizado=%d prediccion=%.2f",
                separacion, altura, lateral, escala, giro, inclina, suavizado, prediccion);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    // -------------------------------------------------------------------------

    public double getSeparacion() { return separacion; }
    public double getAltura()     { return altura; }
    public double getLateral()    { return lateral; }
    public double getEscala()     { return escala; }
    public double getGiro()       { return giro; }
    public double getInclina()    { return inclina; }
    public int    getSuavizado()  { return suavizado; }
    public double getPrediccion() { return prediccion; }

    /** Aplica un valor por nombre. Retorna false si el parametro no existe. */
    public boolean set(String param, double value) {
        switch (param.toLowerCase()) {
            case "separacion", "sep"  -> separacion = value;
            case "altura", "alt"      -> altura = value;
            case "lateral", "lat"     -> lateral = value;
            case "escala", "esc"      -> escala = Math.max(0.05, value);
            case "giro"               -> giro = value;
            case "inclinacion", "inc" -> inclina = value;
            case "suavizado", "suav"  -> suavizado = (int) Math.max(0, Math.min(10, value));
            case "prediccion", "pred" -> prediccion = Math.max(0, value);
            default -> { return false; }
        }
        return true;
    }

    /** Valor actual por nombre, o NaN si el parametro no existe. */
    public double get(String param) {
        return switch (param.toLowerCase()) {
            case "separacion", "sep"  -> separacion;
            case "altura", "alt"      -> altura;
            case "lateral", "lat"     -> lateral;
            case "escala", "esc"      -> escala;
            case "giro"               -> giro;
            case "inclinacion", "inc" -> inclina;
            case "suavizado", "suav"  -> suavizado;
            case "prediccion", "pred" -> prediccion;
            default -> Double.NaN;
        };
    }
}
