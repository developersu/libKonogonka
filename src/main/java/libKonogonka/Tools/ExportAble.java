package libKonogonka.Tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class ExportAble {
    protected BufferedInputStream stream;

    protected boolean export(String saveTo, String fileName, long skip, long size) throws Exception{
        if (skip != stream.skip(skip))
            throw new Exception("Can't seek to start position: "+skip);

        File location = new File(saveTo);
        location.mkdirs();

        try (BufferedOutputStream extractedFileBOS = new BufferedOutputStream(
                Files.newOutputStream(Paths.get(saveTo+File.separator+fileName)))){

            int blockSize = 0x200;
            if (size < 0x200)
                blockSize = (int) size;

            long i = 0;
            byte[] block = new byte[blockSize];

            int actuallyRead;
            while (true) {
                if ((actuallyRead = stream.read(block)) != blockSize)
                    throw new Exception("Read failure. "+blockSize+"/"+actuallyRead);
                extractedFileBOS.write(block);
                i += blockSize;
                if ((i + blockSize) > size) {
                    blockSize = (int) (size - i);
                    if (blockSize == 0)
                        break;
                    block = new byte[blockSize];
                }
            }
        }
        stream.close();
        return true;
    }
}
