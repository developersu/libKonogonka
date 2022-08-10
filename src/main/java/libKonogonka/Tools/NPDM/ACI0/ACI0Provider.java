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
package libKonogonka.Tools.NPDM.ACI0;

import libKonogonka.Tools.NPDM.KernelAccessControlProvider;
import libKonogonka.Tools.NPDM.ServiceAccessControlProvider;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static libKonogonka.LoperConverter.getLEint;

public class ACI0Provider  {
    private String magicNum;
    private byte[] reserved1;
    private byte[] titleID;
    private byte[] reserved2;
    private int fsAccessHeaderOffset;
    private int fsAccessHeaderSize;
    private int serviceAccessControlOffset;
    private int serviceAccessControlSize;
    private int kernelAccessControlOffset;
    private int kernelAccessControlSize;
    private byte[] reserved3;

    private FSAccessHeaderProvider fsAccessHeaderProvider;
    private ServiceAccessControlProvider serviceAccessControlProvider;
    private KernelAccessControlProvider kernelAccessControlProvider;

    public ACI0Provider(byte[] aci0bytes) throws Exception {
        if (aci0bytes.length < 0x40)
            throw new Exception("ACI0 size is too short");
        magicNum = new String(aci0bytes, 0, 0x4, StandardCharsets.UTF_8);
        reserved1 = Arrays.copyOfRange(aci0bytes, 0x4, 0x10);
        titleID = Arrays.copyOfRange(aci0bytes, 0x10, 0x18);
        reserved2 = Arrays.copyOfRange(aci0bytes, 0x18, 0x20);
        fsAccessHeaderOffset = getLEint(aci0bytes, 0x20);
        fsAccessHeaderSize = getLEint(aci0bytes, 0x24);
        serviceAccessControlOffset = getLEint(aci0bytes, 0x28);
        serviceAccessControlSize = getLEint(aci0bytes, 0x2C);
        kernelAccessControlOffset = getLEint(aci0bytes, 0x30);
        kernelAccessControlSize = getLEint(aci0bytes, 0x34);
        reserved3 = Arrays.copyOfRange(aci0bytes, 0x38, 0x40);

        fsAccessHeaderProvider = new FSAccessHeaderProvider(Arrays.copyOfRange(aci0bytes, fsAccessHeaderOffset, fsAccessHeaderOffset+fsAccessHeaderSize));
        serviceAccessControlProvider = new ServiceAccessControlProvider(Arrays.copyOfRange(aci0bytes, serviceAccessControlOffset, serviceAccessControlOffset+serviceAccessControlSize));
        kernelAccessControlProvider = new KernelAccessControlProvider(Arrays.copyOfRange(aci0bytes, kernelAccessControlOffset, kernelAccessControlOffset+kernelAccessControlSize));
    }

    public String getMagicNum()  { return magicNum; }
    public byte[] getReserved1()  { return reserved1; }
    public byte[] getTitleID()  { return titleID; }
    public byte[] getReserved2()  { return reserved2; }
    public int getFsAccessHeaderOffset()  { return fsAccessHeaderOffset; }
    public int getFsAccessHeaderSize()  { return fsAccessHeaderSize; }
    public int getServiceAccessControlOffset()  { return serviceAccessControlOffset; }
    public int getServiceAccessControlSize()  { return serviceAccessControlSize; }
    public int getKernelAccessControlOffset()  { return kernelAccessControlOffset; }
    public int getKernelAccessControlSize()  { return kernelAccessControlSize; }
    public byte[] getReserved3()  { return reserved3; }

    public FSAccessHeaderProvider getFsAccessHeaderProvider() { return fsAccessHeaderProvider; }
    public ServiceAccessControlProvider getServiceAccessControlProvider() { return serviceAccessControlProvider; }
    public KernelAccessControlProvider getKernelAccessControlProvider() { return kernelAccessControlProvider; }
}