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
package libKonogonka.fs.TIK;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static libKonogonka.Converter.*;
/*

DON'T TRUST WIKI. Ticket size always (?) equal 0x02c0 (704 bytes)

File structure byte-by-byte parsing

Starting:
0x4 - Signature type
Signature type == 00 00 01 00 ?
Next:
0x200 - Signature size
Signature type == 01 00 01 00 ?
0x100 - Signature size
Signature type == 02 00 01 00 ?
0x3c - Signature size
Signature type == 03 00 01 00 ?
0x200 - Signature size
Signature type == 04 00 01 00 ?
0x100 - Signature size
Signature type == 05 00 01 00 ?
0x3c - Signature size
Next:
Signature type == 01 00 01 00 ?
0x3c - padding
Signature type == 01 00 01 00 ?
0x3c - padding
Signature type == 02 00 01 00 ?
0x40 - padding
Signature type == 03 00 01 00 ?
0c3c - padding
Signature type == 04 00 01 00 ?
0c3c - padding
Signature type == 05 00 01 00 ?
0x40 - padding
Next:
0x02c0 - Signature data ????? WTF? MUST BE AND IMPLEMENTED AS 0x180
 */
/**
 * TIKProvider is not a container, thus not a content-provider but provider of values-only
 * */
public class TIKProvider  {
    // Signature-related
    private final byte[] sigType;
    private final byte[] signature;
    // Ticket
    private final String Issuer;
    private final byte[] TitleKeyBlockStartingBytes;   // Actually 32 bytes.
    private final byte[] TitleKeyBlockEndingBytes;     // Everything left
    private final byte   Unknown1;
    private final byte   TitleKeyType;
    private final byte[] Unknown2;
    private final byte   MasterKeyRevision;
    private final byte[] Unknown3;
    private final byte[] TicketId;
    private final byte[] DeviceId;
    private final byte[] RightsId;
    //private byte[] RightsIdEndingBytes;
    private final byte[] AccountId;
    private final byte[] Unknown4;

    public TIKProvider(File file) throws Exception{ this(file, 0); }

    public TIKProvider(File file, long offset) throws Exception {

        if (file.length() - offset < 0x02c0)
            throw new Exception("File is too small.");

        BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()));
        if (bis.skip(offset) != offset) {
            bis.close();
            throw new Exception("Unable to skip requested range - " + offset);
        }

        sigType = new byte[0x4];
        if (bis.read(sigType) != 4) {
            bis.close();
            throw new Exception("Unable to read requested range - " + offset);
        }

        byte[] readChunk;

        switch (getLEint(sigType, 0)){
            case 65536: // RSA_4096 SHA1
            case 65539: // RSA_4096 SHA256
                readChunk = new byte[0x23c];
                if (bis.read(readChunk) != 0x23c) {
                    bis.close();
                    throw new Exception("Unable to read requested range - 0x23c");
                }
                signature = Arrays.copyOfRange(readChunk, 0, 0x200);
                break;
            case 65537: // RSA_2048 SHA1
            case 65540: // RSA_2048 SHA256
                readChunk = new byte[0x13c];
                if (bis.read(readChunk) != 0x13c) {
                    bis.close();
                    throw new Exception("Unable to read requested range - 0x13c");
                }
                signature = Arrays.copyOfRange(readChunk, 0, 0x100);
                break;
            case 65538: // ECDSA SHA1
            case 65541: // ECDSA SHA256
                readChunk = new byte[0x7c];
                if (bis.read(readChunk) != 0x7c) {
                    bis.close();
                    throw new Exception("Unable to read requested range - 0x7c");
                }
                signature = Arrays.copyOfRange(readChunk, 0, 0x3c);
                break;
            default:
                bis.close();
                throw new Exception("Unknown ticket (Signature) type. Aborting.");
        }
        // Let's read ticket body itself
        readChunk = new byte[0x180];

        if (bis.read(readChunk) != 0x180) {
            bis.close();
            throw new Exception("Unable to read requested range - Ticket data");
        }
        bis.close();

        Issuer = new String(readChunk, 0, 0x40, StandardCharsets.UTF_8);
        TitleKeyBlockStartingBytes = Arrays.copyOfRange(readChunk, 0x40, 0x50);
        TitleKeyBlockEndingBytes = Arrays.copyOfRange(readChunk, 0x50, 0x140);
        Unknown1 = readChunk[0x140];
        TitleKeyType = readChunk[0x141];
        Unknown2 = Arrays.copyOfRange(readChunk, 0x142, 0x145);
        MasterKeyRevision = readChunk[0x145];
        Unknown3 = Arrays.copyOfRange(readChunk, 0x146, 0x150);
        TicketId = Arrays.copyOfRange(readChunk, 0x150, 0x158);
        DeviceId = Arrays.copyOfRange(readChunk, 0x158, 0x160);
        RightsId = Arrays.copyOfRange(readChunk, 0x160, 0x170);
        AccountId = Arrays.copyOfRange(readChunk,0x170, 0x174);
        Unknown4 = Arrays.copyOfRange(readChunk, 0x174, 0x180);
    }

    public byte[] getSigType() { return sigType; }
    public byte[] getSignature() { return signature; }

    public String getIssuer() { return Issuer; }
    public byte[] getTitleKeyBlockStartingBytes() { return TitleKeyBlockStartingBytes; }
    public byte[] getTitleKeyBlockEndingBytes() { return TitleKeyBlockEndingBytes; }
    public byte getUnknown1() { return Unknown1; }
    public byte getTitleKeyType() { return TitleKeyType; }
    public byte[] getUnknown2() { return Unknown2; }
    public byte getMasterKeyRevision() { return MasterKeyRevision; }
    public byte[] getUnknown3() { return Unknown3; }
    public byte[] getTicketId() { return TicketId; }
    public byte[] getDeviceId() { return DeviceId; }
    public byte[] getRightsId() { return RightsId; }
    public byte[] getAccountId() { return AccountId; }
    public byte[] getUnknown4() { return Unknown4; }
}
