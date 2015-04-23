package alex.bobro.genericdao.entities;


public enum ForeignKeyActions {
    CASCADE("CASCADE"), RESTRICT("RESTRICT"), NO_ACTION("NO ACTION"), SET_NULL("SET NULL"), SET_DEFAULT("SET DEFAULT");

    private ForeignKeyActions(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }
}
