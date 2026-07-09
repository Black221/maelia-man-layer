package sn.lhacksrt.maeliaserver.paramcatalog.domain.model;

/** Type d'un paramètre de simulation MAELIA (déduit du défaut dans launcherBase.gaml). */
public enum ParamType {
    BOOLEAN,
    INTEGER,
    FLOAT,
    STRING,
    ENUM,
    STRING_LIST;

    public static ParamType fromString(String s) {
        if (s == null) return STRING;
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STRING;
        }
    }
}
