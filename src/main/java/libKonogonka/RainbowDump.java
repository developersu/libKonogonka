/*
    Copyright 2019-2022 Dmitry Isaenko

    This file is part of libKonogonka.

    libKonogonka is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    libKonogonka is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with libKonogonka.  If not, see <https://www.gnu.org/licenses/>.
*/
package libKonogonka;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
/*  ANSI_BLACK = "\u001B[30m";
    ANSI_YELLOW = "\u001B[33m";
    ANSI_PURPLE = "\u001B[35m";
    ANSI_CYAN = "\u001B[36m";
    ANSI_WHITE = "\u001B[37m"; */
/**
 * Debug tool like hexdump <3
 */
public class RainbowDump {
    private final static Logger log = LogManager.getLogger(Converter.class);

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";

    private static StringBuilder stringBuilder;
    public static void hexDumpUTF8(byte[] byteArray) {
        stringBuilder = new StringBuilder(" -- RainbowDump --\n");
        if (byteArray == null || byteArray.length == 0)
            return;

        int k = 0;
        boolean lastCharCyrillic = false;
        stringBuilder.append(String.format("%s%08x  %s", ANSI_BLUE, 0, ANSI_RESET));
        for (int i = 0; i < byteArray.length; i++) {
            if (k == 8)
                stringBuilder.append("  ");
            if (k == 16) {
                stringBuilder.append(ANSI_GREEN + "| " + ANSI_RESET);
                lastCharCyrillic = printChars(byteArray, i, lastCharCyrillic);
                stringBuilder.append("\n")
                        .append(String.format("%s%08x  %s", ANSI_BLUE, i, ANSI_RESET));
                k = 0;
            }
            stringBuilder.append(String.format("%02x ", byteArray[i]));
            k++;
        }
        int paddingSize = 16 - (byteArray.length % 16);
        if (paddingSize != 16) {
            for (int i = 0; i < paddingSize; i++) {
                stringBuilder.append("   ");
            }
            if (paddingSize > 7) {
                stringBuilder.append("  ");
            }
        }
        stringBuilder.append(ANSI_GREEN + "| " + ANSI_RESET);
        printChars(byteArray, byteArray.length);
        stringBuilder.append("\n")
                .append(ANSI_RESET)
                .append(new String(byteArray, StandardCharsets.UTF_8))
                .append("\n");

        log.debug(stringBuilder.toString());
    }

    private static void printChars(byte[] byteArray, int pointer){
        printChars(byteArray, pointer, false);
    }
    private static boolean printChars(byte[] byteArray, int pointer, boolean skipFirstByte){
        int j;
        if (pointer < 16)
            j = 0;
        else
            j = pointer-16;

        int utf8val = 0;
        if (skipFirstByte){
            ++j;
            stringBuilder.append(" ");
        }

        for (; j < pointer; j++){
            utf8val = 0;

            if (byteArray.length > (j+1))
                utf8val = ((byteArray[j] & 0xff) << 8) | (byteArray[j+1] & 0xff);

            if ((byteArray[j] > 21) && (byteArray[j] < 126)) // man ascii
                stringBuilder.append((char) byteArray[j]);
            else if (byteArray[j] == 0x0a)
                stringBuilder.append("↲"); //"␤"
            else if (byteArray[j] == 0x0d)
                stringBuilder.append("←"); // "␍"
            else if (utf8val >= 0xd080 && utf8val <= 0xd3bf){
                byte[] arr = new byte[0x2];
                System.arraycopy(byteArray, j, arr, 0, 2);
                stringBuilder.append(new String(arr, StandardCharsets.UTF_8)+" ");
                ++j;
            }
            else
                stringBuilder.append(".");
        }

        return (utf8val >= 0xd080 && utf8val <= 0xd3bf && j > pointer);
    }

    public static void hexDumpUTF8Legacy(byte[] byteArray){
        StringBuilder stringBuilderLegacy = new StringBuilder("HexDumpUTF8Legacy");
        stringBuilderLegacy.append(ANSI_BLUE);

        if (byteArray == null || byteArray.length == 0)
            return;

        for (int i=0; i < byteArray.length; i++)
            stringBuilderLegacy.append(String.format("%02d-", i%100));
        stringBuilderLegacy.append(">" + ANSI_RED).append(byteArray.length).append(ANSI_RESET).append("\n");
        for (byte b: byteArray)
            stringBuilderLegacy.append(String.format("%02x ", b));
        stringBuilderLegacy.append("\n")
                .append(new String(byteArray, StandardCharsets.UTF_8))
                .append("\n");
        log.debug(stringBuilderLegacy.toString());
    }

    public static void binDumpInt(int value){
        log.debug(Converter.intToBinaryString(value));
    }

    public static void binDumpLong(long value){
        log.debug(String.format("%64s", Long.toBinaryString( value )).replace(' ', '0')+" | "+value);
    }

    public static String formatDecHexString(long value){
        return String.format("%1$-20d %1$#x", value);
    }
    public static String formatDecHexString(int value){
        return String.format("%1$-20d %1$#x", value);
    }
}
