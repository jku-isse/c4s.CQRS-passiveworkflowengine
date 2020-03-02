package pingpong.query;

import lombok.extern.slf4j.XSlf4j;
import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XSlf4j
public class CounterModel {

    private static CounterModel instance;

    HashMap<String, Integer> db;
    List<Map<String, Integer>> snapshots;

    private CounterModel() {
        db = new HashMap<>();
        snapshots = new ArrayList<>();
    }

    public static CounterModel getInstance() {
        if (instance == null) {
            instance = new CounterModel();
        }
        return instance;
    }

    public void create(String id, int amount) {
        instance.db.put(id, amount);
    }
    public void increase(String id, int amount) {
        instance.db.put(id, instance.db.get(id) + amount);
    }
    public void decrease(String id, int amount) {
        instance.db.put(id, instance.db.get(id) - amount);
    }

    public void reset() {
        instance.db = new HashMap<>();
    }

    public void createSnapshot(){
        instance.snapshots.add(SerializationUtils.clone(instance.db));
    }


    public void print() {
        log.info("*****************LIVE******************");
        for (Map.Entry<String, Integer> e : instance.db.entrySet()) {
            log.info("id: {}, amount: {}", e.getKey(), e.getValue());
        }
        for (Map<String, Integer> m : instance.snapshots) {
            log.info("*****************SNAP******************");
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                log.info("id: {}, amount: {}", e.getKey(), e.getValue());
            }
        }
        log.info("***************************************");
    }
}
