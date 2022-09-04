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
package libKonogonka.Tools.NCA;

import libKonogonka.Converter;
import libKonogonka.RainbowDump;
import libKonogonka.Tools.NCA.NCASectionTableBlock.NcaFsHeader;
import libKonogonka.Tools.PFS0.IPFS0Provider;
import libKonogonka.Tools.PFS0.PFS0EncryptedProvider;
import libKonogonka.Tools.PFS0.PFS0Provider;
import libKonogonka.Tools.RomFs.IRomFsProvider;
import libKonogonka.Tools.RomFs.RomFsEncryptedProvider;
import libKonogonka.ctraes.AesCtrDecryptSimple;
import libKonogonka.exceptions.EmptySectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.LinkedList;
/**
 * THIS CLASS BECOMES MORE UGLY AFTER EACH ITERATION OF REFACTORING.
 * TODO: MAKE SOME DECOMPOSITION
 * */
public class NCAContent {
    private final static Logger log = LogManager.getLogger(NCAContent.class);
    
    private final File file;
    private final long offsetPosition;
    private final NcaFsHeader ncaFsHeader;
    private final NCAHeaderTableEntry ncaHeaderTableEntry;
    private final byte[] decryptedKey;

    private final LinkedList<byte[]> Pfs0SHA256hashes;
    private IPFS0Provider pfs0;
    private IRomFsProvider romfs;

    // TODO: if decryptedKey is empty, throw exception ??
    public NCAContent(File file,
                      long offsetPosition,
                      NcaFsHeader ncaFsHeader,
                      NCAHeaderTableEntry ncaHeaderTableEntry,
                      byte[] decryptedKey) throws Exception
    {
        this.file = file;
        this.offsetPosition = offsetPosition;
        this.ncaFsHeader = ncaFsHeader;
        this.ncaHeaderTableEntry = ncaHeaderTableEntry;
        this.decryptedKey = decryptedKey;

        Pfs0SHA256hashes = new LinkedList<>();
        // If nothing to do
        if (ncaHeaderTableEntry.getMediaEndOffset() == 0)
            throw new EmptySectionException("Empty section");
        // If it's PFS0Provider
        if (ncaFsHeader.getSuperBlockPFS0() != null)
            this.proceedPFS0();
        else if (ncaFsHeader.getSuperBlockIVFC() != null)
            this.proceedRomFs();
        else
            throw new Exception("NCAContent(): Not supported. PFS0 or RomFS supported only.");
    }

    private void proceedPFS0() throws Exception {
        switch (ncaFsHeader.getCryptoType()){
            case 0x01:
                proceedPFS0NotEncrypted(); // IF NO ENCRYPTION
                break;
            case 0x03:
                proceedPFS0Encrypted(); // If encrypted regular [ 0x03 ]
                break;
            default:
                throw new Exception("NCAContent() -> proceedPFS0(): Non-supported 'Crypto type'");
        }
    }
    private void proceedPFS0NotEncrypted() throws Exception{
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        long thisMediaLocation = offsetPosition + (ncaHeaderTableEntry.getMediaStartOffset() * 0x200);
        long hashTableLocation = thisMediaLocation + ncaFsHeader.getSuperBlockPFS0().getHashTableOffset();
        long pfs0Location = thisMediaLocation + ncaFsHeader.getSuperBlockPFS0().getPfs0offset();

        raf.seek(hashTableLocation);

        byte[] rawData;
        long sha256recordsNumber = ncaFsHeader.getSuperBlockPFS0().getHashTableSize() / 0x20;
        // Collect hashes
        for (int i = 0; i < sha256recordsNumber; i++){
            rawData = new byte[0x20];       // 32 bytes - size of SHA256 hash
            if (raf.read(rawData) != -1)
                Pfs0SHA256hashes.add(rawData);
            else {
                raf.close();
                return;                      // TODO: fix
            }
        }
        raf.close();
        // Get pfs0
        pfs0 = new PFS0Provider(file, pfs0Location);
    }
    private void proceedPFS0Encrypted() throws Exception{
        new CryptoSection03Pfs0(file,
                offsetPosition,
                decryptedKey,
                ncaFsHeader,
                ncaHeaderTableEntry.getMediaStartOffset(),
                ncaHeaderTableEntry.getMediaEndOffset());
    }

