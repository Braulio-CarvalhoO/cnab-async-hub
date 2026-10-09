package br.com.cnab.hub.cnab;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class CnabLines {

    private CnabLines() {
    }

    public static List<String> split(byte[] content) {
        String text = new String(content, StandardCharsets.ISO_8859_1)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        return Arrays.stream(text.split("\n"))
                .map(String::stripTrailing)
                .filter(line -> !line.isBlank())
                .toList();
    }

    public static int dominantWidth(List<String> lines) {
        long width240 = lines.stream().filter(line -> line.length() == 240).count();
        long width400 = lines.stream().filter(line -> line.length() == 400).count();
        return width400 > width240 ? 400 : 240;
    }
}
