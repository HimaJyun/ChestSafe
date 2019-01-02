package jp.jyn.chestsafe.config.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@FunctionalInterface
public interface Parser {
    String toString(Variable variable);

    default String toString(String key, String value) {
        return toString(new StringVariable().put(key, value));
    }

    default String toString(String key, Object value) {
        return toString(new StringVariable().put(key, value));
    }

    default String toString(String key, Supplier<String> value) {
        return toString(new SupplierVariable().put(key, value));
    }


    interface Variable {
        Variable put(String key, String value);

        Variable put(String key, Supplier<String> value);

        default Variable put(String key, Object value) {
            return this.put(key, value.toString());
        }

        default Variable put(String key, int value) {
            return this.put(key, String.valueOf(value));
        }

        default Variable put(String key, boolean value) {
            return this.put(key, String.valueOf(value));
        }

        Variable clear();

        String get(String key);
    }

    class EmptyVariable implements Variable {
        private final static EmptyVariable instance = new EmptyVariable();

        public static EmptyVariable getInstance() { return instance; }

        private EmptyVariable() {}

        @Override
        public Variable put(String key, String value) { return this; }

        @Override
        public Variable put(String key, Supplier<String> value) { return this; }

        @Override
        public Variable put(String key, Object value) { return this; }

        @Override
        public Variable put(String key, int value) { return this; }

        @Override
        public Variable put(String key, boolean value) { return this; }

        @Override
        public Variable clear() { return this; }

        @Override
        public String get(String key) { return null; }
    }

    class SupplierVariable implements Variable {
        private final Map<String, Supplier<String>> variable = new HashMap<>();

        @Override
        public Variable put(String key, String value) {
            return this.put(key, () -> value);
        }

        @Override
        public Variable put(String key, Supplier<String> value) {
            variable.put(key, value);
            return this;
        }

        @Override
        public Variable put(String key, Object value) {
            return this.put(key, value::toString);
        }

        @Override
        public Variable put(String key, int value) {
            return this.put(key, () -> String.valueOf(value));
        }

        @Override
        public Variable put(String key, boolean value) {
            return this.put(key, () -> String.valueOf(value));
        }

        @Override
        public Variable clear() {
            variable.clear();
            return this;
        }

        @Override
        public String get(String key) {
            Supplier<String> supplier = variable.get(key);
            if (supplier == null) {
                return null;
            }
            return supplier.get();
        }
    }

    class StringVariable implements Variable {
        private final Map<String, String> variable = new HashMap<>();

        @Override
        public Variable put(String key, String value) {
            variable.put(key, value);
            return this;
        }

        @Override
        public Variable put(String key, Supplier<String> value) {
            variable.put(key, value.get());
            return this;
        }

        @Override
        public Variable clear() {
            variable.clear();
            return this;
        }

        @Override
        public String get(String key) {
            return variable.get(key);
        }
    }
}
