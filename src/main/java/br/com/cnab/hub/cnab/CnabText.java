package br.com.cnab.hub.cnab;

public final class CnabText {

    private CnabText() {
    }

    public static String numeric(String value, int length) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() > length) {
            return digits.substring(digits.length() - length);
        }
        return "0".repeat(length - digits.length()) + digits;
    }

    public static String alpha(String value, int length) {
        String text = value == null ? "" : value;
        if (text.length() > length) {
            return text.substring(0, length);
        }
        return text + " ".repeat(length - text.length());
    }

    public static String slice(String line, int startInclusive, int endInclusive) {
        return line.substring(startInclusive - 1, endInclusive).trim();
    }

    public static String money(long cents, int length) {
        return numeric(Long.toString(cents), length);
    }
}
