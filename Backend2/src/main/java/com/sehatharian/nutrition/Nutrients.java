package com.sehatharian.nutrition;
import java.util.*;

/** Missing values are absent, never zero. Immutable per-portion snapshot. */
public record Nutrients(Map<String,Double> values) {
    public static final List<String> KEYS=List.of("calories","protein","carbs","fat","fiber","sugar","saturated_fat","salt");
    public Nutrients {
        values=Map.copyOf(values);
        if(values.entrySet().stream().anyMatch(e->!KEYS.contains(e.getKey()) || !Double.isFinite(e.getValue()) || e.getValue()<0))throw new IllegalArgumentException("Nilai gizi tidak valid.");
    }
    public Nutrients portion(double grams) {
        if(!Double.isFinite(grams)||grams<=0||grams>10000)throw new IllegalArgumentException("Porsi harus lebih dari 0 dan maksimal 10.000 gram.");
        Map<String,Double> result=new LinkedHashMap<>();values.forEach((k,v)->result.put(k,v*grams/100));return new Nutrients(result);
    }
    public String encode() {return KEYS.stream().filter(values::containsKey).map(k->k+"="+values.get(k)).collect(java.util.stream.Collectors.joining(";"));}
    public static Nutrients decode(String text) {
        Map<String,Double> values=new HashMap<>();if(text!=null&&!text.isBlank())for(String pair:text.split(";")){String[] p=pair.split("=",2);values.put(p[0],Double.valueOf(p[1]));}return new Nutrients(values);
    }
    public String display() {
        String[] labels={"Energi","Protein","Karbohidrat","Lemak","Serat","Gula","Lemak jenuh","Garam"};StringJoiner out=new StringJoiner("\n");
        for(int i=0;i<KEYS.size();i++){Double v=values.get(KEYS.get(i));out.add(labels[i]+": "+(v==null?"belum diketahui":String.format(Locale.ROOT,"%.2f %s",v,i==0?"kkal":"g")));}return out.toString();
    }
}
