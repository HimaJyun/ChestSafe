package jp.jyn.chestsafe.config.parser;

import org.bukkit.ChatColor;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessageParserTest {
    private Parser parser;

    @Test
    public void rawStringTest1() {
        parser = MessageParser.parse("raw string");
        assertTrue(parser instanceof RawStringParser);
        assertEquals(parser.toString(), "raw string");
    }

    @Test
    public void rawStringTest2() {
        parser = MessageParser.parse("raw &0string&r\\");
        assertTrue(parser instanceof RawStringParser);
        assertEquals(parser.toString(), "raw " + ChatColor.BLACK + "string" + ChatColor.RESET + "\\");
    }

    @Test
    public void colorCodeTest() {
        parser = MessageParser.parse("&z&0&a&r&");
        assertEquals(parser.toString(), "&z" + ChatColor.BLACK + ChatColor.GREEN + ChatColor.RESET + "&");
    }

    @Test
    public void variableTest1() {
        parser = MessageParser.parse("{test} variable");
        assertEquals(
            parser.toString(new Parser.StringVariable().put("test", "aaa")),
            "aaa variable"
        );
    }

    @Test
    public void variableTest2() {
        parser = MessageParser.parse("variable {test}");
        assertEquals(
            parser.toString(new Parser.StringVariable().put("test", "aaa")),
            "variable aaa"
        );
    }

    @Test
    public void variableTest3() {
        parser = MessageParser.parse("variable {test}");
        assertEquals(
            parser.toString(new Parser.StringVariable().put("test", 1)),
            "variable 1"
        );
    }

    @Test
    public void escapeTest() {
        // && &{ &&0 &z & &
        parser = MessageParser.parse("&& &{ &&0 &z & &");
        assertEquals(parser.toString(), "& { &0 &z & &");
    }

    @Test
    public void notFoundVariableTest() {
        parser = MessageParser.parse("variable {test}");
        assertEquals(parser.toString(), "variable {test}");
    }

    @Test
    public void brokenTest1() {
        parser = MessageParser.parse("variable {broken");
        System.out.println(parser.toString());
    }

    @Test
    public void brokenTest2() {
        parser = MessageParser.parse("variable broken}");
        System.out.println(parser.toString());
    }
}
