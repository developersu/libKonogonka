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
package libKonogonka.Tools.PFS0;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import libKonogonka.ctraes.AesCtrBufferedInputStream;
import libKonogonka.ctraes.AesCtrDecryptSimple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static libKonogonka.Converter.*;

public class PFS0Provider implements IPFS0Provider{
    private final static Logger log = LogManager.getLogger(PFS0Provider.class);

    private long rawFileDataStartOffset;

    private String magic;
    private int filesCount;
    private int stringTableSize;
    private byte[] padding;
    private PFS0subFile[] pfs0subFiles;

    private final File file;
    private final long offsetPosition;          // Where data starts, excluding header, string table etc.
    private long mediaStartOffset;
    private long mediaEndOffset;
    private AesCtrDecryptSimple decryptor;

    private final boolean encrypted;

    public PFS0Provider(File fileWithPfs0,
                        long offsetPosition,
                        long mediaStartOffset,
                        long mediaEndOffset,
                        AesCtrDecryptSimple decryptor) throws Exception{
        this.file = fileWithPfs0;
        this.offsetPosition = offsetPosition + mediaStartOffset*0x200;
        this.encrypted = true;

        this.mediaStartOffset = mediaStartOffset;
        this.mediaEndOffset = mediaEndOffset;
        this.decryptor = decryptor;
        proceedPfs0();
    }

    public PFS0Provider(File fileWithPfs0) throws Exception{ this(fileWithPfs0, 0); }

    public PFS0Provider(File fileWithPfs0, long offsetPosition) throws Exception{
        this.file = fileWithPfs0;
        this.offsetPosition = offsetPosition;
        this.encrypted = false;
        //bufferedInputStream = new BufferedInputStream(Files.newInputStream(fileWithPfs0.toPath()));
        proceedPfs0();
    }
    private void proceedPfs0() throws Exception{
        BufferedInputStream bufferedInputStream;

        if (encrypted) {
            bufferedInputStream = new AesCtrBufferedInputStream(decryptor,
                    offsetPosition,
                    mediaStartOffset,
                    mediaEndOffset,
                    Files.newInputStream(file.toPath()));
        }
        else{
            bufferedInputStream = new BufferedInputStream(Files.newInputStream(file.toPath()));
        }

        if (offsetPosition != bufferedInputStream.skip(offsetPosition))
            throw new Exception("PFS0Provider: Unable to skip initial offset: "+offsetPosition);

        byte[] fileStartingBytes = new byte[0x10];
        // Read PFS0Provider, files count, header, padding (4 zero bytes)
        if (bufferedInputStream.read(fileStartingBytes) != 0x10){
            throw new Exception("PFS0Provider: Unable to read starting bytes");
        }
        rawFileDataStartOffset += 0x10;
        // Check PFS0Provider
        magic = new String(fileStartingBytes, 0x0, 0x4, StandardCharsets.US_ASCII);
        if (! magic.equals("PFS0")){
            throw new Exception("PFS0Provider: Bad magic");
        }
        // Get files count
        filesCount = getLEint(fileStartingBytes, 0x4);
        if (filesCount <= 0 ) {
            throw new Exception("PFS0Provider: Files count is too small");
        }
        // Get string table
        stringTableSize = getLEint(fileStartingBytes, 0x8);
        if (stringTableSize <= 0 ){
            throw new Exception("PFS0Provider: String table is too small");
        }
        padding = Arrays.copyOfRange(fileStartingBytes, 0xc, 0x10);
        //---------------------------------------------------------------------------------------------------------
        pfs0subFiles = new PFS0subFile[filesCount];

        long[] offsetsSubFiles = new long[filesCount];
        long[] sizesSubFiles = new long[filesCount];
        int[] strTableOffsets = new int[filesCount];
        byte[][] zeroBytes = new byte[filesCount][];

        byte[] fileEntryTable = new byte[0x18];
        for (int i=0; i<filesCount; i++){
            if (bufferedInputStream.read(fileEntryTable) != 0x18)
                throw new Exception("PFS0Provider: String table is too small");
            offsetsSubFiles[i] = getLElong(fileEntryTable, 0);
            sizesSubFiles[i] = getLElong(fileEntryTable, 0x8);
            strTableOffsets[i] = getLEint(fileEntryTable, 0x10);
            zeroBytes[i] = Arrays.copyOfRange(fileEntryTable, 0x14, 0x18);
            rawFileDataStartOffset += 0x18;
        }
        //**********************************************************************************************************
        // In here pointer in front of String table
        String[] subFileNames = new String[filesCount];
        byte[] stringTbl = new byte[stringTableSize];
        if (bufferedInputStream.read(stringTbl) != stringTableSize){
            throw new Exception("Read PFS0Provider String table failure. Can't read requested string table size ("+stringTableSize+")");
        }
        rawFileDataStartOffset += stringTableSize;
        for (int i=0; i < filesCount; i++){
            int j = 0;
            while (stringTbl[strTableOffsets[i]+j] != (byte)0x00)
                j++;
            subFileNames[i] = new String(stringTbl, strTableOffsets[i], j, StandardCharsets.UTF_8);
        }
        for (int i = 0; i < filesCount; i++){
            pfs0subFiles[i] = new PFS0subFile(
                    subFileNames[i],
                    offsetsSubFiles[i],
                    sizesSubFiles[i],
                    zeroBytes[i]
            );
        }
        bufferedInputStream.close();
    }