    private void proceedRomFs() throws Exception{
        switch (ncaFsHeader.getCryptoType()){
            case 0x01:
                proceedRomFsNotEncrypted(); // IF NO ENCRYPTION
                break;
            case 0x03:
                proceedRomFsEncrypted(); // If encrypted regular [ 0x03 ]
                break;
            default:
                throw new Exception("Non-supported 'Crypto type'");
        }
    }
    private void proceedRomFsNotEncrypted(){   // TODO: Clarify, implement if needed
        log.error("proceedRomFs() -> proceedRomFsNotEncrypted() is not implemented :(");
    }
    private void proceedRomFsEncrypted() throws Exception{
        if (decryptedKey == null)
            throw new Exception("CryptoSection03: unable to proceed. No decrypted key provided.");

        this.romfs = new RomFsEncryptedProvider(
                ncaFsHeader.getSuperBlockIVFC().getLvl6Offset(),
                file,
                offsetPosition,
                decryptedKey,
                ncaFsHeader.getSectionCTR(),
                ncaHeaderTableEntry.getMediaStartOffset(),
                ncaHeaderTableEntry.getMediaEndOffset());
    }

    public LinkedList<byte[]> getPfs0SHA256hashes() { return Pfs0SHA256hashes; }
    public IPFS0Provider getPfs0() { return pfs0; }
    public IRomFsProvider getRomfs() { return romfs; }

    private class CryptoSection03Pfs0 {
        CryptoSection03Pfs0(File file,
                            long offsetPosition,
                            byte[] decryptedKey,
                            NcaFsHeader ncaFsHeader,
                            long mediaStartBlocksOffset,
                            long mediaEndBlocksOffset) throws Exception
        {
            log.debug( "-== Crypto Section 03 PFS0 ==-\n" +
                "Media start location:      " + RainbowDump.formatDecHexString(mediaStartBlocksOffset) + "\n" +
                "Media end location:        " + RainbowDump.formatDecHexString(mediaEndBlocksOffset) + "\n" +
                "Media size:                " + RainbowDump.formatDecHexString((mediaEndBlocksOffset-mediaStartBlocksOffset)) + "\n" +
                "Media actual location:     " + RainbowDump.formatDecHexString((offsetPosition + (mediaStartBlocksOffset * 0x200))) + "\n" +
                "SHA256 hash table size:    " + RainbowDump.formatDecHexString(ncaFsHeader.getSuperBlockPFS0().getHashTableSize()) + "\n" +
                "SHA256 hash table offs:    " + RainbowDump.formatDecHexString(ncaFsHeader.getSuperBlockPFS0().getHashTableOffset()) + "\n" +
                "PFS0 Offset:               " + RainbowDump.formatDecHexString(ncaFsHeader.getSuperBlockPFS0().getPfs0offset()) + "\n" +
                "SHA256 records:            " + RainbowDump.formatDecHexString((ncaFsHeader.getSuperBlockPFS0().getHashTableSize() / 0x20)) + "\n" +
                "KEY (decrypted):           " + Converter.byteArrToHexString(decryptedKey) + "\n" +
                "CTR:                       " + Converter.byteArrToHexString(ncaFsHeader.getSectionCTR()) + "\n");
            if (decryptedKey == null)
                throw new Exception("CryptoSection03: unable to proceed. No decrypted key provided.");

            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long abosluteOffsetPosition = offsetPosition + (mediaStartBlocksOffset * 0x200);
            raf.seek(abosluteOffsetPosition);

            AesCtrDecryptSimple decryptor = new AesCtrDecryptSimple(decryptedKey, ncaFsHeader.getSectionCTR(), mediaStartBlocksOffset * 0x200);

            byte[] encryptedBlock;
            byte[] dectyptedBlock;
            long mediaBlocksSize = mediaEndBlocksOffset - mediaStartBlocksOffset;
            // Prepare thread to parse encrypted data
            PipedOutputStream streamOut = new PipedOutputStream();
            PipedInputStream streamInp = new PipedInputStream(streamOut);

            Thread pThread = new Thread(new ParseThread(
                    streamInp,
                    ncaFsHeader.getSuperBlockPFS0().getPfs0offset(),
                    ncaFsHeader.getSuperBlockPFS0().getHashTableOffset(),
                    ncaFsHeader.getSuperBlockPFS0().getHashTableSize(),
                    offsetPosition,
                    file,
                    decryptedKey,
                    ncaFsHeader.getSectionCTR(),
                    mediaStartBlocksOffset,
                    mediaEndBlocksOffset
            ));
            pThread.start();
            // Decrypt data
            for (int i = 0; i < mediaBlocksSize; i++){
                encryptedBlock = new byte[0x200];
                if (raf.read(encryptedBlock) != -1){
                    //dectyptedBlock = aesCtr.decrypt(encryptedBlock);
                    dectyptedBlock = decryptor.decryptNext(encryptedBlock);
                    // Writing decrypted data to pipe
                    try {
                        streamOut.write(dectyptedBlock);
                    }
                    catch (IOException e){
                        break;
                    }
                }
            }
            pThread.join();
            streamOut.close();
            raf.close();
        }
        /**
        * Since we're representing decrypted data as stream (it's easier to look on it this way),
        * this thread will be parsing it.
        * */
        private class ParseThread implements Runnable{

