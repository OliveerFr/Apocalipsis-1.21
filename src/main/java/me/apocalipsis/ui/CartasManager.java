package me.apocalipsis.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Gestiona el sistema de cartas:
 * - /carta: Abre GUI para enviar libros escritos como cartas
 * - Guarda cartas en JSON
 * - /cartas: GUI admin para ver todas las cartas
 */
public class CartasManager {
    
    private final File cartasFile;
    private final Gson gson;
    private final Logger logger;
    private List<CartaData> cartas;
    
    // Map de jugadores que tienen el menú de carta abierto
    private final Map<UUID, Inventory> menusCarta = new HashMap<>();
    
    // Cooldown para evitar spam (UUID -> tiempo en millis)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 60000; // 1 minuto
    private static final int MAX_PAGINAS = 50; // Límite de páginas
    private static final int MAX_CHARS_POR_PAGINA = 256; // Límite de caracteres por página
    
    public CartasManager(File dataFolder, Logger logger) {
        this.logger = logger;
        this.cartasFile = new File(dataFolder, "cartas.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cartas = new ArrayList<>();
        cargarCartas();
    }
    
    /**
     * Abre el menú para enviar una carta
     */
    public void abrirMenuCarta(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "§d§lEnviar Carta a Santa");
        
        // Slot 13: Instrucciones
        ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        info.editMeta(meta -> {
            meta.setDisplayName("§e§lCómo enviar tu carta");
            meta.setLore(Arrays.asList(
                "",
                "§71. Escribe y firma un libro",
                "§72. Arrastra el libro al §eslot vacío",
                "§73. Cierra el menú para enviar",
                "",
                "§c❤ §7Santa leerá todas las cartas",
                "",
                "§8(El slot vacío está a la izquierda)"
            ));
        });
        menu.setItem(13, info);
        
        // Slot 11: Área para poner el libro (dejar vacío para que puedan poner el libro)
        // No poner nada aquí para que sea un slot vacío donde colocar el libro
        
        // Decoración
        ItemStack deco = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        deco.editMeta(meta -> meta.setDisplayName(" "));
        for (int i : new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 23, 24, 25, 26}) {
            menu.setItem(i, deco);
        }
        
