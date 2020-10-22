package impactassessment.passiveprocessengine.persistance;

import impactassessment.passiveprocessengine.definition.TaskDefinition;

import java.util.HashMap;

public class ShortTermTaskDefinitionCache {

    private static HashMap<String, TaskDefinition> cache = new HashMap<>();

    public ShortTermTaskDefinitionCache() {
    }

    public static void clearCache() {
        cache.clear();
    }

    public static void addToCache(TaskDefinition td) {
        cache.put(td.getId(), td);
    }

    public static TaskDefinition getFromCache(String id) {
        return cache.get(id);
    }

}