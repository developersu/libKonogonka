/*
 * Copyright 2019-2022 Dmitry Isaenko
 *
 * This file is part of libKonogonka.
 *
 * libKonogonka is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * libKonogonka is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with libKonogonka.  If not, see <https://www.gnu.org/licenses/>.
 */

package libKonogonka.Tools.RomFs;

import java.io.File;
import java.io.PipedInputStream;

public interface IRomFsProvider {
    long getLevel6Offset();
    Level6Header getHeader();
    FileSystemEntry getRootEntry();
    PipedInputStream getContent(FileSystemEntry entry) throws Exception;
    File getFile();
}
