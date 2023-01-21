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
package libKonogonka.fs.RomFs.view;

import libKonogonka.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.RainbowDump.formatDecHexString;

public class FileMetaTablePlainView {
    private final static Logger log = LogManager.getLogger(FileMetaTablePlainView.class);

    public FileMetaTablePlainView(long fileMetadataTableLength, byte[] fileMetadataTable) {
        int i = 0;
        while (i < fileMetadataTableLength) {
            FileMeta fileMeta = new FileMeta();
            fileMeta.containingDirectoryOffset = Converter.getLEint(fileMetadataTable, i);
            i += 4;
            fileMeta.nextSiblingFileOffset = Converter.getLEint(fileMetadataTable, i);
            i += 4;
            fileMeta.fileDataOffset = Converter.getLElong(fileMetadataTable, i);
            i += 8;
            fileMeta.fileDataLength = Converter.getLElong(fileMetadataTable, i);
            i += 8;
            fileMeta.nextFileOffset = Converter.getLEint(fileMetadataTable, i);
            i += 4;
            fileMeta.fileNameLength = Converter.getLEint(fileMetadataTable, i);
            i += 4;
            fileMeta.fileName = new String(Arrays.copyOfRange(fileMetadataTable, i, i + fileMeta.fileNameLength), StandardCharsets.UTF_8);

            i += getRealNameSize(fileMeta.fileNameLength);

            log.debug(
                    "- FILE -\n" +
                    "Offset of Containing Directory                    " + formatDecHexString(fileMeta.containingDirectoryOffset) + "\n" +
                    "Offset of next Sibling File                       " + formatDecHexString(fileMeta.nextSiblingFileOffset) + "\n" +
                    "Offset of File's Data                             " + formatDecHexString(fileMeta.fileDataOffset) + "\n" +
                    "Length of File's Data                             " + formatDecHexString(fileMeta.fileDataLength) + "\n" +
                    "Offset of next File in the same Hash Table bucket " + formatDecHexString(fileMeta.nextFileOffset) + "\n" +
                    "Name Length                                       " + formatDecHexString(fileMeta.fileNameLength) + "\n" +
                    "Name Length (rounded up to multiple of 4)         " + fileMeta.fileName + "\n"
            );
        }
    }

    private int getRealNameSize(int value){
        if (value % 4 == 0)
            return value;
        return value + 4 - value % 4;
    }

    private static class FileMeta{
        int containingDirectoryOffset;
        int nextSiblingFileOffset;
        long fileDataOffset;
        long fileDataLength;
        int nextFileOffset;
        int fileNameLength;
        String fileName;
    }
}
