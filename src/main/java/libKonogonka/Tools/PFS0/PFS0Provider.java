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
import libKonogonka.Tools.NCA.NCASectionTableBlock.SuperBlockPFS0;
import libKonogonka.ctraes.AesCtrBufferedInputStream;
import libKonogonka.ctraes.AesCtrDecryptSimple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

import static libKonogonka.Converter.getLEint;
import static libKonogonka.Converter.getLElong;

public class PFS0Provider implements IPFS0Provider{
    private final static Logger log = LogManager.getLogger(PFS0Provider.class);

    private String magic;
    private int filesCount;
    private int stringTableSize;
    private byte[] padding;
    private PFS0subFile[] pfs0subFiles;
    //---------------------------------------
    private long rawBlockDataStart;

    private final File file;
    private long offsetPositionInFile;
    private long mediaStartOffset;  // In 512-blocks
    private long mediaEndOffset;    // In 512-blocks

    private long ncaOffset;
    private BufferedInputStream stream;
    private SuperBlockPFS0 superBlockPFS0;
    private AesCtrDecryptSimple decryptor;

    private LinkedList<byte[]> pfs0SHA256hashes;

    private boolean encrypted;

    public PFS0Provider(File nspFile) throws Exception{
        this.file = nspFile;
        createBufferedInputStream();
        readPfs0Header();
    }

    public PFS0Provider(File file,
                        long ncaOffset,
                        SuperBlockPFS0 superBlockPFS0,
                        long mediaStartOffset,
                        long mediaEndOffset) throws Exception{
        this.file = file;
        this.ncaOffset = ncaOffset;
        this.superBlockPFS0 = superBlockPFS0;
        this.offsetPositionInFile = ncaOffset + mediaStartOffset * 0x200;
        this.mediaStartOffset = mediaStartOffset;
        this.mediaEndOffset = mediaEndOffset;
        this.rawBlockDataStart = superBlockPFS0.getPfs0offset();
        //bufferedInputStream = new BufferedInputStream(Files.newInputStream(fileWithPfs0.toPath()));
        createBufferedInputStream();
        long toSkip = offsetPositionInFile + superBlockPFS0.getHashTableOffset();
        if (toSkip != stream.skip(toSkip))
            throw new Exception("Can't skip bytes prior Hash Table offset");
        collectHashes();

        createBufferedInputStream();
        toSkip = offsetPositionInFile + superBlockPFS0.getPfs0offset();
        if (toSkip != stream.skip(toSkip))
            throw new Exception("Can't skip bytes prior PFS0 offset");
        readPfs0Header();
    }

    public PFS0Provider(File file,
                        long ncaOffset,
                        SuperBlockPFS0 superBlockPFS0,
                        AesCtrDecryptSimple decryptor,
                        long mediaStartOffset,
                        long mediaEndOffset
    ) throws Exception {
        this.file = file;
        this.ncaOffset = ncaOffset;
        this.superBlockPFS0 = superBlockPFS0;
        this.decryptor = decryptor;
        this.offsetPositionInFile = ncaOffset + mediaStartOffset * 0x200;
        this.mediaStartOffset = mediaStartOffset;
        this.mediaEndOffset = mediaEndOffset;
        this.rawBlockDataStart = superBlockPFS0.getPfs0offset();
        this.encrypted = true;

        createAesCtrEncryptedBufferedInputStream();
        long toSkip = offsetPositionInFile + superBlockPFS0.getHashTableOffset();
        if (toSkip != stream.skip(toSkip))
            throw new Exception("Can't skip bytes prior Hash Table offset");
        collectHashes();

        createAesCtrEncryptedBufferedInputStream();
        toSkip = offsetPositionInFile + superBlockPFS0.getPfs0offset();
        if (toSkip != stream.skip(toSkip))
            throw new Exception("Can't skip bytes prior PFS0 offset");
        readPfs0Header();
    }

