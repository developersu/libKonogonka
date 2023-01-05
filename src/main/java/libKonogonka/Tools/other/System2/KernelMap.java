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
package libKonogonka.Tools.other.System2;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KernelMap {
    private final static Logger log = LogManager.getLogger(KernelMap.class);
    
    private final int textStartOffset;
    private final int textEndOffset;
    private final int rodataStartOffset;
    private final int rodataEndOffset;
    private final int dataStartOffset;
    private final int dataEndOffset;
    private final int bssStartOffset;
    private final int bssEndOffset;
    private final int ini1Offset;
    private final int dynamicOffset;
    private final int initArrayStartOffset;
    private final int initArrayEndOffset;

    public KernelMap(byte[] mapBytes, int offset){
        textStartOffset = Converter.getLEint(mapBytes, offset);
        textEndOffset = Converter.getLEint(mapBytes, offset + 0x4);
        rodataStartOffset = Converter.getLEint(mapBytes, offset + 0x8);
        rodataEndOffset = Converter.getLEint(mapBytes, offset + 0xC);
        dataStartOffset = Converter.getLEint(mapBytes, offset + 0x10);
        dataEndOffset = Converter.getLEint(mapBytes, offset + 0x14);
        bssStartOffset = Converter.getLEint(mapBytes, offset + 0x18);
        bssEndOffset = Converter.getLEint(mapBytes, offset + 0x1C);
        ini1Offset = Converter.getLEint(mapBytes, offset + 0x20);
        dynamicOffset = Converter.getLEint(mapBytes, offset + 0x24);
        initArrayStartOffset = Converter.getLEint(mapBytes, offset + 0x28);
        initArrayEndOffset = Converter.getLEint(mapBytes, offset + 0x2C);
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

    //taken from hactool
    public boolean isValid(long maxSize) { // section0 size
        if (textStartOffset != 0)
            return false;
        if (textStartOffset >= textEndOffset)            
            return false;
        if ((textEndOffset & 0xFFF) > 0)
            return false;
        if (textEndOffset > rodataStartOffset)          
            return false;
        if ((rodataStartOffset & 0xFFF) > 0)
            return false;
        if (rodataStartOffset >= rodataEndOffset)        
            return false;
        if ((rodataEndOffset & 0xFFF) > 0)
            return false;
        if (rodataEndOffset > dataStartOffset)           
            return false;
        if ((dataStartOffset & 0xFFF) > 0)
            return false;
        if (dataStartOffset >= dataEndOffset)            
            return false;
        if (dataEndOffset > bssStartOffset)              
            return false;
        if (bssStartOffset > bssEndOffset)               
            return false;
        if (bssEndOffset > ini1Offset)
            return false;
        if (ini1Offset > maxSize - 0x80)
            return false;

        return true;
    }
    public void printDebug(){
        log.debug("_ Kernel map _\n" +
                "  .text Start Offset            " + RainbowDump.formatDecHexString(textStartOffset) + "\n" +
                "  .text End Offset              " + RainbowDump.formatDecHexString(textEndOffset) + "\n" +
                "  .rodata Start Offset          " + RainbowDump.formatDecHexString(rodataStartOffset) + "\n" +
                "  .rodata End Offset            " + RainbowDump.formatDecHexString(rodataEndOffset) + "\n" +
                "  .data Start Offset            " + RainbowDump.formatDecHexString(dataStartOffset) + "\n" +
                "  .data End Offset              " + RainbowDump.formatDecHexString(dataEndOffset) + "\n" +
                "  .bss Start Offset             " + RainbowDump.formatDecHexString(bssStartOffset) + "\n" +
                "  .bss End Offset               " + RainbowDump.formatDecHexString(bssEndOffset) + "\n" +
                "  INI1 Offset                   " + RainbowDump.formatDecHexString(ini1Offset) + "\n" +
                "  Dynamic Offset                " + RainbowDump.formatDecHexString(dynamicOffset) + "\n" +
                "  Init array Start Offset       " + RainbowDump.formatDecHexString(initArrayStartOffset) + "\n" +
                "  Init array End Offset         " + RainbowDump.formatDecHexString(initArrayEndOffset));
    }
}
