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
package libKonogonka.Tools.RomFs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.file.Files;

public class RomFsDecryptedContentRetrieve implements Runnable {
    private final static Logger log = LogManager.getLogger(RomFsDecryptedContentRetrieve.class);

    private final File parentFile;
    private final PipedOutputStream streamOut;
    private final long internalFileRealPosition;
    private final long internalFileSize;

    RomFsDecryptedContentRetrieve(File parentFile,
                                  PipedOutputStream streamOut,
                                  long internalFileRealPosition,
                                  long internalFileSize){
        this.parentFile = parentFile;
        this.streamOut = streamOut;
        this.internalFileRealPosition = internalFileRealPosition;
        this.internalFileSize = internalFileSize;
    }

    @Override
    public void run() {
        log.trace("Executing thread");
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(parentFile.toPath()))){
            fastForwardBySkippingBytes(bis, internalFileRealPosition);

            int readPice = 8388608; // 8mb NOTE: consider switching to 1mb 1048576
            long readFrom = 0;
            byte[] readBuffer;

            while (readFrom < internalFileSize) {
                if (internalFileSize - readFrom < readPice)
                    readPice = Math.toIntExact(internalFileSize - readFrom);    // it's safe, I guarantee
                readBuffer = new byte[readPice];
                if (bis.read(readBuffer) != readPice) {
                    log.error("getContent(): Unable to read requested size from file.");
                    return;
                }
                streamOut.write(readBuffer);
                readFrom += readPice;
            }
        } catch (Exception exception) {
            log.error("RomFsDecryptedProvider -> getContent(): Unable to provide stream", exception);
        }
        finally {
            closeStreamOut();
        }
        log.trace("Thread died");
    }
    private void fastForwardBySkippingBytes(BufferedInputStream bis, long size) throws Exception{
        long mustSkip = size;
        long skipped = 0;
        while (mustSkip > 0){
            skipped += bis.skip(mustSkip);
            mustSkip = size - skipped;
        }
    }
    private void closeStreamOut(){
        try {
            streamOut.close();
        }
        catch (IOException e){
            log.error("RomFsDecryptedProvider -> getContent(): Unable to close 'StreamOut'");
        }
    }
}