    private void readPfs0Header()throws Exception{
        byte[] fileStartingBytes = new byte[0x10];
        if (0x10 != stream.read(fileStartingBytes))
            throw new Exception("Reading stream suddenly ended while trying to read starting 0x10 bytes");

        // Update position
        rawBlockDataStart += 0x10;
        // Check PFS0Provider
        magic = new String(fileStartingBytes, 0x0, 0x4, StandardCharsets.US_ASCII);
        if (! magic.equals("PFS0")){
            throw new Exception("Bad magic");
        }
        // Get files count
        filesCount = getLEint(fileStartingBytes, 0x4);
        if (filesCount <= 0 ) {
            throw new Exception("Files count is too small");
        }
        // Get string table
        stringTableSize = getLEint(fileStartingBytes, 0x8);
        if (stringTableSize <= 0 ){
            throw new Exception("String table is too small");
        }
        padding = Arrays.copyOfRange(fileStartingBytes, 0xc, 0x10);
        //-------------------------------------------------------------------
        pfs0subFiles = new PFS0subFile[filesCount];

        long[] offsetsSubFiles = new long[filesCount];
        long[] sizesSubFiles = new long[filesCount];
        int[] strTableOffsets = new int[filesCount];
        byte[][] zeroBytes = new byte[filesCount][];

        byte[] fileEntryTable = new byte[0x18];
        for (int i=0; i < filesCount; i++){
            if (0x18 != stream.read(fileEntryTable))
                throw new Exception("Reading stream suddenly ended while trying to read File Entry Table #"+i);

            offsetsSubFiles[i] = getLElong(fileEntryTable, 0);
            sizesSubFiles[i] = getLElong(fileEntryTable, 0x8);
            strTableOffsets[i] = getLEint(fileEntryTable, 0x10);
            zeroBytes[i] = Arrays.copyOfRange(fileEntryTable, 0x14, 0x18);
            rawBlockDataStart += 0x18;
        }
        //*******************************************************************
        // In here pointer in front of String table
        String[] subFileNames = new String[filesCount];
        byte[] stringTbl = new byte[stringTableSize];
        if (stream.read(stringTbl) != stringTableSize){
            throw new Exception("Read PFS0Provider String table failure. Can't read requested string table size ("+stringTableSize+")");
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
                    zeroBytes[i]);
        }
        stream.close();
    }

    private void createAesCtrEncryptedBufferedInputStream() throws Exception{
        decryptor.reset();
        this.stream = new AesCtrBufferedInputStream(
                decryptor,
                ncaOffset,
                mediaStartOffset,
                mediaEndOffset,
                Files.newInputStream(file.toPath()));
    }

    private void createBufferedInputStream() throws Exception{
        this.stream = new BufferedInputStream(Files.newInputStream(file.toPath()));
    }

    private void collectHashes() throws Exception{
        pfs0SHA256hashes = new LinkedList<>();
        long hashTableOffset = superBlockPFS0.getHashTableOffset();
        long hashTableSize = superBlockPFS0.getHashTableSize();

        if (hashTableOffset > 0){
            if (hashTableOffset != stream.skip(hashTableOffset))
                throw new Exception("Unable to skip bytes till Hash Table Offset: "+hashTableOffset);
        }
        for (int i = 0; i < hashTableSize / 0x20; i++){
            byte[] sectionHash = new byte[0x20];
            if (0x20 != stream.read(sectionHash))
                throw new Exception("Unable to read hash");
            pfs0SHA256hashes.add(sectionHash);
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
    public long getRawFileDataStart() { return rawBlockDataStart;}
    @Override
    public PFS0subFile[] getPfs0subFiles() { return pfs0subFiles; }
    @Override
    public File getFile(){ return file; }

    @Override
    public boolean exportContent(String saveToLocation, String subFileName){
        for (int i = 0; i < pfs0subFiles.length; i++){
            if (pfs0subFiles[i].getName().equals(subFileName))
                return exportContent(saveToLocation, i);
        }
        return false;
    }
    @Override
    public boolean exportContent(String saveToLocation, int subFileNumber){
        PFS0subFile subFile = pfs0subFiles[subFileNumber];
        File location = new File(saveToLocation);
        location.mkdirs();

        try (BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                Files.newOutputStream(Paths.get(saveToLocation+File.separator+subFile.getName())))){
            if (encrypted)
                createAesCtrEncryptedBufferedInputStream();
            else
                createBufferedInputStream();

            long subFileSize = subFile.getSize();

            long toSkip = subFile.getOffset() + mediaStartOffset * 0x200 + rawBlockDataStart;
            if (toSkip != stream.skip(toSkip))
                throw new Exception("Unable to skip offset: "+toSkip);

            int blockSize = 0x200;
            if (subFileSize < 0x200)
                blockSize = (int) subFileSize;

            long i = 0;
            byte[] block = new byte[blockSize];

            int actuallyRead;
            while (true) {
                if ((actuallyRead = stream.read(block)) != blockSize)
                    throw new Exception("Read failure. Block Size: "+blockSize+", actuallyRead: "+actuallyRead);
                extractedFileBOS.write(block);
                i += blockSize;
                if ((i + blockSize) > subFileSize) {
                    blockSize = (int) (subFileSize - i);
                    if (blockSize == 0)
                        break;
                    block = new byte[blockSize];
                }
            }
        }
        catch (Exception e){
            log.error("File export failure", e);
            return false;
        }
        return true;
    }

    //TODO: REMOVE
    @Override
    public PipedInputStream getProviderSubFilePipedInpStream(String subFileName) throws Exception {return null;}
    @Override
    public PipedInputStream getProviderSubFilePipedInpStream(int subFileNumber) throws Exception {return null;}


    public LinkedList<byte[]> getPfs0SHA256hashes() {
        return pfs0SHA256hashes;
    }

    public void printDebug(){
        log.debug(".:: PFS0Provider ::.\n" +
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