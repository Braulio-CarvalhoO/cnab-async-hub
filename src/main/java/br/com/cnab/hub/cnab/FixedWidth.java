package br.com.cnab.hub.cnab;

final class FixedWidth {

    private final char[] chars;

    FixedWidth(int length) {
        this.chars = new char[length];
        java.util.Arrays.fill(chars, ' ');
    }

    FixedWidth put(int startInclusive, String value) {
        for (int i = 0; i < value.length() && startInclusive - 1 + i < chars.length; i++) {
            chars[startInclusive - 1 + i] = value.charAt(i);
        }
        return this;
    }

    String line() {
        return new String(chars);
    }
}