    @Override
    public boolean isEncrypted() { return encrypted; }
    @Override
    public String getMagic() { return magic; }
    @Override
    public int getFilesCount() { return filesCount; }
    @Override
    public int getStringTableSize() { return stringTableSize; }
    @Override
    public byte[] getPadding() { return padding; }
    @Override
    public long getRawFileDataStart() { return rawFileDataStartOffset; }
    @Override
    public PFS0subFile[] getPfs0subFiles() { return pfs0subFiles; }
    @Override
    public File getFile(){ return file; }
    @Override
    public PipedInputStream getProviderSubFilePipedInpStream(int subFileNumber) throws Exception{        // TODO: Throw exceptions?
        if (subFileNumber >= pfs0subFiles.length) {
            throw new Exception("PFS0Provider -> getPfs0subFilePipedInpStream(): Requested sub file doesn't exists");
        }
        PipedOutputStream streamOut = new PipedOutputStream();
        Thread workerThread;

        PipedInputStream streamIn = new PipedInputStream(streamOut);

        workerThread = new Thread(() -> {
            System.out.println("PFS0Provider -> getPfs0subFilePipedInpStream(): Executing thread");
            try {
                long subFileRealPosition = rawFileDataStartOffset + pfs0subFiles[subFileNumber].getOffset();
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                if (bis.skip(subFileRealPosition) != subFileRealPosition) {
                    System.out.println("PFS0Provider -> getPfs0subFilePipedInpStream(): Unable to skip requested offset");
                    return;
                }

                int readPice = 8388608; // 8mb NOTE: consider switching to 1mb 1048576

                long readFrom = 0;
                long realFileSize = pfs0subFiles[subFileNumber].getSize();

                byte[] readBuf;

                while (readFrom < realFileSize) {
                    if (realFileSize - readFrom < readPice)
                        readPice = Math.toIntExact(realFileSize - readFrom);    // it's safe, I guarantee
                    readBuf = new byte[readPice];
                    if (bis.read(readBuf) != readPice) {
                        System.out.println("PFS0Provider -> getPfs0subFilePipedInpStream(): Unable to read requested size from file.");
                        return;
                    }
                    streamOut.write(readBuf);
                    readFrom += readPice;
                }
                bis.close();
                streamOut.close();
            } catch (IOException ioe) {
                System.out.println("PFS0Provider -> getPfs0subFilePipedInpStream(): Unable to provide stream");
                ioe.printStackTrace();
            }
            System.out.println("PFS0Provider -> getPfs0subFilePipedInpStream(): Thread died");
        });
        workerThread.start();
        return streamIn;
    }
    /**
     * Some sugar
     * */
    @Override
    public PipedInputStream getProviderSubFilePipedInpStream(String subFileName) throws Exception {
        for (int i = 0; i < pfs0subFiles.length; i++){
            if (pfs0subFiles[i].getName().equals(subFileName))
                return getProviderSubFilePipedInpStream(i);
        }
        return null;
    }

    public void printDebug(){
        log.debug(".:: PFS0Provider ::.\n" +
                "File name:                " + file.getName() + "\n\n" +
                "Raw file data start:      " + RainbowDump.formatDecHexString(rawFileDataStartOffset) + "\n" +
                "Magic                     " + magic + "\n" +
                "Files count               " + RainbowDump.formatDecHexString(filesCount) + "\n" +
                "String Table Size         " + RainbowDump.formatDecHexString(stringTableSize) + "\n" +
                "Padding                   " + Converter.byteArrToHexString(padding) + "\n"
        );
        for (PFS0subFile subFile : pfs0subFiles){
            log.debug(
                    "\nName:                     " + subFile.getName() + "\n" +
                    "Offset                    " + RainbowDump.formatDecHexString(subFile.getOffset()) + "\n" +
                    "Size                      " + RainbowDump.formatDecHexString(subFile.getSize()) + "\n" +
                    "Zeroes                    " + Converter.byteArrToHexString(subFile.getZeroes()) + "\n" +
                    "----------------------------------------------------------------"
            );
        }
    }
}
