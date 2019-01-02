package jp.jyn.chestsafe.config.parser;

public class RawStringParser implements Parser {
    private final String string;

    public RawStringParser(CharSequence string) {
        this.string = string.toString();
    }

    @Override
    public String toString(Variable variable) {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }
}
