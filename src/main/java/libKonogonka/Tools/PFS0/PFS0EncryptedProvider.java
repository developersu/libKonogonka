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
import libKonogonka.Tools.RomFs.Level6Header;
import libKonogonka.ctraes.AesCtrDecryptSimple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static libKonogonka.Converter.*;

public class PFS0EncryptedProvider implements IPFS0Provider{
    private final static Logger log = LogManager.getLogger(PFS0EncryptedProvider.class);

    //private long rawFileDataStart;      // Always -1 @ PFS0EncryptedProvider

    private final String magic;
    private final int filesCount;
    private final int stringTableSize;
    private final byte[] padding;
    private final PFS0subFile[] pfs0subFiles;

    //---------------------------------------

    private long rawBlockDataStart;

    private final long offsetPositionInFile;
    private final File file;
    private final byte[] key;
    private final byte[] sectionCTR;
    private final long mediaStartOffset;  // In 512-blocks
    private final long mediaEndOffset;    // In 512-blocks

    public PFS0EncryptedProvider(PipedInputStream pipedInputStream,
                                 long pfs0offsetPosition,
                                 long offsetPositionInFile,
                                 File fileWithEncPFS0,
                                 byte[] key,
                                 byte[] sectionCTR,
                                 long mediaStartOffset,
                                 long mediaEndOffset
    ) throws Exception{
        // Populate 'meta' data that is needed for getProviderSubFilePipedInpStream()
        this.offsetPositionInFile = offsetPositionInFile + mediaStartOffset*0x200;
        this.file = fileWithEncPFS0;
        this.key = key;
        this.sectionCTR = sectionCTR;
        this.mediaStartOffset = mediaStartOffset;
        this.mediaEndOffset = mediaEndOffset;
        // pfs0offsetPosition is a position relative to Media block. Let's add pfs0 'header's' bytes count and get raw data start position in media block
        //rawFileDataStart = -1;                  // Set -1 for PFS0EncryptedProvider
        // Detect raw data start position using next var
        rawBlockDataStart = pfs0offsetPosition;

        byte[] fileStartingBytes = new byte[0x10];
        // Read PFS0Provider, files count, header, padding (4 zero bytes)

        for (int i = 0; i < 0x10; i++){
            int currentByte = pipedInputStream.read();
            if (currentByte == -1) {
                throw new Exception("PFS0EncryptedProvider: Reading stream suddenly ended while trying to read starting 0x10 bytes");
            }
            fileStartingBytes[i] = (byte)currentByte;
        }
        // Update position
        rawBlockDataStart += 0x10;
        // Check PFS0Provider
        magic = new String(fileStartingBytes, 0x0, 0x4, StandardCharsets.US_ASCII);
        if (! magic.equals("PFS0")){
            throw new Exception("PFS0EncryptedProvider: Bad magic");
        }
        // Get files count
        filesCount = getLEint(fileStartingBytes, 0x4);
        if (filesCount <= 0 ) {
            throw new Exception("PFS0EncryptedProvider: Files count is too small");
        }
        // Get string table
        stringTableSize = getLEint(fileStartingBytes, 0x8);
        if (stringTableSize <= 0 ){
            throw new Exception("PFS0EncryptedProvider: String table is too small");
        }
        padding = Arrays.copyOfRange(fileStartingBytes, 0xc, 0x10);
        //---------------------------------------------------------------------------------------------------------
        pfs0subFiles = new PFS0subFile[filesCount];

        long[] offsetsSubFiles = new long[filesCount];
        long[] sizesSubFiles = new long[filesCount];
        int[] strTableOffsets = new int[filesCount];
        byte[][] zeroBytes = new byte[filesCount][];

        byte[] fileEntryTable = new byte[0x18];
        for (int i=0; i < filesCount; i++){
            for (int j = 0; j < 0x18; j++){
                int currentByte = pipedInputStream.read();
                if (currentByte == -1) {
                    throw new Exception("PFS0EncryptedProvider: Reading stream suddenly ended while trying to read File Entry Table #"+i);
                }
                fileEntryTable[j] = (byte)currentByte;
            }
            offsetsSubFiles[i] = getLElong(fileEntryTable, 0);
            sizesSubFiles[i] = getLElong(fileEntryTable, 0x8);
            strTableOffsets[i] = getLEint(fileEntryTable, 0x10);
            zeroBytes[i] = Arrays.copyOfRange(fileEntryTable, 0x14, 0x18);
            // Update position
            rawBlockDataStart += 0x18;
        }
        //**********************************************************************************************************
        // In here pointer in front of String table
        String[] subFileNames = new String[filesCount];
        byte[] stringTbl = new byte[stringTableSize];

        for (int i = 0; i < stringTableSize; i++){
            int currentByte = pipedInputStream.read();
            if (currentByte == -1) {
                throw new Exception("PFS0EncryptedProvider: Reading stream suddenly ended while trying to read string table");
            }
            stringTbl[i] = (byte)currentByte;
        }
        // Update position
        rawBlockDataStart += stringTableSize;

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
    }

