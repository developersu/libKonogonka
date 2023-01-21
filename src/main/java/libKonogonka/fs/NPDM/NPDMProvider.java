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
package libKonogonka.fs.NPDM;

import libKonogonka.fs.NPDM.ACI0.ACI0Provider;
import libKonogonka.fs.NPDM.ACID.ACIDProvider;
import libKonogonka.aesctr.InFileStreamProducer;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.Converter.*;

public class NPDMProvider{

    private final String magicNum;
    private final byte[] reserved1;
    private final byte MMUFlags;
    private final byte reserved2;
    private final byte mainThreadPrio;
    private final byte mainThreadCoreNum;
    private final byte[] reserved3;
    private final int personalMmHeapSize;     // safe-to-store
    private final int version;                // safe?
    private final long mainThreadStackSize;    // TODO: check if safe
    private final String titleName;
    private final byte[] productCode;
    private final byte[] reserved4;
    private final int aci0offset;            // originally 4-bytes (u-int)
    private final int aci0size;              // originally 4-bytes (u-int)
    private final int acidOffset;            // originally 4-bytes (u-int)
    private final int acidSize;              // originally 4-bytes (u-int)

    private ACI0Provider aci0;
    private ACIDProvider acid;

    public NPDMProvider(File file) throws Exception {
        this(file, 0);
    }
    public NPDMProvider(File file, long offset) throws Exception {
        this(new InFileStreamProducer(file, offset));
    }
    public NPDMProvider(InFileStreamProducer producer) throws Exception{
        try (BufferedInputStream stream = producer.produce()) {
            byte[] mainBuf = new byte[0x80];
            if (stream.read(mainBuf) != 0x80)
                throw new Exception("NPDMProvider: Failed to read 'META'");
            aci0offset = getLEint(mainBuf, 0x70);
            aci0size = getLEint(mainBuf, 0x74);
            acidOffset = getLEint(mainBuf, 0x78);
            acidSize = getLEint(mainBuf, 0x7C);

            if (aci0offset < acidOffset) {
                calculateACI0(stream, aci0offset - 0x80);
                calculateACID(stream, acidOffset - aci0offset - aci0size);
            } else {
                calculateACID(stream, acidOffset - 0x80);
                calculateACI0(stream, aci0offset - acidOffset - acidSize);
            }
            magicNum = new String(mainBuf, 0, 4, StandardCharsets.UTF_8);
            reserved1 = Arrays.copyOfRange(mainBuf, 0x4, 0xC);
            MMUFlags = mainBuf[0xC];
            reserved2 = mainBuf[0xD];
            mainThreadPrio = mainBuf[0xE];
            mainThreadCoreNum = mainBuf[0xF];
            reserved3 = Arrays.copyOfRange(mainBuf, 0x10, 0x14);
            personalMmHeapSize = getLEint(mainBuf, 0x14);
            version = getLEint(mainBuf, 0x18);
            mainThreadStackSize = getLElongOfInt(mainBuf, 0x1C);
            titleName = new String(mainBuf, 0x20, 0x10, StandardCharsets.UTF_8);
            productCode = Arrays.copyOfRange(mainBuf, 0x30, 0x40);
            reserved4 = Arrays.copyOfRange(mainBuf, 0x40, 0x70);
        }
    }

    private void calculateACID(BufferedInputStream stream, int toSkip) throws Exception{
        byte[] acidBuf = new byte[acidSize];
        if (stream.skip(toSkip) != toSkip)
            throw new Exception("NPDMProvider: Failed to skip bytes till 'ACID'");
        if (acidSize != stream.read(acidBuf))
            throw new Exception("NPDMProvider: Failed to read 'ACID'");
        acid = new ACIDProvider(acidBuf);
    }
    private void calculateACI0(BufferedInputStream stream, int toSkip) throws Exception{
        byte[] aci0Buf = new byte[aci0size];
        if (stream.skip(toSkip) != toSkip)
            throw new Exception("NPDMProvider: Failed to skip bytes till 'ACI0'");
        if (aci0size != stream.read(aci0Buf))
            throw new Exception("NPDMProvider: Failed to read 'ACI0'");
        aci0 = new ACI0Provider(aci0Buf);
    }

    public String getMagicNum() { return magicNum; }
    public byte[] getReserved1() { return reserved1; }
    public byte getMMUFlags() { return MMUFlags; }
    public byte getReserved2() { return reserved2; }
    public byte getMainThreadPrio() { return mainThreadPrio; }
    public byte getMainThreadCoreNum() { return mainThreadCoreNum; }
    public byte[] getReserved3() { return reserved3; }
    public int getPersonalMmHeapSize() { return personalMmHeapSize; }
    public int getVersion() { return version; }
    public long getMainThreadStackSize() { return mainThreadStackSize; }
    public String getTitleName() { return titleName; }
    public byte[] getProductCode() { return productCode; }
    public byte[] getReserved4() { return reserved4; }
    public int getAci0offset() { return aci0offset; }
    public int getAci0size() { return aci0size; }
    public int getAcidOffset() { return acidOffset; }
    public int getAcidSize() { return acidSize; }

    public ACI0Provider getAci0() { return aci0; }
    public ACIDProvider getAcid() { return acid; }
}
