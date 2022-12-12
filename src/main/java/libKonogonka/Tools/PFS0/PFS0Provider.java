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

import libKonogonka.RainbowDump;
import libKonogonka.Tools.ISuperProvider;
import libKonogonka.Tools.NCA.NCASectionTableBlock.SuperBlockPFS0;
import libKonogonka.ctraes.InFileStreamProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

public class PFS0Provider implements ISuperProvider {
    private final static Logger log = LogManager.getLogger(PFS0Provider.class);

    private final long rawBlockDataStart;

    private long offsetPositionInFile;
    private final InFileStreamProducer producer;
    private BufferedInputStream stream;
    private SuperBlockPFS0 superBlockPFS0;

    private long mediaStartOffset;

    private final PFS0Header header;
    private LinkedList<byte[]> pfs0SHA256hashes;

    public PFS0Provider(File nspFile) throws Exception{
        this.producer = new InFileStreamProducer(nspFile);
        this.stream = producer.produce();
        this.header = new PFS0Header(stream);
        this.rawBlockDataStart = 0x10L + (header.getFilesCount() * 0x18L) + header.getStringTableSize();
    }

    public PFS0Provider(InFileStreamProducer producer,
                        long offsetPositionInFile,
                        SuperBlockPFS0 superBlockPFS0,
                        long mediaStartOffset) throws Exception {
        this.producer = producer;
        this.offsetPositionInFile = offsetPositionInFile;
        this.superBlockPFS0 = superBlockPFS0;
        this.mediaStartOffset = mediaStartOffset;

        this.stream = producer.produce();
        long toSkip = offsetPositionInFile + superBlockPFS0.getHashTableOffset();
        if (toSkip != stream.skip(toSkip))
            throw new Exception("Can't skip bytes prior Hash Table offset");
        collectHashes();

        this.stream = producer.produce();
        toSkip = offsetPositionInFile + superBlockPFS0.getPfs0offset();
        if (toSkip != stream.skip(toSkip))
            throw new Exception("Can't skip bytes prior PFS0 offset");
        this.header = new PFS0Header(stream);
        this.rawBlockDataStart = superBlockPFS0.getPfs0offset() + 0x10L + (header.getFilesCount() * 0x18L) + header.getStringTableSize();
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

    public boolean isEncrypted() { return producer.isEncrypted(); }
    public PFS0Header getHeader() {return header;}

    @Override
    public long getRawFileDataStart() { return rawBlockDataStart;}
    @Override
    public boolean exportContent(String saveToLocation, String subFileName){
        PFS0subFile[] pfs0subFiles = header.getPfs0subFiles();
        for (int i = 0; i < pfs0subFiles.length; i++){
            if (pfs0subFiles[i].getName().equals(subFileName))
                return exportContent(saveToLocation, i);
        }
        return false;
    }
    @Override
    public boolean exportContent(String saveToLocation, int subFileNumber){
        PFS0subFile subFile = header.getPfs0subFiles()[subFileNumber];
        File location = new File(saveToLocation);
        location.mkdirs();

        try (BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                Files.newOutputStream(Paths.get(saveToLocation+File.separator+subFile.getName())))){

            this.stream = producer.produce();

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

    @Override
    public InFileStreamProducer getStreamProducer(String subFileName) throws FileNotFoundException {
        PFS0subFile[] pfs0subFiles = header.getPfs0subFiles();
        for (int i = 0; i < pfs0subFiles.length; i++) {
            if (pfs0subFiles[i].getName().equals(subFileName))
                return getStreamProducer(i);
        }
        throw new FileNotFoundException("No file with such name exists: "+subFileName);
    }
    @Override
    public InFileStreamProducer getStreamProducer(int subFileNumber) {
        PFS0subFile subFile = header.getPfs0subFiles()[subFileNumber];
        long subFileOffset = subFile.getOffset() + mediaStartOffset * 0x200 + rawBlockDataStart;

        return producer.getSuccessor(subFileOffset);
    }

    public LinkedList<byte[]> getPfs0SHA256hashes() {
        return pfs0SHA256hashes;
    }

    @Override
    public File getFile() {
        return producer.getFile();
    }

    public void printDebug(){
        log.debug(".:: PFS0Provider ::.\n" +
                "File name:                " + getFile().getName() + "\n" +
                "Raw block data start      " + RainbowDump.formatDecHexString(rawBlockDataStart) + "\n" +
                "Offset position in file   " + RainbowDump.formatDecHexString(offsetPositionInFile) + "\n" +
                "Media Start Offset        " + RainbowDump.formatDecHexString(mediaStartOffset) + "\n"
        );
        header.printDebug();
    }
}