package com.sehatharian;

import com.sehatharian.config.*;
import com.sehatharian.content.ContentService;
import com.sehatharian.content.ContentService.State;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.nutrition.*;
import static com.sehatharian.nutrition.MenuModels.*;
import static com.sehatharian.CuratedMenuIntegration.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/** Actual v1 -> v2 upgrade in a disposable DB, with explicit publication and owner isolation. */
public final class MenuContinuationIntegration {
    public static void main(String[] args) throws Exception {
        String db = System.getenv("DB_NAME");
        if (db == null || !db.matches("sehatharian_test_curated_lanjutan_[a-z0-9_]+"))
            throw new SecurityException("Disposable continuation schema required");
        if (args.length == 1 && args[0].equals("reopen")) {
            var rows = new NutritionService(access("alpha@example.test")).history(LocalDate.now());
            ok(rows.stream().anyMatch(m -> m.name().equals("Oat semalam buah") && m.portions() == 1
                    && m.nutrients().values().isEmpty() && m.details().contains("240.0 g")),
                "fresh JVM retains sourced oat ingredient snapshot with unknown total nutrition");
            return;
        }
        DatabaseBootstrap.initialize();
        ok(scalar("SELECT COUNT(*) FROM pengguna") == 0, "new schema has no demo users");
        FoodImport.main(new String[]{args[2]});
        account("admin@example.test", true);
        account("alpha@example.test", false);
        account("beta@example.test", false);
        var admin = access("admin@example.test");
        var owner = access("alpha@example.test");
        var other = access("beta@example.test");
        records(owner);
        prefs(owner, Diet.VEGETARIAN, Set.of());
        var importer = new CuratedMenus(admin);
        var cms = new ContentService(admin);
        var first = CuratedMenus.read(Path.of(args[0]));
        var next = CuratedMenus.read(Path.of(args[1]));
        var oldIds = importer.apply(first);
        for (int id : oldIds) publish(cms, id);
        var personal = new PersonalMenus(owner);
        ok(personal.recommend(LocalDate.now()).items().size() == 3, "v1 retains three eligible recipes");
        long versions = scalar("SELECT COUNT(*) FROM content_version");
        var plan = importer.plan(next);
        ok(plan.ready() && scalar("SELECT COUNT(*) FROM content_version") == versions
                && scalar("SELECT COUNT(*) FROM ingredient_definition") == 18,
            "v2 dry-run adds no definitions or versions");
        rejects(() -> new CuratedMenus(owner).apply(next), "user cannot import v2 recipes");
        var ids = importer.apply(next);
        ok(ids.subList(0, 3).equals(oldIds.subList(0, 3)), "three unchanged v1 menus reuse exact receipts");
        ok(!ids.get(3).equals(oldIds.get(3)) && !ids.get(4).equals(oldIds.get(4))
                && scalar("SELECT COUNT(*) FROM curated_menu_import") == 7,
            "only oat and rice gain new source-version receipts");
        ok(scalar("SELECT COUNT(*) FROM ingredient_definition") == 27
                && scalar("SELECT MAX(revision) FROM ingredient_definition") == 1,
            "nine precise definitions added without revising existing ingredients");
        ok(personal.recommend(LocalDate.now()).items().size() == 3,
            "new draft revisions do not replace previous publications");
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<List<Integer>> retry = () -> new CuratedMenus(admin).apply(next);
            var a = pool.submit(retry);
            var b = pool.submit(retry);
            ok(a.get().equals(ids) && b.get().equals(ids)
                    && scalar("SELECT COUNT(*) FROM curated_menu_import") == 7,
                "concurrent retries reuse seven receipts without duplicates");
        } finally { pool.shutdownNow(); }
        for (int i = 3; i < 5; i++) {
            var mapped = new MenuComposition(admin).admin(ids.get(i));
            ok(mapped.composition().equals(next.menus().get(i).composition())
                    && !mapped.eligible(), "exact new mapping stored as unreviewed draft");
            publish(cms, ids.get(i));
            ok(new MenuComposition(owner).published(ids.get(i)).eligible(), "explicit review and publish enables recipe");
        }
        ok(personal.recommend(LocalDate.now()).items().size() == 5,
            "five sourced recipes eligible for explicit vegetarian preferences");
        for (var category : List.of(Allergen.MILK, Allergen.EGG, Allergen.SOY, Allergen.WHEAT)) {
            prefs(owner, Diet.VEGETARIAN, Set.of(category));
            var matches = personal.recommend(LocalDate.now()).items();
            int expected = category == Allergen.MILK || category == Allergen.EGG ? 3 : 4;
            ok(matches.size() == expected && matches.stream().allMatch(m ->
                    m.mapping().ingredients().values().stream().noneMatch(i -> i.allergens().contains(category))),
                "hard exclusion for " + category.name() + " applies to newly specified products");
        }
        prefs(owner, Diet.VEGAN, Set.of());
        ok(personal.recommend(LocalDate.now()).items().size() == 2,
            "vegan selection excludes dairy oat and egg rice");
        prefs(owner, Diet.VEGETARIAN, Set.of());
        var oat = personal.review(ids.get(3), LocalDate.now(), "sarapan", 1);
        var rice = personal.review(ids.get(4), LocalDate.now(), "makan_siang", 1);
        ok(oat.recipeServings() == 4 && rice.recipeServings() == 4,
            "portion review uses four explicit adapted portions");
        ok(oat.nutrients().values().isEmpty() && rice.nutrients().values().isEmpty(),
            "unmapped yogurt and soy sauce keep all unsupported nutrition totals unknown");
        ok(oat.details().contains("240.0 g") && rice.details().contains("15.0 ml")
                && rice.details().contains("Kikkoman"), "review retains exact weighed product specifications");
        int meal = personal.log(oat);
        rejects(() -> personal.log(rice), "saving another meal invalidates earlier history-based review");
        personal.log(personal.review(ids.get(4), LocalDate.now(), "makan_siang", 1));
        var stored = new NutritionService(owner).history(LocalDate.now()).stream()
            .filter(m -> m.id() == meal).findFirst().orElseThrow();
        ok(new NutritionService(other).history(LocalDate.now()).isEmpty(), "other user sees no new menu snapshots");
        var composition = next.menus().get(3).composition();
        int revision = cms.saveMenuDraft(ids.get(3), "Oat — revisi porsi uji", "UJI revisi independen",
            new Composition(Completeness.COMPLETE, 2.0, composition.source(), composition.lines()));
        publish(cms, revision);
        rejects(() -> personal.log(oat), "new publication blocks stale portion review");
        var after = new NutritionService(owner).history(LocalDate.now()).stream()
            .filter(m -> m.id() == meal).findFirst().orElseThrow();
        ok(after.details().equals(stored.details()) && after.nutrients().values().isEmpty(),
            "old four-portion snapshot survives subsequent two-portion revision");
        System.out.println("ALL " + checks + " MENU CONTINUATION JDBC CHECKS PASSED");
    }
}
