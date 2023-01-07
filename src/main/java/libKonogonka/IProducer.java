package libKonogonka;

import java.io.BufferedInputStream;
import java.io.File;

public interface IProducer {

    BufferedInputStream produce() throws Exception;
    IProducer getSuccessor(long subOffset);
    boolean isEncrypted();
    File getFile();
}