            PipedInputStream pipedInputStream;

            long hashTableOffset;
            long hashTableSize;
            long hashTableRecordsCount;
            long pfs0offset;

            private final long MetaOffsetPositionInFile;
            private final File MetaFileWithEncPFS0;
            private final byte[] MetaKey;
            private final byte[] MetaSectionCTR;
            private final long MetaMediaStartOffset;
            private final long MetaMediaEndOffset;


            ParseThread(PipedInputStream pipedInputStream,
                        long pfs0offset,
                        long hashTableOffset,
                        long hashTableSize,

                        long MetaOffsetPositionInFile,
                        File MetaFileWithEncPFS0,
                        byte[] MetaKey,
                        byte[] MetaSectionCTR,
                        long MetaMediaStartOffset,
                        long MetaMediaEndOffset
            ){
                this.pipedInputStream = pipedInputStream;
                this.hashTableOffset = hashTableOffset;
                this.hashTableSize = hashTableSize;
                this.hashTableRecordsCount = hashTableSize / 0x20;
                this.pfs0offset = pfs0offset;

                this.MetaOffsetPositionInFile = MetaOffsetPositionInFile;
                this.MetaFileWithEncPFS0 = MetaFileWithEncPFS0;
                this.MetaKey = MetaKey;
                this.MetaSectionCTR = MetaSectionCTR;
                this.MetaMediaStartOffset = MetaMediaStartOffset;
                this.MetaMediaEndOffset = MetaMediaEndOffset;

            }

            @Override
            public void run() {
                long counter = 0;       // How many bytes already read

                try{
                    if (hashTableOffset > 0){
                        if (hashTableOffset != pipedInputStream.skip(hashTableOffset))
                            return;                                                     // TODO: fix?
                        counter = hashTableOffset;
                    }
                    // Loop for collecting all recrods from sha256 hash table
                    while ((counter - hashTableOffset) < hashTableSize){
                        int hashCounter = 0;
                        byte[] sectionHash = new byte[0x20];
                        // Loop for collecting bytes for every SINGLE records, where record size == 0x20
                        while (hashCounter < 0x20){
                            int currentByte = pipedInputStream.read();
                            if (currentByte == -1)
                                break;
                            sectionHash[hashCounter] = (byte)currentByte;
                            hashCounter++;
                            counter++;
                        }
                        // Write after collecting
                        Pfs0SHA256hashes.add(sectionHash);  // From the NCAContentProvider obviously
                    }
                    // Skip padding and go to PFS0 location
                    if (counter < pfs0offset){
                        long toSkip = pfs0offset-counter;
                        if (toSkip != pipedInputStream.skip(toSkip))
                            return;                                                     // TODO: fix?
                        counter += toSkip;
                    }
                    //---------------------------------------------------------
                    pfs0 = new PFS0EncryptedProvider(pipedInputStream,
                            counter,
                            MetaOffsetPositionInFile,
                            MetaFileWithEncPFS0,
                            MetaKey,
                            MetaSectionCTR,
                            MetaMediaStartOffset,
                            MetaMediaEndOffset);
                    pipedInputStream.close();
                }
                catch (Exception e){
                    log.debug("NCA Content parsing thread exception: ", e);
                }
                //finally { System.out.println("NCA Content thread dies");}
            }
        }
    }

