package dev.blackilykat;

import java.io.IOException;

public class Console { // this is here to be able to easily make changes to all print/read calls if needed
    public static void println(String message) {
        print(message + "\n");
    }
    public static void print(String message) {
        System.out.print(message);
    }
    public static String readln() {
        StringBuilder builder = new StringBuilder();
        char c;
        try {
            while ((c = (char) System.in.read()) != '\n') builder.append(c);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return builder.toString();
    }
}
