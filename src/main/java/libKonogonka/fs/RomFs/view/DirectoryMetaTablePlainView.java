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

public class DirectoryMetaTablePlainView {

    private final static Logger log = LogManager.getLogger(DirectoryMetaTablePlainView.class);

    // directoryMetadataTableLength must be declared since directoryMetadataTable could be bigger than declared size for encrypted blocks
    public DirectoryMetaTablePlainView(long directoryMetadataTableLength, byte[] directoryMetadataTable){
        int i = 0;
        while (i < directoryMetadataTableLength){
            FolderMeta folderMeta = new FolderMeta();
            folderMeta.parentDirectoryOffset = Converter.getLEint(directoryMetadataTable, i);
            i += 4;
            folderMeta.nextSiblingDirectoryOffset = Converter.getLEint(directoryMetadataTable, i);
            i += 4;
            folderMeta.firstSubdirectoryOffset = Converter.getLEint(directoryMetadataTable, i);
            i += 4;
            folderMeta.firstFileOffset = Converter.getLEint(directoryMetadataTable, i);
            i += 4;
            folderMeta.nextDirectoryOffset = Converter.getLEint(directoryMetadataTable, i);
            i += 4;
            folderMeta.dirNameLength = Converter.getLEint(directoryMetadataTable, i);
            i += 4;
            folderMeta.dirName = new String(Arrays.copyOfRange(directoryMetadataTable, i, i + folderMeta.dirNameLength), StandardCharsets.UTF_8);
            i += getRealNameSize(folderMeta.dirNameLength);

            log.debug(
                    "- DIRECTORY -\n" +
                    "Offset of Parent Directory (self if Root)              " + formatDecHexString(folderMeta.parentDirectoryOffset     ) +"\n" +
                    "Offset of next Sibling Directory                       " + formatDecHexString(folderMeta.nextSiblingDirectoryOffset) +"\n" +
                    "Offset of first Child Directory (Subdirectory)         " + formatDecHexString(folderMeta.firstSubdirectoryOffset   ) +"\n" +
                    "Offset of first File (in File Metadata Table)          " + formatDecHexString(folderMeta.firstFileOffset           ) +"\n" +
                    "Offset of next Directory in the same Hash Table bucket " + formatDecHexString(folderMeta.nextDirectoryOffset       ) +"\n" +
                    "Name Length                                            " + formatDecHexString(folderMeta.dirNameLength             ) +"\n" +
                    "Name Length (rounded up to multiple of 4)              " + folderMeta.dirName +                                       "\n"
            );
        }
    }

    private int getRealNameSize(int value){
        if (value % 4 == 0)
            return value;
        return value + 4 - value % 4;
    }

    private static class FolderMeta {
        int parentDirectoryOffset;
        int nextSiblingDirectoryOffset;
        int firstSubdirectoryOffset;
        int firstFileOffset;
        int nextDirectoryOffset;
        int dirNameLength;
        String dirName;
    }
}
