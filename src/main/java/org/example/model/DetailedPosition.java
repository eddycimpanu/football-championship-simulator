package org.example.model;

public enum DetailedPosition {
    GK(FieldRole.GOALKEEPER),
    CB(FieldRole.DEFENDER), LB(FieldRole.DEFENDER), RB(FieldRole.DEFENDER),
    LWB(FieldRole.DEFENDER), RWB(FieldRole.DEFENDER),
    CDM(FieldRole.MIDFIELDER), CM(FieldRole.MIDFIELDER), CAM(FieldRole.MIDFIELDER),
    LM(FieldRole.MIDFIELDER), RM(FieldRole.MIDFIELDER),
    LW(FieldRole.FORWARD), RW(FieldRole.FORWARD),
    CF(FieldRole.FORWARD), ST(FieldRole.FORWARD);

    private final FieldRole fieldRole;

    DetailedPosition(FieldRole fieldRole) {
        this.fieldRole = fieldRole;
    }

    public FieldRole getFieldRole() {
        return fieldRole;
    }
}