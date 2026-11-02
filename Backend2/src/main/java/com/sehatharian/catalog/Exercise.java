package com.sehatharian.catalog;

import java.util.List;
import java.util.Map;

/** Global reference content. External slug is never an activity primary key. */
public record Exercise(String externalId, String name, String description, String difficulty,
                       String equipment, String bodyPart, String category, List<String> instructions,
                       List<String> tips, List<String> primaryMuscles, List<String> secondaryMuscles,
                       Map<String,String> images) {
    public Exercise {
        instructions = List.copyOf(instructions); tips = List.copyOf(tips);
        primaryMuscles = List.copyOf(primaryMuscles); secondaryMuscles = List.copyOf(secondaryMuscles);
        images = Map.copyOf(images);
    }
}