    @Override
    public boolean isEncrypted() { return true; }
    @Override
    public String getMagic() { return magic; }
    @Override
    public int getFilesCount() { return filesCount; }
    @Override
    public int getStringTableSize() { return stringTableSize; }
    @Override
    public byte[] getPadding() { return padding; }
    @Override
    public long getRawFileDataStart() { return rawBlockDataStart; }
    @Override
    public PFS0subFile[] getPfs0subFiles() { return pfs0subFiles; }
    @Override
    public File getFile(){ return file; }
    @Override
    public PipedInputStream getProviderSubFilePipedInpStream(int subFileNumber) throws Exception { // TODO: rewrite
        if (subFileNumber >= pfs0subFiles.length)
            throw new Exception("PFS0Provider -> getPfs0subFilePipedInpStream(): Requested sub file doesn't exists");

        Thread workerThread;
        PipedOutputStream streamOut = new PipedOutputStream();

        PipedInputStream streamIn = new PipedInputStream(streamOut);
        workerThread = new Thread(() -> {
            log.debug("PFS0EncryptedProvider -> getPfs0subFilePipedInpStream(): Executing thread:\nSub file: " +
                    pfs0subFiles[subFileNumber].getName() +
                    "\nFor block #                         "+((rawBlockDataStart + pfs0subFiles[subFileNumber].getOffset()) / 0x200) +
                    "\nAnd initial skipped bytes are:      "+offsetPositionInFile +
                    "\nWhere Raw Block Data Start:         "+rawBlockDataStart +
                    "\nAnd sub file offset:                "+pfs0subFiles[subFileNumber].getOffset()+
                    "\nSkip bytes                          "+((rawBlockDataStart + pfs0subFiles[subFileNumber].getOffset()) - ((rawBlockDataStart + pfs0subFiles[subFileNumber].getOffset()) / 0x200) * 0x200)+
                    "\nKEY                                 "+Converter.byteArrToHexString(key)+
                    "\nSection CTR                         "+Converter.byteArrToHexString(sectionCTR)+
                    "\n______________________________________________________________");
            try {
                BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()));
                // Check if skip was successful
                if (bis.skip(offsetPositionInFile) != offsetPositionInFile) {
                    System.out.println("PFS0EncryptedProvider -> getPfs0subFilePipedInpStream(): Failed to skip range "+offsetPositionInFile);
                    return;
                }

                AesCtrDecryptSimple aesCtrDecryptSimple = new AesCtrDecryptSimple(key, sectionCTR, mediaStartOffset * 0x200);

                byte[] encryptedBlock;
                byte[] dectyptedBlock;

                //----------------------------- Pre-set: skip non-necessary data --------------------------------

                long startBlock = (rawBlockDataStart + pfs0subFiles[subFileNumber].getOffset()) / 0x200;            // <- pointing to place where actual data starts
                int skipBytes;

                if (startBlock > 0) {
                    aesCtrDecryptSimple.skipNext(startBlock);
                    skipBytes = (int)(startBlock * 0x200);
                    if (bis.skip(skipBytes) != skipBytes) {
                        System.out.println("PFS0EncryptedProvider -> getPfs0subFilePipedInpStream(): Failed to skip range "+skipBytes);
                        return;
                    }
                }

                //----------------------------- Step 1: get starting bytes from the end of the junk block --------------------------------

                // Since our data could be located in position with some offset from the decrypted block, let's skip bytes left. Considering the case when data is not aligned to block
                skipBytes = (int) ((rawBlockDataStart + pfs0subFiles[subFileNumber].getOffset()) - startBlock * 0x200); // <- How much bytes shall we skip to reach requested data start of sub-file

                if (skipBytes > 0) {
                    encryptedBlock = new byte[0x200];
                    if (bis.read(encryptedBlock) == 0x200) {
                        dectyptedBlock = aesCtrDecryptSimple.decryptNext(encryptedBlock);
                        // If we have extra-small file that is less then a block and even more
                        if ((0x200 - skipBytes) > pfs0subFiles[subFileNumber].getSize()){
                            streamOut.write(dectyptedBlock, skipBytes, (int) pfs0subFiles[subFileNumber].getSize());    // safe cast
                            bis.close();
                            streamOut.close();
                            return;
                        }
                        else
                            streamOut.write(dectyptedBlock, skipBytes, 0x200 - skipBytes);
                    }
                    else {
                        System.out.println("PFS0EncryptedProvider -> getProviderSubFilePipedInpStream(): Unable to get 512 bytes from 1st bock");
                        return;
                    }
                    startBlock++;
                }
                long endBlock = pfs0subFiles[subFileNumber].getSize() / 0x200 + startBlock;  // <- pointing to place where any data related to this media-block ends

                //----------------------------- Step 2: Detect if we have junk data on the end of the final block --------------------------------
                int extraData = (int)(rawBlockDataStart+pfs0subFiles[subFileNumber].getOffset()+pfs0subFiles[subFileNumber].getSize() - (endBlock*0x200));  // safe cast
                if (extraData < 0){
                    endBlock--;
                }
                //----------------------------- Step 3: Read main part of data --------------------------------
                // Here we're reading main amount of bytes. We can read only less bytes.
                while ( startBlock < endBlock) {
                    encryptedBlock = new byte[0x200];
                    if (bis.read(encryptedBlock) == 0x200) {
                        //dectyptedBlock = aesCtr.decrypt(encryptedBlock);
                        dectyptedBlock = aesCtrDecryptSimple.decryptNext(encryptedBlock);
                        // Writing decrypted data to pipe
                        streamOut.write(dectyptedBlock);
                    }
                    else {
                        System.out.println("PFS0EncryptedProvider -> getProviderSubFilePipedInpStream(): Unable to get 512 bytes from bock");
                        return;
                    }
                    startBlock++;
                }
                //----------------------------- Step 4: Read what's left --------------------------------
                // Now we have to find out if data overlaps to one more extra block
                if (extraData > 0){                 // In case we didn't get what we want
                    encryptedBlock = new byte[0x200];
                    if (bis.read(encryptedBlock) == 0x200) {
                        dectyptedBlock = aesCtrDecryptSimple.decryptNext(encryptedBlock);
                        streamOut.write(dectyptedBlock, 0, extraData);
                    }
                    else {
                        System.out.println("PFS0EncryptedProvider -> getProviderSubFilePipedInpStream(): Unable to get 512 bytes from bock");
                        return;
                    }
                }
                else if (extraData < 0){                // In case we can get more than we need
                    encryptedBlock = new byte[0x200];
                    if (bis.read(encryptedBlock) == 0x200) {
                        dectyptedBlock = aesCtrDecryptSimple.decryptNext(encryptedBlock);
                        streamOut.write(dectyptedBlock, 0, 0x200 + extraData);       // WTF ??? THIS LOOKS INCORRECT
                    }
                    else {
                        System.out.println("PFS0EncryptedProvider -> getProviderSubFilePipedInpStream(): Unable to get 512 bytes from last bock");
                        return;
                    }
                }
                bis.close();
                streamOut.close();
            }
            catch (Exception e){
                System.out.println("PFS0EncryptedProvider -> getProviderSubFilePipedInpStream(): "+e.getMessage());
                e.printStackTrace();
            }
            System.out.println("PFS0EncryptedProvider -> getPfs0subFilePipedInpStream(): Thread died");


        });
        workerThread.start();
        return streamIn;
    }

    @Override
    public PipedInputStream getProviderSubFilePipedInpStream(String subFileName) throws Exception{
        for (int i = 0; i < pfs0subFiles.length; i++){
            if (pfs0subFiles[i].getName().equals(subFileName))
                return getProviderSubFilePipedInpStream(i);
        }
        return null;
    }

    public void printDebug(){
        log.debug(".:: PFS0EncryptedProvider ::.\n" +
                "File name:                " + file.getName() + "\n" +
                "Raw block data start      " + RainbowDump.formatDecHexString(rawBlockDataStart) + "\n" +
                "Magic                     " + magic + "\n" +
                "Files count               " + RainbowDump.formatDecHexString(filesCount) + "\n" +
                "String Table Size         " + RainbowDump.formatDecHexString(stringTableSize) + "\n" +
                "Padding                   " + Converter.byteArrToHexString(padding) + "\n\n" +

                "Offset position in file   " + RainbowDump.formatDecHexString(offsetPositionInFile) + "\n" +
                "Media Start Offset        " + RainbowDump.formatDecHexString(mediaStartOffset) + "\n" +
                "Media End Offset          " + RainbowDump.formatDecHexString(mediaEndOffset) + "\n"
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
