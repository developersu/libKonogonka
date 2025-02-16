/*
    Copyright 2019-2023 Dmitry Isaenko

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
package libKonogonka.fs.other.System2;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KernelMap {
    private final static Logger log = LogManager.getLogger(KernelMap.class);

    private int textStartOffset;
    private int textEndOffset;
    private int rodataStartOffset;
    private int rodataEndOffset;
    private int dataStartOffset;
    private int dataEndOffset;
    private int bssStartOffset;
    private int bssEndOffset;
    private int ini1Offset;
    private int dynamicOffset;
    private int initArrayStartOffset;
    private int initArrayEndOffset;
    private int systemRegistersOffset; // 17.0.0+

    private KernelMap() { }

    /**
     * Construct KernelMap
     *
     * @return null if mapBytes is an invalid data set and KernelMap object otherwise
     */
    public static KernelMap constructKernelMap(byte[] mapBytes, int offset, int maxSize) {
        KernelMap kernelMap = new KernelMap();
        kernelMap.textStartOffset = Converter.getLEint(mapBytes, offset);
        if (kernelMap.textStartOffset != 0)
            return null;
        kernelMap.textEndOffset = Converter.getLEint(mapBytes, offset + 0x4);
        kernelMap.rodataStartOffset = Converter.getLEint(mapBytes, offset + 0x8);
        kernelMap.rodataEndOffset = Converter.getLEint(mapBytes, offset + 0xC);
        kernelMap.dataStartOffset = Converter.getLEint(mapBytes, offset + 0x10);
        kernelMap.dataEndOffset = Converter.getLEint(mapBytes, offset + 0x14);
        kernelMap.bssStartOffset = Converter.getLEint(mapBytes, offset + 0x18);
        kernelMap.bssEndOffset = Converter.getLEint(mapBytes, offset + 0x1C);
        kernelMap.ini1Offset = Converter.getLEint(mapBytes, offset + 0x20); // 0x08d000
        kernelMap.dynamicOffset = Converter.getLEint(mapBytes, offset + 0x24);
        kernelMap.initArrayStartOffset = Converter.getLEint(mapBytes, offset + 0x28);
        kernelMap.initArrayEndOffset = Converter.getLEint(mapBytes, offset + 0x2C);
        kernelMap.systemRegistersOffset = Converter.getLEint(mapBytes, offset + 0x30);

        // taken from hactool
        if (kernelMap.textStartOffset >= kernelMap.textEndOffset)
            return null;
        if ((kernelMap.textEndOffset & 0xFFF) > 0)
            return null;
        if (kernelMap.textEndOffset > kernelMap.rodataStartOffset)
            return null;
        if ((kernelMap.rodataStartOffset & 0xFFF) > 0)
            return null;
        if (kernelMap.rodataStartOffset >= kernelMap.rodataEndOffset)
            return null;
        if ((kernelMap.rodataEndOffset & 0xFFF) > 0)
            return null;
        if (kernelMap.rodataEndOffset > kernelMap.dataStartOffset)
            return null;
        if ((kernelMap.dataStartOffset & 0xFFF) > 0)
            return null;
        if (kernelMap.dataStartOffset >= kernelMap.dataEndOffset)
            return null;
        if (kernelMap.dataEndOffset > kernelMap.bssStartOffset)
            return null;
        if (kernelMap.bssStartOffset > kernelMap.bssEndOffset)
            return null;
        if (kernelMap.bssEndOffset > kernelMap.ini1Offset)
            return null;
        /*
        if (kernelMap.ini1Offset > maxSize - 0x80)
            return null;
        */
        System.out.println("FOUND AT:" + RainbowDump.formatDecHexString(offset));
        kernelMap.printDebug();
        return kernelMap;
    }

    public int getTextStartOffset() { return textStartOffset; }
    public int getTextEndOffset() { return textEndOffset; }
    public int getRodataStartOffset() { return rodataStartOffset; }
    public int getRodataEndOffset() { return rodataEndOffset; }
    public int getDataStartOffset() { return dataStartOffset; }
    public int getDataEndOffset() { return dataEndOffset; }
    public int getBssStartOffset() { return bssStartOffset; }
    public int getBssEndOffset() { return bssEndOffset; }
    public int getIni1Offset() { return ini1Offset; }
    public int getDynamicOffset() { return dynamicOffset; }
    public int getInitArrayStartOffset() { return initArrayStartOffset; }
    public int getInitArrayEndOffset() { return initArrayEndOffset; }

    public void printDebug() {
        log.debug("_ Kernel map _\n" +
                "  .text Start Offset                    " + RainbowDump.formatDecHexString(textStartOffset) + "\n" +
                "  .text End Offset                      " + RainbowDump.formatDecHexString(textEndOffset) + "\n" +
                "  .rodata Start Offset                  " + RainbowDump.formatDecHexString(rodataStartOffset) + "\n" +
                "  .rodata End Offset                    " + RainbowDump.formatDecHexString(rodataEndOffset) + "\n" +
                "  .data Start Offset                    " + RainbowDump.formatDecHexString(dataStartOffset) + "\n" +
                "  .data End Offset                      " + RainbowDump.formatDecHexString(dataEndOffset) + "\n" +
                "  .bss Start Offset                     " + RainbowDump.formatDecHexString(bssStartOffset) + "\n" +
                "  .bss End Offset                       " + RainbowDump.formatDecHexString(bssEndOffset) + "\n" +
                "  INI1 Offset                           " + RainbowDump.formatDecHexString(ini1Offset) + "\n" +
                "  Dynamic Offset                        " + RainbowDump.formatDecHexString(dynamicOffset) + "\n" +
                "  Init array Start Offset               " + RainbowDump.formatDecHexString(initArrayStartOffset) + "\n" +
                "  Init array End Offset                 " + RainbowDump.formatDecHexString(initArrayEndOffset) + "\n" +
                "  System registers offset (FW 17.0.0+)  " + RainbowDump.formatDecHexString(systemRegistersOffset));
    }
}
