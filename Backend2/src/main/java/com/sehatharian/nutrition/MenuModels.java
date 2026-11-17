package com.sehatharian.nutrition;

import java.util.*;
import java.text.Normalizer;

/** Explicit recipe semantics; dietary matching is not a medical safety classification. */
public final class MenuModels {
    private MenuModels(){}
    public static final String ALLERGEN_SOURCE="https://www.fda.gov/food/buy-store-serve-safe-food/food-allergies-what-you-need-know";
    public static final String DIET_SOURCE="https://www.nhs.uk/live-well/eat-well/how-to-eat-a-balanced-diet/the-vegetarian-diet/ | https://www.nhs.uk/live-well/eat-well/how-to-eat-a-balanced-diet/the-vegan-diet/";
    public static final String LIMIT="Pencocokan hanya berdasarkan bahan yang tercatat. Label produk, kontaminasi silang dan variasi resep perlu diperiksa terpisah; bukan jaminan bebas alergen.";
    public enum Allergen {
        MILK("Susu"),EGG("Telur"),FISH("Ikan"),CRUSTACEAN("Krustasea"),TREE_NUT("Kacang pohon"),PEANUT("Kacang tanah"),WHEAT("Gandum"),SOY("Kedelai"),SESAME("Wijen");
        private final String label;Allergen(String label){this.label=label;}@Override public String toString(){return label;}
    }
    public enum Origin { UNKNOWN,PLANT,MINERAL,EGG,DAIRY,MEAT,FISH,OTHER_ANIMAL }
    public enum Assessment { UNREVIEWED,REVIEWED }
    public enum Completeness { UNREVIEWED,INCOMPLETE,COMPLETE }
    public enum Diet { UNKNOWN,OMNIVORE,VEGETARIAN,VEGAN }
    public enum Declaration { UNKNOWN,NONE,SELECTED }
    public record Ingredient(String id,String name,Origin origin,Assessment assessment,Set<Allergen> allergens,String source,String basis,long revision,List<String> aliases){
        public Ingredient{allergens=Set.copyOf(allergens);aliases=List.copyOf(aliases);}
        @Override public String toString(){return name;}
        public boolean reviewed(){return assessment==Assessment.REVIEWED&&origin!=Origin.UNKNOWN&&!source.isBlank()&&!basis.isBlank();}
    }
    public record Line(String ingredientId,String unresolvedName,Double quantity,String unit,Double gramsPerUnit,String conversionSource,String foodId,String foodVersion,boolean nutritionVerified,String basisSource){
        public Line {
            if(unresolvedName==null||unresolvedName.length()>255||!Set.of("g","ml","unit","sdm","sdt","cup","unknown").contains(unit)
                    ||quantity!=null&&(!Double.isFinite(quantity)||quantity<=0||quantity>100000)
                    ||gramsPerUnit!=null&&(!Double.isFinite(gramsPerUnit)||gramsPerUnit<=0||gramsPerUnit>10000))throw new IllegalArgumentException("Jumlah atau satuan bahan tidak valid.");
            Objects.requireNonNull(conversionSource);Objects.requireNonNull(basisSource);
            if(ingredientId==null&&unresolvedName.isBlank())throw new IllegalArgumentException("Bahan yang belum dikenal perlu nama asli.");
        }
        public Double grams(){return quantity==null||unit.equals("unknown")?null:unit.equals("g")?quantity:gramsPerUnit!=null&&!conversionSource.isBlank()?quantity*gramsPerUnit:null;}
    }
    public record Composition(Completeness completeness,Double servings,String source,List<Line> lines){
        public Composition {
            Objects.requireNonNull(completeness);Objects.requireNonNull(source);lines=List.copyOf(lines);
            if(lines.size()>50||source.length()>8000||servings!=null&&(!Double.isFinite(servings)||servings<=0||servings>1000))throw new IllegalArgumentException("Porsi resep atau jumlah bahan tidak valid.");
        }
        public static Composition unknown(){return new Composition(Completeness.UNREVIEWED,null,"",List.of());}
    }
    public static String normalize(String value){return Normalizer.normalize(Objects.toString(value,""),Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
    public static String allergens(Set<Allergen> values){return values.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(","));}
    public static Set<Allergen> allergens(String value){if(value==null||value.isBlank())return Set.of();Set<Allergen> out=EnumSet.noneOf(Allergen.class);for(String token:value.split(","))out.add(Allergen.valueOf(token));return Set.copyOf(out);}
    public static String digest(List<String> parts){try{var bytes=new java.io.ByteArrayOutputStream();var stream=new java.io.DataOutputStream(bytes);for(String part:parts){byte[] data=Objects.toString(part,"<null>").getBytes(java.nio.charset.StandardCharsets.UTF_8);stream.writeInt(data.length);stream.write(data);}return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));}catch(Exception e){throw new IllegalStateException(e);}}
    public static boolean dietAllows(Diet diet,Origin origin){return origin!=Origin.UNKNOWN&&switch(diet){case UNKNOWN->false;case OMNIVORE->true;case VEGETARIAN->!Set.of(Origin.MEAT,Origin.FISH,Origin.OTHER_ANIMAL).contains(origin);case VEGAN->origin==Origin.PLANT||origin==Origin.MINERAL;};}
}
