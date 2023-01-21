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

import libKonogonka.fs.RomFs.FileSystemEntry;
import java.util.List;

/**
 * Used in pair with FileSystemEntry
 * */
public class FileSystemTreeViewMaker {
    private StringBuilder tree;
    private int spacerForSizes;

    private FileSystemTreeViewMaker(){}

    private void init(List<FileSystemEntry> content){
        tree = new StringBuilder("/\n");
        for (FileSystemEntry entry: content)
            printEntry(2, entry);
    }

    private void printEntry(int count, FileSystemEntry entry) {
        int i;
        for (i = 0; i < count; i++)
            tree.append(" ");

        if (entry.isDirectory()) {
            tree.append("|-");
            tree.append(entry.getName());
            tree.append("\n");
            for (FileSystemEntry e : entry.getContent())
                printEntry(count + 2, e);
            return;
        }

        tree.append("|-");
        tree.append(entry.getName());
        tree.append(String.format("%"+(spacerForSizes-entry.getName().length()-i)+"s0x%-10x 0x%-10x", "", entry.getOffset(), entry.getSize()));
        tree.append("\n");
    }

    public static String make(List<FileSystemEntry> content, int spacerForSizes){
        FileSystemTreeViewMaker maker = new FileSystemTreeViewMaker();
        maker.spacerForSizes = spacerForSizes;
        maker.init(content);
        return maker.tree.toString();
    }
}
