package com.sehatharian;

import com.sehatharian.catalog.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ExerciseCatalogTest {
    @TempDir Path dir;
    Properties props() {
        Properties p=new Properties();p.setProperty("format","1");p.setProperty("schema","3");p.setProperty("language","en");p.setProperty("count","2");p.setProperty("source","Synthetic reference");p.setProperty("license","Synthetic");p.setProperty("commit","a".repeat(40));
        for(int i=0;i<2;i++) {String k="exercise."+i+".";
            Map.of("id","example-"+i,"name_en",i==0?"Example First":"Another Second","difficulty",i==0?"beginner":"advanced","equipment","_unspecified","body_part","core","category","strength","instructions_en","Synthetic instruction","primary_muscles","","images.main","images/flat/shared.png").forEach((key,value)->p.setProperty(k+key,value));
        }return p;
    }
    ExerciseCatalog write(Properties p)throws Exception {try(var out=Files.newOutputStream(dir.resolve("catalog.xml"))){p.storeToXML(out,"synthetic fixture");}return new ExerciseCatalog(dir);}
    @Test void sharedAssetDoesNotMergeExternalIdsAndLoadsWithoutAccountOrDatabase()throws Exception {
        var catalog=write(props());var snapshot=catalog.load();assertEquals(2,snapshot.exercises().size());assertNotEquals(snapshot.exercises().get(0).externalId(),snapshot.exercises().get(1).externalId());assertSame(snapshot,catalog.load());
    }
    @Test void filterIntersectionSearchAndNoResult()throws Exception {
        var rows=write(props()).load().exercises();assertEquals(1,ExerciseCatalog.filter(rows,"EXAMPLE",null,null,null,null).size());assertEquals(0,ExerciseCatalog.filter(rows,"example","advanced",null,null,null).size());assertEquals(1,ExerciseCatalog.filter(rows,"","advanced","_unspecified","core","strength").size());
    }
    @Test void missingAssetsRemainExplicitFailuresNotRemoteFallback()throws Exception {
        var catalog=write(props());assertEquals(2,catalog.load().exercises().size());assertThrows(java.io.IOException.class,()->catalog.asset("images/flat/shared.png"));
    }
    @Test void rejectsTraversalAndSymlinkOutsideCatalog()throws Exception {
        var catalog=write(props());assertThrows(java.io.IOException.class,()->catalog.asset("../../a.png"));assertThrows(java.io.IOException.class,()->catalog.asset("https://x/a.png"));
        Path outside=Files.createTempFile("ui3-outside-",".png");try{Files.createDirectories(dir.resolve("images/flat"));Files.createSymbolicLink(dir.resolve("images/flat/shared.png"),outside);assertThrows(java.io.IOException.class,()->catalog.asset("images/flat/shared.png"));}finally{Files.deleteIfExists(outside);}
    }
    @Test void rejectsDuplicateIdUnknownEnumMissingInstructionsAndBrokenMetadata()throws Exception {
        for(String key:List.of("exercise.1.id","exercise.1.category","exercise.1.instructions_en","commit")){
            Properties p=props();p.setProperty(key,key.endsWith("id")?"example-0":key.endsWith("category")?"unknown":"");var catalog=write(p);assertThrows(java.io.IOException.class,catalog::load);
        }
    }
}
