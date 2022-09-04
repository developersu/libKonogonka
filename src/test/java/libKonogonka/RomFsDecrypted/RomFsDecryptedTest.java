/*
    Copyright 2018-2022 Dmitry Isaenko
     
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
package libKonogonka.RomFsDecrypted;

import java.io.File;
import java.nio.file.Path;

import libKonogonka.Tools.RomFs.RomFsDecryptedProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// log.fatal("Configuration File Defined To Be :: "+System.getProperty("log4j.configurationFile"));

public class RomFsDecryptedTest {
    @TempDir
    Path mainLogsDir;

    private static final String decryptedFileAbsolutePath = "./FilesForTests/NCAContent_0 [lv6 147456].bin";
    private File decryptedFile;
    long lv6offset;
    RomFsDecryptedProvider provider;

    @Disabled
    @DisplayName("RomFsDecryptedProvider: tests")
    @Test
    void romFsValidation() throws Exception{
        makeFile();
        parseLv6offsetFromFileName();
        makeProvider();
        provider.printDebug();
    }

    void makeFile(){
        decryptedFile = new File(decryptedFileAbsolutePath);
    }
    void parseLv6offsetFromFileName(){
        lv6offset = Long.parseLong(decryptedFile.getName().replaceAll("(^.*lv6\\s)|(]\\.bin)", ""));
    }
    void makeProvider() throws Exception{
        provider = new RomFsDecryptedProvider(decryptedFile, lv6offset);
    }

/*
    void checkFilesWorkers(){
        assertTrue(fw1 instanceof WorkerFiles);
        assertTrue(fw2 instanceof WorkerFiles);
        assertTrue(fw3 instanceof WorkerFiles);
    }

 */
}