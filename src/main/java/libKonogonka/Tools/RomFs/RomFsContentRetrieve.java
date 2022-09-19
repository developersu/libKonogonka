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

import libKonogonka.ctraes.AesCtrBufferedInputStream;
import libKonogonka.ctraes.AesCtrDecryptSimple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;

public class RomFsContentRetrieve implements Runnable{
    private final static Logger log = LogManager.getLogger(RomFsContentRetrieve.class);

    private final PipedOutputStream streamOut;
    private final long internalFileSize;
    private final long startPosition;
    private final BufferedInputStream bis;

    RomFsContentRetrieve(File parentFile,
                         PipedOutputStream streamOut,
                         long internalFileRealPosition,
                         long internalFileSize) throws Exception{
        this.streamOut = streamOut;
        this.internalFileSize = internalFileSize;

        this.startPosition = internalFileRealPosition;
        this.bis = new BufferedInputStream(Files.newInputStream(parentFile.toPath()));
    }

    RomFsContentRetrieve(File parentFile,
                         PipedOutputStream streamOut,
                         AesCtrDecryptSimple decryptor,
                         long entryOffset,
                         long internalFileSize,
                         long headersFileDataOffset,        //level6Header.getFileDataOffset()
                         long level6Offset,
                         long ncaOffsetPosition,
                         long mediaStartOffset,
                         long mediaEndOffset
                                  ) throws Exception{
        log.fatal("Current implementation works incorrectly");
        this.streamOut = streamOut;
        this.internalFileSize = internalFileSize;

        this.startPosition = entryOffset + mediaStartOffset*0x200 + headersFileDataOffset + level6Offset;

        this.bis = new AesCtrBufferedInputStream(
                decryptor,
                ncaOffsetPosition,
                mediaStartOffset,
                mediaEndOffset,
                Files.newInputStream(parentFile.toPath())
        );
    }

    @Override
    public void run() {
        log.trace("Executing thread");
        try {
            skipBytesTillBegining();

            int readPiece = 8388608;
            long readFrom = 0;
            byte[] readBuffer;

            while (readFrom < internalFileSize) {
                if (internalFileSize - readFrom < readPiece)
                    readPiece = Math.toIntExact(internalFileSize - readFrom);
                readBuffer = new byte[readPiece];
                if (bis.read(readBuffer) != readPiece) {
                    log.error("getContent(): Unable to read requested size from file.");
                    return;
                }
                streamOut.write(readBuffer);
                readFrom += readPiece;
            }
        } catch (Exception exception) {
            log.error("RomFsProvider -> getContent(): Unable to provide stream", exception);
        }
        finally {
            closeStreams();
        }
        log.trace("Thread died");
    }
    private void skipBytesTillBegining() throws Exception{
        long mustSkip = startPosition;
        long skipped = 0;
        while (mustSkip > 0){
            skipped += bis.skip(mustSkip);
            mustSkip = startPosition - skipped;
        }
    }
    private void closeStreams(){
        try {
            streamOut.close();
        }
        catch (IOException e){
            log.error("RomFsProvider -> getContent(): Unable to close 'StreamOut'");
        }
        try {
            bis.close();
        }
        catch (IOException e){
            log.error("RomFsProvider -> getContent(): Unable to close 'StreamOut'");
        }
    }
}
