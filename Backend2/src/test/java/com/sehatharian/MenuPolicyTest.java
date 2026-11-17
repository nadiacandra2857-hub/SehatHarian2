package com.sehatharian;
import com.sehatharian.nutrition.*;
import static com.sehatharian.nutrition.MenuModels.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MenuPolicyTest {
    @Test void dietaryRulesUseKnownOriginsAndNeverGuessUnknown(){
        assertTrue(dietAllows(Diet.VEGETARIAN,Origin.EGG));assertTrue(dietAllows(Diet.VEGETARIAN,Origin.DAIRY));
        assertFalse(dietAllows(Diet.VEGETARIAN,Origin.MEAT));assertFalse(dietAllows(Diet.VEGETARIAN,Origin.FISH));
        assertFalse(dietAllows(Diet.VEGAN,Origin.EGG));assertFalse(dietAllows(Diet.VEGAN,Origin.DAIRY));assertFalse(dietAllows(Diet.VEGAN,Origin.OTHER_ANIMAL));
        for(Diet diet:Diet.values())assertFalse(dietAllows(diet,Origin.UNKNOWN));assertTrue(dietAllows(Diet.VEGAN,Origin.PLANT));
    }
    @Test void emptyDeclarationsAreDistinctFromExplicitNone(){
        var blank=new MenuPreferences.Profile(0,Diet.UNKNOWN,Declaration.UNKNOWN,Declaration.UNKNOWN,Set.of(),Set.of(),Set.of(),"","",List.of());
        var none=new MenuPreferences.Profile(1,Diet.VEGETARIAN,Declaration.NONE,Declaration.NONE,Set.of(),Set.of(),Set.of(),"","",List.of());
        assertNotNull(blank.gate());assertNull(none.gate());assertNotEquals(blank.signature(),none.signature());
        var unknown=new MenuPreferences.Profile(2,Diet.VEGETARIAN,Declaration.NONE,Declaration.SELECTED,Set.of(),Set.of(),Set.of(),"","campuran X",List.of("Pantangan: campuran X"));assertNotNull(unknown.gate());
    }
    @Test void conversionsNeedKnownUnitAndExplicitSource(){
        assertEquals(25d,line(25d,"g",null,"").grams());
        assertNull(line(2d,"sdm",15d,"").grams());assertEquals(30d,line(2d,"sdm",15d,"Takaran terukur fixture").grams());
        assertNull(line(null,"g",null,"").grams());assertNull(line(2d,"unknown",15d,"source").grams());
        assertThrows(IllegalArgumentException.class,()->line(-1d,"g",null,""));
    }
    Line line(Double amount,String unit,Double conversion,String source){return new Line("rice","",amount,unit,conversion,source,null,null,false,"");}
    @Test void normalizedAliasesDoNotBecomeSubstringMatching(){assertEquals("kacang tanah",normalize("  KACANG　TANAH  "));assertNotEquals(normalize("kacang"),normalize("kacang tanah"));}
}
