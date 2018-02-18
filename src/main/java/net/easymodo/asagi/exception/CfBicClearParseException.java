package net.easymodo.asagi.exception;

public class CfBicClearParseException extends Exception {
    private static final long serialVersionUID = -8123637445404320429L;

    public CfBicClearParseException(Throwable e) {
        super(e);
    }

    public CfBicClearParseException(String s) {
        super(s);
    }

    public CfBicClearParseException(String s, Throwable c) {
        super(s, c);
    }
}