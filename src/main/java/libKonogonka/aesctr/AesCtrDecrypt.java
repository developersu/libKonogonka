package libKonogonka.aesctr;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public abstract class AesCtrDecrypt {
    protected AesCtrDecrypt(){
        if (Security.getProvider("BC") == null)
            Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Decrypts next block of bytes. Usually 0x200.
     * @param encryptedBlock Encrypted bytes
     * @return Decrypted bytes
     */
    abstract public byte[] decryptNext(byte[] encryptedBlock);
    /**
     * Initializes cipher again using updated IV (CTR)
     * @param blockCount - how many blockCount from encrypted section start should be skipped. Block size = 0x200
     * */
    abstract public void resetAndSkip(long blockCount) throws Exception;
    /**
     * Initializes cipher again using initial IV (CTR)
     * */
    abstract public void reset() throws Exception;
}
