package gg;

class Sensor {

    final String id;
    private final String description;

    Sensor(String id, String description) {
        this.id = id;
        this.description = description;
    }

    String encode() {
        return id + "$" + description;
    }

    static Sensor decode(String s){
        String[] split = s.split("\\$");
        return new Sensor(split[0], split[1]);

    }
}