        menusCarta.put(player.getUniqueId(), menu);
        player.openInventory(menu);
    }
    
    /**
     * Procesa el cierre del menú de carta
     */
    public void procesarCierreCarta(Player player, Inventory inventory) {
        UUID uuid = player.getUniqueId();
        if (!menusCarta.containsKey(uuid)) {
            return; // No es un menú de carta
        }
        
        menusCarta.remove(uuid);
        
        // Verificar cooldown
        long ahora = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid)) {
            long tiempoRestante = (cooldowns.get(uuid) + COOLDOWN_MS) - ahora;
            if (tiempoRestante > 0) {
                long segundos = tiempoRestante / 1000;
                player.sendMessage("§c✦ Debes esperar " + segundos + " segundos para enviar otra carta.");
                return;
            }
        }
        
        // Buscar libro en el inventario (puede estar firmado o sin firmar)
        ItemStack libro = inventory.getItem(11);
        if (libro == null || (libro.getType() != Material.WRITTEN_BOOK && libro.getType() != Material.WRITABLE_BOOK)) {
            player.sendMessage("§c✦ No pusiste ningún libro.");
            player.sendMessage("§7Debes escribir un libro (puede estar firmado o sin firmar).");
            return;
        }
        
        BookMeta bookMeta = (BookMeta) libro.getItemMeta();
        if (bookMeta == null || !bookMeta.hasPages()) {
            player.sendMessage("§c✦ El libro está vacío.");
            return;
        }
        
        // Validar límites
        List<String> paginas = bookMeta.getPages();
        if (paginas.size() > MAX_PAGINAS) {
            player.sendMessage("§c✦ Tu carta es demasiado larga (máximo " + MAX_PAGINAS + " páginas).");
            return;
        }
        
        // Verificar que haya contenido real
        boolean tieneContenido = false;
        for (String pagina : paginas) {
            if (pagina != null && !pagina.trim().isEmpty()) {
                tieneContenido = true;
                break;
            }
        }
        
        if (!tieneContenido) {
            player.sendMessage("§c✦ Tu carta está vacía. Escribe tus deseos.");
            return;
        }
        
        if (!tieneContenido) {
            player.sendMessage("§c✦ Tu carta está vacía. Escribe tus deseos.");
            return;
        }
        
        // Guardar la carta
        CartaData carta = new CartaData();
        carta.remitente = player.getName();
        carta.remitenteUUID = uuid.toString();
        carta.titulo = bookMeta.hasTitle() ? bookMeta.getTitle() : "Sin título";
        carta.autor = bookMeta.hasAuthor() ? bookMeta.getAuthor() : player.getName();
        carta.paginas = bookMeta.getPages();
        carta.fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        cartas.add(carta);
        guardarCartas();
        
        // Establecer cooldown
        cooldowns.put(uuid, ahora);
        
        // Mensaje emotivo al jugador
        player.sendMessage("");
        player.sendMessage("§d§l✦ Carta Enviada ✦");
        player.sendMessage("");
        player.sendMessage("§7Tu carta ha sido enviada a Santa Claus.");
        player.sendMessage("§7Título: §f" + carta.titulo);
        player.sendMessage("");
        player.sendMessage("§7Santa leerá tus deseos y esperanzas.");
        player.sendMessage("§c❤ §7Nunca dejes de creer en ti mismo");
        player.sendMessage("");
        
        // Efectos especiales
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.8f);
        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
        
        // Notificar a admins online
        for (org.bukkit.entity.Player admin : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("apocalipsis.admin")) {
                admin.sendMessage("§7[§cCartas§7] §e" + player.getName() + " §7envió una carta: §f" + carta.titulo);
                admin.playSound(admin.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
            }
        }
        
        logger.info("[Cartas] " + player.getName() + " envió una carta: " + carta.titulo);
    }
    
    /**
     * Abre el menú admin para ver todas las cartas
     */
    public void abrirMenuCartasAdmin(Player admin) {
        if (cartas.isEmpty()) {
            admin.sendMessage("§c✦ No hay cartas enviadas aún.");
            return;
        }
        
        int size = Math.min(54, ((cartas.size() - 1) / 9 + 1) * 9);
        Inventory menu = Bukkit.createInventory(null, size, "§c§lCartas para Santa (" + cartas.size() + ")");
        
        for (int i = 0; i < Math.min(cartas.size(), size); i++) {
            CartaData carta = cartas.get(i);
            
            ItemStack libro = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) libro.getItemMeta();
            
            if (meta != null) {
                meta.setTitle(carta.titulo);
                meta.setAuthor(carta.autor);
                meta.setPages(carta.paginas);
                
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7De: §e" + carta.remitente);
                lore.add("§7Fecha: §f" + carta.fecha);
                lore.add("");
                lore.add("§eClick derecho para leer");
                meta.setLore(lore);
                
                libro.setItemMeta(meta);
            }
            
            menu.setItem(i, libro);
        }
        
        admin.openInventory(menu);
    }
    
    /**
     * Verifica si un menú es un menú de carta
     */
    public boolean esMenuCarta(Inventory inventory) {
        String title = inventory.getSize() == 27 ? "§d§lEnviar Carta a Santa" : null;
        return title != null && inventory.getType().toString().contains("CHEST");
    }
    
    /**
     * Verifica si un jugador tiene un menú de carta abierto
     */
    public boolean tieneMenuCartaAbierto(UUID uuid) {
        return menusCarta.containsKey(uuid);
    }
    
    /**
     * Carga las cartas desde JSON
     */
    private void cargarCartas() {
        if (!cartasFile.exists()) {
            cartas = new ArrayList<>();
            return;
        }
        
        try (Reader reader = new InputStreamReader(new FileInputStream(cartasFile), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<CartaData>>(){}.getType();
            List<CartaData> loadedCartas = gson.fromJson(reader, listType);
            cartas = loadedCartas != null ? loadedCartas : new ArrayList<>();
            logger.info("[Cartas] Cargadas " + cartas.size() + " cartas");
        } catch (Exception e) {
            logger.warning("[Cartas] Error al cargar cartas: " + e.getMessage());
            cartas = new ArrayList<>();
        }
    }
    
    /**
     * Guarda las cartas en JSON
     */
    private void guardarCartas() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(cartasFile), StandardCharsets.UTF_8)) {
            gson.toJson(cartas, writer);
        } catch (Exception e) {
            logger.severe("[Cartas] Error al guardar cartas: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene todas las cartas
     */
    public List<CartaData> getCartas() {
        return new ArrayList<>(cartas);
    }
    
    /**
     * Clase interna para datos de carta
     */
    public static class CartaData {
        public String remitente;
        public String remitenteUUID;
        public String titulo;
        public String autor;
        public List<String> paginas;
        public String fecha;
    }
}
