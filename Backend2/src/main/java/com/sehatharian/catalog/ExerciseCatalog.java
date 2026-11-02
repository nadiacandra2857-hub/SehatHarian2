package com.sehatharian.catalog;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Offline adapter for normalized Java XML properties prepared with Python's standard JSON parser.
 * No JDBC, default identity, network, demo fallback, or personal data dependency. */
public final class ExerciseCatalog {
    public record Snapshot(List<Exercise> exercises, String source, String commit, String license) {
        public Snapshot { exercises = List.copyOf(exercises); }
    }
    private final Path root;
    private Snapshot cached;
    public ExerciseCatalog(Path root) { this.root = root.toAbsolutePath().normalize(); }
    public static ExerciseCatalog local() {
        String value = System.getenv("SEHATHARIAN_REPDB_DIR");
        Path location = Path.of(value == null ? ".runtime/repdb" : value);
        // JPro starts with Frontend2 as its working directory; packaged deployments may set the env explicitly.
        if (value == null && !Files.isDirectory(location) && Files.isRegularFile(Path.of("../settings.gradle")))
            location = Path.of("../.runtime/repdb");
        return new ExerciseCatalog(location);
    }
    public synchronized Snapshot load() throws IOException {
        if (cached != null) return cached;
        Properties p = new Properties();
        try (var input = Files.newInputStream(root.resolve("catalog.xml"))) { p.loadFromXML(input); }
        if (!"1".equals(p.getProperty("format")) || !"3".equals(p.getProperty("schema")) || !"en".equals(p.getProperty("language")))
            throw new IOException("Format katalog lokal tidak didukung. Jalankan penyiapan ulang.");
        String commit = required(p, "commit");
        if (!commit.matches("[a-f0-9]{40}")) throw new IOException("Commit sumber tidak valid.");
        int count;
        try { count = Integer.parseInt(required(p, "count")); }
        catch (NumberFormatException e) { throw new IOException("Jumlah katalog tidak valid.", e); }
        if (count < 0 || count > 5000) throw new IOException("Jumlah katalog di luar batas.");
        var items = new ArrayList<Exercise>(); var ids = new HashSet<String>();
        for (int i = 0; i < count; i++) {
            String prefix = "exercise." + i + ".";
            String id = required(p, prefix + "id");
            if (!id.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || !ids.add(id)) throw new IOException("ID katalog tidak valid atau duplikat.");
            String difficulty = required(p,prefix+"difficulty"), category = required(p,prefix+"category"), body = required(p,prefix+"body_part");
            if (!Set.of("beginner","intermediate","advanced").contains(difficulty)
                    || !Set.of("strength","stretching","cardio","olympic","plyometrics").contains(category)
                    || !Set.of("upper_legs","back","core","shoulders","upper_arms","chest","full_body","lower_legs","lower_arms").contains(body))
                throw new IOException("Enum katalog tidak dikenal.");
            var images = new LinkedHashMap<String,String>();
            for (String pose : List.of("start","peak","main")) {
                String path = p.getProperty(prefix+"images."+pose);
                if (path != null) { validatePath(path); images.put(pose,path); }
            }
            if (!images.containsKey("main") && !(images.containsKey("start") && images.containsKey("peak"))) throw new IOException("Posisi gambar tidak lengkap.");
            var instructions = lines(p,prefix+"instructions_en"); var muscles = lines(p,prefix+"primary_muscles");
            if (instructions.isEmpty()) throw new IOException("Instruksi tidak lengkap.");
            items.add(new Exercise(id,required(p,prefix+"name_en"),p.getProperty(prefix+"description",""),difficulty,
                    required(p,prefix+"equipment"),body,category,instructions,lines(p,prefix+"tips_en"),muscles,
                    lines(p,prefix+"secondary_muscles"),images));
        }
        cached = new Snapshot(items,required(p,"source"),commit,required(p,"license"));
        return cached;
    }
    public Path asset(String path) throws IOException {
        validatePath(path);
        Path resolved = root.resolve(path).normalize();
        if (!Files.isRegularFile(resolved) || !resolved.toRealPath().startsWith(root.toRealPath())) throw new IOException("Gambar tidak tersedia.");
        return resolved;
    }
    private static void validatePath(String path) throws IOException {
        if (path == null || !path.matches("images/flat/[a-z0-9][a-z0-9-]*\\.png")) throw new IOException("Path gambar tidak aman.");
    }
    private static String required(Properties p,String key) throws IOException {
        String value=p.getProperty(key);
        if(value==null || value.isBlank())throw new IOException("Field katalog tidak lengkap: "+key);
        return value;
    }
    private static List<String> lines(Properties p,String key) { return p.getProperty(key,"").lines().filter(s->!s.isBlank()).toList(); }
    public static List<Exercise> filter(List<Exercise> items,String query,String difficulty,String equipment,String body,String category) {
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return items.stream().filter(e->e.name().toLowerCase(Locale.ROOT).contains(q))
                .filter(e->match(difficulty,e.difficulty()) && match(equipment,e.equipment()) && match(body,e.bodyPart()) && match(category,e.category())).toList();
    }
    private static boolean match(String selected,String value) { return selected==null || selected.isEmpty() || selected.equals(value); }
}
