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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.Converter.*;

public class PFS0Provider implements IPFS0Provider{
    private final long rawFileDataStart;          // Where data starts, excluding header, string table etc.

    private final String magic;
    private final int filesCount;
    private final int stringTableSize;
    private final byte[] padding;
    private final PFS0subFile[] pfs0subFiles;

    private final File file;

    public PFS0Provider(File fileWithPfs0) throws Exception{ this(fileWithPfs0, 0); }

    public PFS0Provider(File fileWithPfs0, long pfs0offsetPosition) throws Exception{
        file = fileWithPfs0;
        RandomAccessFile raf = new RandomAccessFile(fileWithPfs0, "r");         // TODO: replace to bufferedInputStream

        raf.seek(pfs0offsetPosition);
        byte[] fileStartingBytes = new byte[0x10];
        // Read PFS0Provider, files count, header, padding (4 zero bytes)
        if (raf.read(fileStartingBytes) != 0x10){
            raf.close();
            throw new Exception("PFS0Provider: Unable to read starting bytes");
        }
        // Check PFS0Provider
        magic = new String(fileStartingBytes, 0x0, 0x4, StandardCharsets.US_ASCII);
        if (! magic.equals("PFS0")){
            raf.close();
            throw new Exception("PFS0Provider: Bad magic");
        }
        // Get files count
        filesCount = getLEint(fileStartingBytes, 0x4);
        if (filesCount <= 0 ) {
            raf.close();
            throw new Exception("PFS0Provider: Files count is too small");
        }
        // Get string table
        stringTableSize = getLEint(fileStartingBytes, 0x8);
        if (stringTableSize <= 0 ){
            raf.close();
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
            if (raf.read(fileEntryTable) != 0x18)
                throw new Exception("PFS0Provider: String table is too small");
            offsetsSubFiles[i] = getLElong(fileEntryTable, 0);
            sizesSubFiles[i] = getLElong(fileEntryTable, 0x8);
            strTableOffsets[i] = getLEint(fileEntryTable, 0x10);
            zeroBytes[i] = Arrays.copyOfRange(fileEntryTable, 0x14, 0x18);
        }
        //**********************************************************************************************************
        // In here pointer in front of String table
        String[] subFileNames = new String[filesCount];
        byte[] stringTbl = new byte[stringTableSize];
        if (raf.read(stringTbl) != stringTableSize){
            throw new Exception("Read PFS0Provider String table failure. Can't read requested string table size ("+stringTableSize+")");
        }

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
        rawFileDataStart = raf.getFilePointer();
        raf.close();
    }

    @Override
    public boolean isEncrypted() { return false; }
    @Override
    public String getMagic() { return magic; }
    @Override
    public int getFilesCount() { return filesCount; }
    @Override
    public int getStringTableSize() { return stringTableSize; }
    @Override
    public byte[] getPadding() { return padding; }
    @Override
    public long getRawFileDataStart() { return rawFileDataStart; }
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
                long subFileRealPosition = rawFileDataStart + pfs0subFiles[subFileNumber].getOffset();
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
}
