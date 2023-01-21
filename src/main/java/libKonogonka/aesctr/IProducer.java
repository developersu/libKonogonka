package libKonogonka.aesctr;

import java.io.BufferedInputStream;
import java.io.File;

public interface IProducer {
    BufferedInputStream produce() throws Exception;
    IProducer getSuccessor(long subOffset) throws Exception;
    boolean isEncrypted();
    File getFile();
}
