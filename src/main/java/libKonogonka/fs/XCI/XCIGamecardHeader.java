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
package libKonogonka.fs.XCI;

import libKonogonka.Converter;

import java.util.Arrays;

import static libKonogonka.Converter.getLEint;
import static libKonogonka.Converter.getLElong;
/**
 * Header information
 * */
public class XCIGamecardHeader{
    private final byte[] rsa2048PKCS1sig;
    private final boolean magicHead;
    private final byte[] SecureAreaStartAddr;
    private final boolean bkupAreaStartAddr;
    private final byte titleKEKIndexBoth;
    private final byte titleKEKIndex;
    private final byte KEKIndex;
    private final byte gcSize;
    private final byte gcVersion;
    private final byte gcFlags;
    private final byte[] pkgID;
    private final long valDataEndAddr;
    private final byte[] gcInfoIV;
    private final long hfs0partOffset;
    private final long hfs0headerSize;
    private final byte[] hfs0headerSHA256;
    private final byte[] hfs0initDataSHA256;
    private final int secureModeFlag;
    private final int titleKeyFlag;
    private final int keyFlag;
    private final byte[] normAreaEndAddress;

    XCIGamecardHeader(byte[] headerBytes) throws Exception{
        if (headerBytes.length != 400)
            throw new Exception("XCIGamecardHeader Incorrect array size. Expected 400 bytes while received "+headerBytes.length);
        rsa2048PKCS1sig = Arrays.copyOfRange(headerBytes, 0, 256);
        magicHead = Arrays.equals(Arrays.copyOfRange(headerBytes, 256, 260), new byte[]{0x48, 0x45, 0x41, 0x44});
        SecureAreaStartAddr = Arrays.copyOfRange(headerBytes, 260, 264);
        bkupAreaStartAddr = Arrays.equals(Arrays.copyOfRange(headerBytes, 264, 268), new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
        titleKEKIndexBoth = headerBytes[268];
            titleKEKIndex = (byte) ((titleKEKIndexBoth >> 4) & (byte) 0x0F);
            KEKIndex = (byte) (titleKEKIndexBoth & 0x0F);
        gcSize = headerBytes[269];
        gcVersion = headerBytes[270];
        gcFlags = headerBytes[271];
        pkgID = Arrays.copyOfRange(headerBytes, 272, 280);
        valDataEndAddr = getLElong(headerBytes, 280);       //TODO: FIX/simplify //
        gcInfoIV = Converter.flip(Arrays.copyOfRange(headerBytes, 288, 304));
        hfs0partOffset = getLElong(headerBytes, 304);
        hfs0headerSize = getLElong(headerBytes, 312);
        hfs0headerSHA256 = Arrays.copyOfRange(headerBytes, 320, 352);
        hfs0initDataSHA256  = Arrays.copyOfRange(headerBytes, 352, 384);
        secureModeFlag = getLEint(headerBytes, 384);
        titleKeyFlag = getLEint(headerBytes, 388);
        keyFlag = getLEint(headerBytes, 392);
        normAreaEndAddress = Arrays.copyOfRange(headerBytes, 396, 400);
    }

    public byte[] getRsa2048PKCS1sig() { return rsa2048PKCS1sig; }
    public boolean isMagicHeadOk() { return magicHead; }
    public byte[] getSecureAreaStartAddr() { return SecureAreaStartAddr; }
    public boolean isBkupAreaStartAddrOk() { return bkupAreaStartAddr; }
    public byte getTitleKEKIndexBoth() { return titleKEKIndexBoth; }
    public byte getTitleKEKIndex() { return titleKEKIndex; }
    public byte getKEKIndex() { return KEKIndex; }
    public byte getGcSize() { return gcSize; }
    public byte getGcVersion() { return gcVersion; }
    public byte getGcFlags() { return gcFlags; }
    public byte[] getPkgID() { return pkgID; }
    public long getValDataEndAddr() { return valDataEndAddr; }
    public byte[] getGcInfoIV() { return gcInfoIV; }
    public long getHfs0partOffset() { return hfs0partOffset; }
    public long getHfs0headerSize() { return hfs0headerSize; }
    public byte[] getHfs0headerSHA256() { return hfs0headerSHA256; }
    public byte[] getHfs0initDataSHA256() { return hfs0initDataSHA256; }
    public int getSecureModeFlag() { return secureModeFlag; }
    public boolean isSecureModeFlagOk(){ return secureModeFlag == 1; }
    public int getTitleKeyFlag() { return titleKeyFlag; }
    public boolean istitleKeyFlagOk(){ return titleKeyFlag == 2; }
    public int getKeyFlag() { return keyFlag; }
    public boolean iskeyFlagOk(){ return keyFlag == 0; }
    public byte[] getNormAreaEndAddr() { return normAreaEndAddress; }
}