    /**
     * Export NCA content AS IS.
     * Not so good for PFS0 since there are SHAs list that discourages but good for 'romfs' and things like that
     * */
    public PipedInputStream getRawDataContentPipedInpStream() throws Exception {
        long mediaStartBlocksOffset = ncaHeaderTableEntry.getMediaStartOffset();
        long mediaEndBlocksOffset = ncaHeaderTableEntry.getMediaEndOffset();
        long mediaBlocksSize = mediaEndBlocksOffset - mediaStartBlocksOffset;

        RandomAccessFile raf = new RandomAccessFile(file, "r");
        ///--------------------------------------------------------------------------------------------------
        log.debug("NCAContent() -> exportEncryptedSectionType03() information" + "\n" +
        "Media start location: " + mediaStartBlocksOffset + "\n" +
        "Media end location:   " + mediaEndBlocksOffset + "\n" +
        "Media size          : " + (mediaEndBlocksOffset-mediaStartBlocksOffset) + "\n" +
        "Media act. location:  " + (offsetPosition + (mediaStartBlocksOffset * 0x200)) + "\n" +
        "KEY:                  " + Converter.byteArrToHexString(decryptedKey) + "\n" +
        "CTR:                  " + Converter.byteArrToHexString(ncaFsHeader.getSectionCTR()) + "\n");
        //---------------------------------------------------------------------------------------------------/

        if (ncaFsHeader.getCryptoType() == 0x01){
            log.trace("NCAContent -> getRawDataContentPipedInpStream (Zero encryption section type 01): Thread started");

            Thread workerThread;
            PipedOutputStream streamOut = new PipedOutputStream();

            PipedInputStream streamIn = new PipedInputStream(streamOut);
            workerThread = new Thread(() -> {
                try {
                    byte[] rawDataBlock;
                    for (int i = 0; i < mediaBlocksSize; i++){
                        rawDataBlock = new byte[0x200];
                        if (raf.read(rawDataBlock) != -1)
                            streamOut.write(rawDataBlock);
                        else
                            break;
                    }
                }
                catch (Exception e){
                    log.error("NCAContent -> exportRawData() failure", e);
                }
                finally {
                    try {
                        raf.close();
                    }catch (Exception ignored) {}
                    try {
                        streamOut.close();
                    }catch (Exception ignored) {}
                }
                log.trace("NCAContent -> exportRawData(): Thread died");
            });
            workerThread.start();
            return streamIn;
        }
        else if (ncaFsHeader.getCryptoType() == 0x03){
            log.trace("NCAContent -> getRawDataContentPipedInpStream (Encrypted Section Type 03): Thread started");

            if (decryptedKey == null)
                throw new Exception("NCAContent -> exportRawData(): unable to proceed. No decrypted key provided.");

            Thread workerThread;
            PipedOutputStream streamOut = new PipedOutputStream();

            PipedInputStream streamIn = new PipedInputStream(streamOut);
            workerThread = new Thread(() -> {
                try {
                    //RandomAccessFile raf = new RandomAccessFile(file, "r");
                    long abosluteOffsetPosition = offsetPosition + (mediaStartBlocksOffset * 0x200);
                    raf.seek(abosluteOffsetPosition);

                    AesCtrDecryptSimple decryptor = new AesCtrDecryptSimple(decryptedKey,
                            ncaFsHeader.getSectionCTR(),
                            mediaStartBlocksOffset * 0x200);

                    byte[] encryptedBlock;
                    byte[] dectyptedBlock;

                    // Decrypt data
                    for (int i = 0; i < mediaBlocksSize; i++){
                        encryptedBlock = new byte[0x200];
                        if (raf.read(encryptedBlock) != -1){
                            dectyptedBlock = decryptor.decryptNext(encryptedBlock);
                            // Writing decrypted data to pipe
                            streamOut.write(dectyptedBlock);
                        }
                        else
                            break;
                    }
                }
                catch (Exception e){
                    log.error("NCAContent -> exportRawData(): ", e);
                }
                finally {
                    try {
                        raf.close();
                    }catch (Exception ignored) {}
                    try {
                        streamOut.close();
                    }catch (Exception ignored) {}
                }
                log.trace("NCAContent -> exportRawData(): Thread died");
            });
            workerThread.start();
            return streamIn;
        }
        else
            return null;
    }
    public long getRawDataContentSize(){
        return (ncaHeaderTableEntry.getMediaEndOffset() - ncaHeaderTableEntry.getMediaStartOffset()) * 0x200;
    }
    public String getFileName(){
        return file.getName();
    }
}