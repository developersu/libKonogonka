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
import libKonogonka.Tools.NCA.NCASectionTableBlock.NcaFsHeader;
import libKonogonka.Tools.PFS0.IPFS0Provider;
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
    private final long ncaOffsetPosition;
    private final NcaFsHeader ncaFsHeader;
    private final NCAHeaderTableEntry ncaHeaderTableEntry;
    private final byte[] decryptedKey;

    private LinkedList<byte[]> Pfs0SHA256hashes;
    private IPFS0Provider pfs0;
    private IRomFsProvider romfs;

    // TODO: if decryptedKey is empty, throw exception ??
    public NCAContent(File file,
                      long ncaOffsetPosition,
                      NcaFsHeader ncaFsHeader,
                      NCAHeaderTableEntry ncaHeaderTableEntry,
                      byte[] decryptedKey) throws Exception
    {
        this.file = file;
        this.ncaOffsetPosition = ncaOffsetPosition;
        this.ncaFsHeader = ncaFsHeader;
        this.ncaHeaderTableEntry = ncaHeaderTableEntry;
        this.decryptedKey = decryptedKey;
        System.out.println("NCAContent pfs0offsetPosition: "+ncaOffsetPosition);
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
        pfs0 = new PFS0Provider(file,
                ncaOffsetPosition,
                ncaFsHeader.getSuperBlockPFS0(),
                ncaHeaderTableEntry.getMediaStartOffset(),
                ncaHeaderTableEntry.getMediaEndOffset());
        Pfs0SHA256hashes = pfs0.getPfs0SHA256hashes();
    }

    private void proceedPFS0Encrypted() throws Exception{
        AesCtrDecryptSimple decryptor = new AesCtrDecryptSimple(decryptedKey, ncaFsHeader.getSectionCTR(),
                ncaHeaderTableEntry.getMediaStartOffset() * 0x200);
        pfs0 = new PFS0Provider(file,
                ncaOffsetPosition,
                ncaFsHeader.getSuperBlockPFS0(),
                decryptor,
                ncaHeaderTableEntry.getMediaStartOffset(),
                ncaHeaderTableEntry.getMediaEndOffset());
        Pfs0SHA256hashes = pfs0.getPfs0SHA256hashes();
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
                ncaOffsetPosition,
                decryptedKey,
                ncaFsHeader.getSectionCTR(),
                ncaHeaderTableEntry.getMediaStartOffset(),
                ncaHeaderTableEntry.getMediaEndOffset());
    }

    public LinkedList<byte[]> getPfs0SHA256hashes() { return Pfs0SHA256hashes; }
    public IPFS0Provider getPfs0() { return pfs0; }
    public IRomFsProvider getRomfs() { return romfs; }


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
        "Media act. location:  " + (ncaOffsetPosition + (mediaStartBlocksOffset * 0x200)) + "\n" +
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
                    long abosluteOffsetPosition = ncaOffsetPosition + (mediaStartBlocksOffset * 0x200);
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