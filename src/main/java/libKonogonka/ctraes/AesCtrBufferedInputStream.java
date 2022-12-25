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
package libKonogonka.ctraes;

import libKonogonka.RainbowDump;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class AesCtrBufferedInputStream extends BufferedInputStream {
    private final static Logger log = LogManager.getLogger(AesCtrBufferedInputStream.class);

    private final AesCtrDecryptSimple decryptor;
    private final long mediaOffsetPositionStart;
    private final long mediaOffsetPositionEnd;
    private final long fileSize;

    public AesCtrBufferedInputStream(AesCtrDecryptSimple decryptor,
                                     long ncaOffsetPosition,
                                     long mediaStartOffset,
                                     long mediaEndOffset,
                                     InputStream inputStream,
                                     long fileSize){
        super(inputStream);
        this.decryptor = decryptor;
        this.mediaOffsetPositionStart = ncaOffsetPosition + (mediaStartOffset * 0x200);
        this.mediaOffsetPositionEnd = ncaOffsetPosition + (mediaEndOffset * 0x200);
        this.fileSize = fileSize;

        log.trace("\n  Offset Position             "+ncaOffsetPosition+
                  "\n  MediaOffsetPositionStart    "+RainbowDump.formatDecHexString(mediaOffsetPositionStart)+
                  "\n  MediaOffsetPositionEnd      "+RainbowDump.formatDecHexString(mediaOffsetPositionEnd));
    }

    private byte[] decryptedBytes;
    private long pseudoPos;
    private int pointerInsideDecryptedSection;

    @Override
    public synchronized int read(byte[] b) throws IOException {
        int bytesToRead = b.length;
        if (isPointerInsideEncryptedSection()){
            int bytesFromFirstBlock = 0x200 - pointerInsideDecryptedSection;
            if (bytesFromFirstBlock > bytesToRead){
                log.trace("1.2. Pointer Inside + End Position Inside (Decrypted) Encrypted Section ("+pseudoPos+"-"+(pseudoPos+b.length)+")");
                System.arraycopy(decryptedBytes, pointerInsideDecryptedSection, b, 0, bytesToRead);

                pseudoPos += bytesToRead;
                pointerInsideDecryptedSection += bytesToRead;
                return b.length;
            }

            if (isEndPositionInsideEncryptedSection(b.length)) {
                log.trace("1.1. Pointer Inside + End Position Inside Encrypted Section ("+pseudoPos+"-"+(pseudoPos+b.length)+")");
                int middleBlocksCount = (bytesToRead - bytesFromFirstBlock) / 0x200;
                int bytesFromLastBlock = (bytesToRead - bytesFromFirstBlock) % 0x200;
                //1
                System.arraycopy(decryptedBytes, pointerInsideDecryptedSection, b, 0, bytesFromFirstBlock);
                //2
                for (int i = 0; i < middleBlocksCount; i++) {
                    fillDecryptedCache();
                    System.arraycopy(decryptedBytes, 0, b, bytesFromFirstBlock+i*0x200, 0x200);
                }
                //3
                if(fileSize > (pseudoPos+bytesToRead)) {
                    fillDecryptedCache();
                    System.arraycopy(decryptedBytes, 0, b, bytesFromFirstBlock + middleBlocksCount * 0x200, bytesFromLastBlock);
                }
                pseudoPos += bytesToRead;
                pointerInsideDecryptedSection = bytesFromLastBlock;
                return b.length;
            }
            log.trace("1. Pointer Inside + End Position Outside Encrypted Section ("+pseudoPos+"-"+(pseudoPos+b.length)+")");
            int middleBlocksCount = (int) ((mediaOffsetPositionEnd - (pseudoPos+bytesFromFirstBlock)) / 0x200);
            int bytesFromEnd = bytesToRead - bytesFromFirstBlock - middleBlocksCount * 0x200;
            //1
            System.arraycopy(decryptedBytes, pointerInsideDecryptedSection, b, 0, bytesFromFirstBlock);
            //2
            //log.debug("\n"+bytesFromFirstBlock+"\n"+ middleBlocksCount+" = "+(middleBlocksCount*0x200)+" bytes\n"+ bytesFromEnd+"\n");
            for (int i = 0; i < middleBlocksCount; i++) {
                fillDecryptedCache();
                System.arraycopy(decryptedBytes, 0, b, bytesFromFirstBlock+i*0x200, 0x200);
            }
            //3             // TODO: if it's zero?
            System.arraycopy(readChunk(bytesFromEnd), 0, b, bytesFromFirstBlock+middleBlocksCount*0x200, bytesFromEnd);
            pseudoPos += bytesToRead;
            pointerInsideDecryptedSection = 0;
            return b.length;
        }  
        if (isEndPositionInsideEncryptedSection(bytesToRead)) {
            log.trace("2. End Position Inside Encrypted Section ("+pseudoPos+"-"+(pseudoPos+b.length)+")");
            int bytesTillEncrypted = (int) (mediaOffsetPositionStart - pseudoPos);
            int fullEncryptedBlocks = (bytesToRead - bytesTillEncrypted) / 0x200;
            int incompleteEncryptedBytes = (bytesToRead - bytesTillEncrypted) % 0x200;
            System.arraycopy(readChunk(bytesTillEncrypted), 0, b, 0, bytesTillEncrypted);
            //2
            for (int i = 0; i < fullEncryptedBlocks; i++) {
                fillDecryptedCache();
                System.arraycopy(decryptedBytes, 0, b, fullEncryptedBlocks+i*0x200, 0x200);
            }
            //3
            fillDecryptedCache();
            System.arraycopy(decryptedBytes, 0, b, bytesTillEncrypted+fullEncryptedBlocks*0x200, incompleteEncryptedBytes);
            pseudoPos += bytesToRead;
            pointerInsideDecryptedSection = incompleteEncryptedBytes;
            return b.length;
        }
        log.trace("3. Not encrypted ("+pseudoPos+"-"+(pseudoPos+b.length)+")");
        pseudoPos += bytesToRead;
        pointerInsideDecryptedSection = 0;
        return super.read(b);
    }
    private void fillDecryptedCache() throws IOException{
        try{
            decryptedBytes = decryptor.decryptNext(readChunk(0x200));
        }
        catch (Exception e){
            throw new IOException(e);
        }
    }
    private void resetAndSkip(long blockSum) throws IOException{
        try{
            decryptor.reset();
            decryptor.skipNext(blockSum);                                                   // recalculate
        }
        catch (Exception e){
            throw new IOException(e);
        }
    }
    private byte[] readChunk(int bytes) throws IOException{
        byte[] chunkBytes = new byte[bytes];
        long actuallyRead = super.read(chunkBytes);
        if (actuallyRead != bytes)
            throw new IOException("Can't read. Need block of "+ bytes +" while only " +
                    actuallyRead + " bytes.");
        return chunkBytes;
    }

    private boolean isPointerInsideEncryptedSection(){
        return (pseudoPos-pointerInsideDecryptedSection >= mediaOffsetPositionStart) && (pseudoPos-pointerInsideDecryptedSection < mediaOffsetPositionEnd);
    }
    private boolean isEndPositionInsideEncryptedSection(long requestedBytesCount){
        return ((pseudoPos-pointerInsideDecryptedSection + requestedBytesCount) >= mediaOffsetPositionStart) && ((pseudoPos-pointerInsideDecryptedSection + requestedBytesCount) < mediaOffsetPositionEnd);
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        if (isPointerInsideEncryptedSection()){
            long realCountOfBytesToSkip = n - (0x200 - pointerInsideDecryptedSection);
            if (realCountOfBytesToSkip <= 0){
                pseudoPos += n;
                pointerInsideDecryptedSection += n;
                return n;
            }

            if (isEndPositionInsideEncryptedSection(n)){ // If we need to move somewhere out of the encrypted section
                log.trace("4.1. Pointer Inside + End Position Inside Encrypted Section ("+pseudoPos+"-"+(pseudoPos+n)+")");
                long blocksToSkipCountingFromStart = (pseudoPos+n - mediaOffsetPositionStart) / 0x200;        // always positive
                resetAndSkip(blocksToSkipCountingFromStart);

                long leftovers = realCountOfBytesToSkip % 0x200;           // most likely will be 0;  TODO: a lot of tests
                long bytesToSkipTillRequiredBlock = realCountOfBytesToSkip - leftovers;
                skipLoop(bytesToSkipTillRequiredBlock);
                fillDecryptedCache();
                pseudoPos += n;
                pointerInsideDecryptedSection = (int) leftovers;
                return n;
            }
            log.trace("4. Pointer Inside + End Position Outside Encrypted Section ("+pseudoPos+"-"+(pseudoPos+n)+")");
            skipLoop(realCountOfBytesToSkip);
            pseudoPos += n;
            pointerInsideDecryptedSection = 0;
            return n;
            // just fast-forward to position we need and flush caches
        }

        if (isEndPositionInsideEncryptedSection(n)) {  //pointer will be inside Encrypted Section, but now outside
            log.trace("5. End Position Inside Encrypted Section ("+pseudoPos+"-"+(pseudoPos+n)+")");
            //skip to start if the block we need
            long bytesToSkipTillEncryptedBlock = mediaOffsetPositionStart - pseudoPos;
            long blocksToSkipCountingFromStart = (n - bytesToSkipTillEncryptedBlock) / 0x200;        // always positive
            long bytesToSkipTillRequiredBlock = bytesToSkipTillEncryptedBlock + blocksToSkipCountingFromStart * 0x200;
            long leftovers = n - bytesToSkipTillRequiredBlock;           // most likely will be 0;

            long skipped = super.skip(bytesToSkipTillRequiredBlock);
            if (bytesToSkipTillRequiredBlock != skipped)
                throw new IOException("Can't skip bytes. To skip: " +
                        bytesToSkipTillEncryptedBlock +
                        ".\nActually skipped: " + skipped +
                        ".\nLeftovers inside encrypted section: " + leftovers);
            log.trace("\tBlocks skipped "+blocksToSkipCountingFromStart);
            resetAndSkip(blocksToSkipCountingFromStart);
            fillDecryptedCache();
            pseudoPos += n;
            pointerInsideDecryptedSection = (int) leftovers;
            return n;
        }
        log.trace("6. Not encrypted ("+pseudoPos+"-"+(pseudoPos+n)+")");
        skipLoop(n);
        pseudoPos += n;
        pointerInsideDecryptedSection = 0;
        return n;
    }
    private void skipLoop(long size) throws IOException{
        long mustSkip = size;
        long skipped = 0;
        while (mustSkip > 0){
            skipped += super.skip(mustSkip);
            mustSkip = size - skipped;
            log.trace("Skip loop: skipped: "+skipped+"\tmustSkip "+mustSkip);
        }
    }
    @Override
    public synchronized int read() throws IOException {
        byte[] b = new byte[1];
        if (read(b) != -1)
            return b[0];
        return -1;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int readlimit) {}

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("Not supported");
    }
}