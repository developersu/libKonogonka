/*
    Copyright 2019-2025 Dmitry Isaenko

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

        if (isValid(kernelMap, maxSize))
            return kernelMap;

        return null;
    }

    public static KernelMap constructKernelMap17(byte[] mapBytes, int offset, int branchTarget, int maxSize) {
        KernelMap kernelMap = new KernelMap();
        final int realOffset = offset + branchTarget;
        kernelMap.textStartOffset = Converter.getLEint(mapBytes, offset)+realOffset;
        kernelMap.textEndOffset = Converter.getLEint(mapBytes, offset + 0x4)+realOffset;
        kernelMap.rodataStartOffset = Converter.getLEint(mapBytes, offset + 0x8)+realOffset;
        kernelMap.rodataEndOffset = Converter.getLEint(mapBytes, offset + 0xC)+realOffset;
        kernelMap.dataStartOffset = Converter.getLEint(mapBytes, offset + 0x10)+realOffset;
        kernelMap.dataEndOffset = Converter.getLEint(mapBytes, offset + 0x14)+realOffset;
        kernelMap.bssStartOffset = Converter.getLEint(mapBytes, offset + 0x18)+realOffset;
        kernelMap.bssEndOffset = Converter.getLEint(mapBytes, offset + 0x1C)+realOffset;
        kernelMap.ini1Offset = Converter.getLEint(mapBytes, offset + 0x20)+realOffset; // 0x08d000
        kernelMap.dynamicOffset = Converter.getLEint(mapBytes, offset + 0x24)+realOffset;
        kernelMap.initArrayStartOffset = Converter.getLEint(mapBytes, offset + 0x28)+realOffset;
        kernelMap.initArrayEndOffset = Converter.getLEint(mapBytes, offset + 0x2C)+realOffset;
        kernelMap.systemRegistersOffset = Converter.getLEint(mapBytes, offset + 0x30)+realOffset;

        if (isValid(kernelMap, maxSize))
            return kernelMap;
        
        return null;
    }

    private static boolean isValid(KernelMap kernelMap, int maxSize){
        // taken from hactool
        if (kernelMap.textStartOffset != 0)
            return false;
        if (kernelMap.textStartOffset >= kernelMap.textEndOffset)
            return false;
        if ((kernelMap.textEndOffset & 0xFFF) > 0)
            return false;
        if (kernelMap.textEndOffset > kernelMap.rodataStartOffset)
            return false;
        if ((kernelMap.rodataStartOffset & 0xFFF) > 0)
            return false;
        if (kernelMap.rodataStartOffset >= kernelMap.rodataEndOffset)
            return false;
        if ((kernelMap.rodataEndOffset & 0xFFF) > 0)
            return false;
        if (kernelMap.rodataEndOffset > kernelMap.dataStartOffset)
            return false;
        if ((kernelMap.dataStartOffset & 0xFFF) > 0)
            return false;
        if (kernelMap.dataStartOffset >= kernelMap.dataEndOffset)
            return false;
        if (kernelMap.dataEndOffset > kernelMap.bssStartOffset)
            return false;
        if (kernelMap.bssStartOffset > kernelMap.bssEndOffset)
            return false;
        if (kernelMap.bssEndOffset > kernelMap.ini1Offset)
            return false;
        if (kernelMap.ini1Offset > maxSize - 0x80)
            return false;

        return true;
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
