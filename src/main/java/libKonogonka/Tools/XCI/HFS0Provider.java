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

import libKonogonka.Tools.ExportAble;
import libKonogonka.Tools.ISuperProvider;
import libKonogonka.ctraes.InFileStreamProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static libKonogonka.Converter.*;

/**
 * HFS0
 * */
public class HFS0Provider extends ExportAble implements ISuperProvider {
    private final static Logger log = LogManager.getLogger(HFS0Provider.class);

    private final String magic;
    private final int filesCount;
    private final byte[] padding;
    private final int stringTableSize;
    private final long rawFileDataStart;

    private final HFS0File[] hfs0Files;

    private final File file;

    HFS0Provider(long hfsOffsetPosition, RandomAccessFile raf, File file) throws Exception{
        this.file = file;    // Will be used @ getHfs0FilePipedInpStream. It's a bad implementation.
        byte[] hfs0bytes = new byte[16];

        raf.seek(hfsOffsetPosition);
        if (raf.read(hfs0bytes) != 16){
            throw new Exception("Read HFS0 structure failure. Can't read first 16 bytes on requested offset.");
        }

        this.magic = new String(hfs0bytes, 0x0, 0x4, StandardCharsets.US_ASCII);
        this.filesCount = getLEint(hfs0bytes, 0x4);
        this.stringTableSize = getLEint(hfs0bytes, 8);
        this.padding = Arrays.copyOfRange(hfs0bytes, 12, 16);

        hfs0Files = new HFS0File[filesCount];

        // TODO: IF NOT EMPTY TABLE: add validation

        long[] offsetSubFile = new long[filesCount];
        long[] sizeSubFile = new long[filesCount];
        int[] hashedRegionSubFile = new int[filesCount];
        boolean[] paddingSubFile = new boolean[filesCount];
        byte[][] SHA256HashSubFile = new byte[filesCount][];
        int[] stringTableOffsetSubFile = new int[filesCount];

        try {
            // Populate meta information regarding each file inside (?) HFS0
            byte[] metaInfoBytes = new byte[64];
            for (int i = 0; i < filesCount; i++){
                if (raf.read(metaInfoBytes) != 64)
                    throw new Exception("Read HFS0 File Entry Table failure for file # "+i);
                offsetSubFile[i] = getLElong(metaInfoBytes, 0);
                sizeSubFile[i] = getLElong(metaInfoBytes, 8);
                hashedRegionSubFile[i] = getLEint(metaInfoBytes, 20);
                paddingSubFile[i] = Arrays.equals(Arrays.copyOfRange(metaInfoBytes, 24, 32), new byte[8]);
                SHA256HashSubFile[i] = Arrays.copyOfRange(metaInfoBytes, 32, 64);

                stringTableOffsetSubFile[i] = getLEint(metaInfoBytes, 16);
            }
            // Define location of actual data for this HFS0
            rawFileDataStart = raf.getFilePointer()+stringTableSize;
            if (stringTableSize <= 0)
                throw new Exception("String table size of HFS0 less or equal to zero");
            byte[] stringTbl = new byte[stringTableSize];
            if (raf.read(stringTbl) != stringTableSize){
                throw new Exception("Read HFS0 String table failure. Can't read requested string table size ("+stringTableSize+")");
            }
            String[] namesSubFile = new String[filesCount];
            // Parse string table
            for (int i = 0; i < filesCount; i++){
                int j = 0;
                while (stringTbl[stringTableOffsetSubFile[i]+j] != (byte)0x00)
                    j++;
                namesSubFile[i] = new String(stringTbl, stringTableOffsetSubFile[i], j, StandardCharsets.UTF_8);
            }
            //----------------------------------------------------------------------------------------------------------
            // Set files
            for (int i = 0; i < filesCount; i++){
                hfs0Files[i] = new HFS0File(
                        namesSubFile[i],
                        offsetSubFile[i],
                        sizeSubFile[i],
                        hashedRegionSubFile[i],
                        paddingSubFile[i],
                        SHA256HashSubFile[i]
                );
            }
        }
        catch (IOException ioe){
            throw new Exception("Read HFS0 structure failure: "+ioe.getMessage());
        }
    }

    public String getMagic() { return magic; }
    public int getFilesCount() { return filesCount; }
    public byte[] getPadding() { return padding; }
    public int getStringTableSize() { return stringTableSize; }
    @Override
    public long getRawFileDataStart() { return rawFileDataStart; }
    public HFS0File[] getHfs0Files() { return hfs0Files; }

    @Override
    public boolean exportContent(String saveToLocation, String subFileName) throws Exception {
        for (int i = 0; i < hfs0Files.length; i++) {
            if (hfs0Files[i].getName().equals(subFileName))
                return exportContent(saveToLocation, i);
        }
        throw new FileNotFoundException("No file with such name exists: " + subFileName);
    }

    @Override
    public boolean exportContent(String saveToLocation, int subFileNumber) throws Exception {
        HFS0File subFile = hfs0Files[subFileNumber];
        stream = getStreamProducer(subFileNumber).produce();
        return export(saveToLocation, subFile.getName(), 0, subFile.getSize());
    }
    @Override
    public InFileStreamProducer getStreamProducer(String subFileName) throws FileNotFoundException{
        for (int i = 0; i < hfs0Files.length; i++){
            if (hfs0Files[i].getName().equals(subFileName))
                return getStreamProducer(i);
        }
        throw new FileNotFoundException("No file with such name exists: "+subFileName);
    }
    @Override
    public InFileStreamProducer getStreamProducer(int subFileNumber) {
        long offset = rawFileDataStart + hfs0Files[subFileNumber].getOffset();
        return new InFileStreamProducer(file, offset);
    }

    @Override
    public File getFile() {
        return file;
    }
}