package com.sehatharian;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.sehatharian.nutrition.Nutrients;
import com.sehatharian.content.ContentService;
import com.sehatharian.repository.ActualSleepRepository;
import com.sehatharian.catalog.TeamExercises;
import java.time.*;
import java.util.*;
class RombakDomainTest {
    @Test void portionPreservesMissingAndRejectsNonFinite(){var n=new Nutrients(Map.of("calories",200.0,"protein",8.0));assertEquals(300,n.portion(150).values().get("calories"));assertEquals(12,n.portion(150).values().get("protein"));assertFalse(n.portion(150).values().containsKey("fiber"));for(double g:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,10001})assertThrows(IllegalArgumentException.class,()->n.portion(g));assertEquals(n,Nutrients.decode(n.encode()));}
    @Test void midnightDurationUsesDates(){var day=LocalDate.now().minusDays(3);assertEquals(480,ActualSleepRepository.duration(day.atTime(22,0),day.plusDays(1).atTime(6,0)));assertThrows(IllegalArgumentException.class,()->ActualSleepRepository.duration(day.atTime(22,0),day.atTime(6,0)));}
    @Test void publicationCannotSkipReview(){assertFalse(ContentService.transitionAllowed(ContentService.State.DRAFT,ContentService.State.PUBLISHED));assertTrue(ContentService.transitionAllowed(ContentService.State.REVIEWED,ContentService.State.PUBLISHED));assertFalse(ContentService.transitionAllowed(ContentService.State.PUBLISHED,ContentService.State.DRAFT));}
    @Test void teamNamespaceDoesNotReplaceLegacy(){assertEquals(9,TeamExercises.ALL.size());assertEquals(9,TeamExercises.ALL.stream().map(c->c.exercise().externalId()).distinct().count());assertTrue(TeamExercises.find("air-bike").isEmpty());assertEquals("Sumo_Squad.mp4",TeamExercises.find("team-sumo-squat").orElseThrow().file());}
}
