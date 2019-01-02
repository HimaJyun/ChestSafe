package jp.jyn.chestsafe.config.parser;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class MessageParser implements Parser {
    private final List<Node> nodes = new ArrayList<>();
    private final ThreadLocal<StringBuilder> localBuilder = ThreadLocal.withInitial(StringBuilder::new);

    private MessageParser() {}

    public static Parser parse(String string) {
        MessageParser result = new MessageParser();
        StringBuilder buf = new StringBuilder();

        char[] value = string.toCharArray();
        for (int i = 0; i < value.length; i++) {
            switch (value[i]) {
                case '{':
                    if (buf.length() != 0) {
                        result.nodes.add(Node.stringNode(buf.toString()));
                        buf.setLength(0);
                    }
                    break;
                case '}':
                    result.nodes.add(Node.variableNode(buf.toString()));
                    buf.setLength(0);
                    break;
                case '&':
                    if (++i < value.length) {
                        ChatColor color = ChatColor.getByChar(value[i]);
                        if (color != null) {
                            // &1 &2 -> ChatColor
                            buf.append(color.toString());
                        } else {
                            switch (value[i]) {
                                case '{':
                                case '}':
                                case '&':
                                    // &{ &} && -> { } &
                                    buf.append(value[i]);
                                    break;
                                default:
                                    // &z -> &z
                                    buf.append('&');
                                    --i;
                            }
                        }
                    } else {
                        buf.append('&');
                    }
                    break;
                default:
                    buf.append(value[i]);
            }
        }
        result.nodes.add(Node.stringNode(buf.toString()));

        // include variable
        for (Node node : result.nodes) {
            if (node.type == Node.Type.VARIABLE) {
                return result;
            }
        }
        // string only
        return new RawStringParser(result.toString());
    }

    @Override
    public String toString(Variable variable) {
        StringBuilder builder = localBuilder.get();
        builder.setLength(0);

        for (Node node : nodes) {
            if (node.type == Node.Type.STRING) {
                builder.append(node.value);
            } else if (node.type == Node.Type.VARIABLE) {
                String v = variable.get(node.value);
                if (v != null) {
                    builder.append(v);
                } else {
                    // unknown variable(typo... etc)
                    builder.append('{').append(node.value).append('}');
                }
            }
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return toString(EmptyVariable.getInstance());
    }

    private static class Node {
        private enum Type {STRING, VARIABLE}

        private final Type type;
        private final String value;

        private Node(Type type, String value) {
            this.type = type;
            this.value = value;
        }

        private static Node variableNode(String value) {
            return new Node(Type.VARIABLE, value);
        }

        private static Node stringNode(String value) {
            return new Node(Type.STRING, value);
        }
    }
}
