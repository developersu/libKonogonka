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

/**
 * Debug tool like hexdump <3
 */
public class RainbowDump {
    private final static Logger log = LogManager.getLogger(Converter.class);

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    private static StringBuilder stringBuilder;
    public static void hexDumpUTF8(byte[] byteArray){
        stringBuilder = new StringBuilder(" -- RainbowDump --\n");
        if (byteArray == null || byteArray.length == 0)
            return;

        int k = 0;
        stringBuilder.append(String.format("%s%08x  %s", ANSI_BLUE, 0, ANSI_RESET));
        for (int i = 0; i < byteArray.length; i++) {
            if (k == 8)
                stringBuilder.append("  ");
            if (k == 16){
                stringBuilder.append(ANSI_GREEN+"| "+ANSI_RESET);
                printChars(byteArray, i);
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
        stringBuilder.append(ANSI_GREEN+"| "+ANSI_RESET);
        printChars(byteArray, byteArray.length);
        stringBuilder.append("\n")
                .append(ANSI_RESET)
                .append(new String(byteArray, StandardCharsets.UTF_8))
                .append("\n");

        log.debug(stringBuilder.toString());
    }

    private static void printChars(byte[] byteArray, int pointer){
        for (int j = pointer-16; j < pointer; j++){
            if ((byteArray[j] > 21) && (byteArray[j] < 126)) // man ascii
                stringBuilder.append((char) byteArray[j]);
            else if (byteArray[j] == 0x0a)
                stringBuilder.append("↲"); //"␤"
            else if (byteArray[j] == 0x0d)
                stringBuilder.append("←"); // "␍"
            else
                stringBuilder.append(".");
        }
    }


    public static void hexDumpUTF8Legacy(byte[] byteArray){
        if (byteArray == null || byteArray.length == 0)
            return;
        System.out.print(ANSI_BLUE);
        for (int i=0; i < byteArray.length; i++)
            System.out.printf("%02d-", i%100);
        System.out.println(">"+ANSI_RED+byteArray.length+ANSI_RESET);
        for (byte b: byteArray)
            System.out.printf("%02x ", b);
        System.out.println();
        System.out.print(new String(byteArray, StandardCharsets.UTF_8)+"\n");
    }

    public static void binDumpInt(int value){
        log.debug(Converter.intToBinaryString(value));
    }

    public static void binDumpLong(long value){
        System.out.println(String.format("%64s", Long.toBinaryString( value )).replace(' ', '0')+" | "+value);
    }

    public static String formatDecHexString(long value){
        return String.format("%-20d 0x%x", value, value);
    }
    public static String formatDecHexString(int value){
        return String.format("%-20d 0x%x", value, value);
    }
}
