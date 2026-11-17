package com.sehatharian.catalog;

import java.util.*;

/** Team media uses its own namespace. Existing RepDB slugs remain untouched. */
public final class TeamExercises {
    public record Clip(Exercise exercise,String file) {}
    private static Clip clip(String id,String name,String file,String instruction) {
        return new Clip(new Exercise("team-"+id,name,"Video karakter 3D buatan tim SehatHarian","beginner","bodyweight","full_body","strength",
            List.of(instruction,"Ikuti kemampuan Anda dan hentikan bila terasa nyeri."),List.of(),List.of(),List.of(),Map.of()),file);
    }
    public static final List<Clip> ALL=List.of(
        clip("bicycle-kick","Bicycle Kick","Bycycle_Kick.mp4","Berbaring, tekuk kaki bergantian dan putar tubuh ke arah lutut berlawanan."),
        clip("flutter-kicks","Flutter Kicks","Flutter_kicks.mp4","Jaga tubuh stabil, gerakkan kaki bergantian naik dan turun."),
        clip("walking","Jalan Kaki","Jalan_Kaki.mp4","Berjalan dengan langkah nyaman dan ayunan tangan alami."),
        clip("jogging","Jogging","Jogging.mp4","Berlari ringan dengan langkah pendek dan tubuh tegak."),
        clip("mountain-climbers","Mountain Climbers","Mountain_Climbers.mp4","Dari posisi plank, tarik lutut bergantian ke depan."),
        clip("push-up","Push-up","Push_Up.mp4","Jaga tubuh lurus, tekuk siku lalu dorong kembali secara terkendali."),
        clip("sit-up","Sit-up","Sit_Up.mp4","Tekuk lutut dan angkat tubuh dengan gerakan terkendali tanpa menarik leher."),
        clip("sumo-squat","Sumo Squat","Sumo_Squad.mp4","Buka kaki lebih lebar dari bahu, tekuk lutut searah ujung kaki lalu berdiri."),
        clip("tricep-dip","Tricep Dip","Tricep_Dip.mp4","Gunakan kursi stabil, tekuk siku lalu dorong tubuh kembali secara terkendali.")
    );
    private TeamExercises() {}
    public static Optional<Clip> find(String id) { return ALL.stream().filter(c->c.exercise().externalId().equals(id)).findFirst(); }
}
