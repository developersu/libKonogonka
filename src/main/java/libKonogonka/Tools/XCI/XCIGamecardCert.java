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
package libKonogonka.Tools.XCI;

import libKonogonka.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * Gamecard Cert
 * */
public class XCIGamecardCert {
    private final static Logger log = LogManager.getLogger(XCIGamecardCert.class);

    private final byte[] rsa2048PKCS1sig;
    private final byte[] magicCert;
    private final byte[] unknown1;
    private final byte kekIndex;
    private final byte[] unknown2;
    private final byte[] deviceID;
    private final byte[] unknown3;
    private final byte[] encryptedData;

    XCIGamecardCert(byte[] certBytes) throws Exception{
        if (certBytes.length != 512)
            throw new Exception("XCIGamecardCert Incorrect array size. Expected 512 bytes while received "+certBytes.length);
        rsa2048PKCS1sig = Arrays.copyOfRange(certBytes, 0, 256);
        magicCert = Arrays.copyOfRange(certBytes, 256, 260);
        unknown1 = Arrays.copyOfRange(certBytes, 260, 264);
        kekIndex = certBytes[264];
        unknown2 = Arrays.copyOfRange(certBytes, 265, 272);
        deviceID = Arrays.copyOfRange(certBytes, 272, 288);
        unknown3 = Arrays.copyOfRange(certBytes, 288, 298);
        encryptedData = Arrays.copyOfRange(certBytes, 298, 512);
    }
    public byte[] getRsa2048PKCS1sig() { return rsa2048PKCS1sig; }
    public byte[] getMagicCert() { return magicCert; }
    public boolean isMagicCertOk(){ return Arrays.equals(magicCert, new byte[]{0x48, 0x45, 0x41, 0x44}); }
    public byte[] getUnknown1() { return unknown1; }
    public byte getKekIndex() { return kekIndex; }
    public byte[] getUnknown2() { return unknown2; }
    public byte[] getDeviceID() { return deviceID; }
    public byte[] getUnknown3() { return unknown3; }
    public byte[] getEncryptedData() { return encryptedData; }

    public void printDebug(){
        log.debug("== XCIGamecardCert ==\n" +
                "rsa2048PKCS1sig  " + Converter.byteArrToHexStringAsLE(rsa2048PKCS1sig) + "\n" +
                "magicCert        " + Converter.byteArrToHexStringAsLE(magicCert) + "\n" +
                "unknown1         " + Converter.byteArrToHexStringAsLE(unknown1) + "\n" +
                "kekIndex         " + kekIndex + "\n" +
                "unknown2         " + Converter.byteArrToHexStringAsLE(unknown2) + "\n" +
                "deviceID         " + Converter.byteArrToHexStringAsLE(deviceID) + "\n" +
                "unknown3         " + Converter.byteArrToHexStringAsLE(unknown3) + "\n" +
                "encryptedData    " + Converter.byteArrToHexStringAsLE(encryptedData) + "\n"
        );
    }
}
