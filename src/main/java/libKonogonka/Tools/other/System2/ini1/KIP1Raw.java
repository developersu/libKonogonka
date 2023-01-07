/*
    Copyright 2019-2023 Dmitry Isaenko
     
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
package libKonogonka.Tools.other.System2.ini1;

public class KIP1Raw {
    private KIP1Header headerObject;
    private final byte[] header;
    private final byte[] _textDecompressedSection;
    private final byte[] _rodataDecompressedSection;
    private final byte[] _dataDecompressedSection;

    KIP1Raw(byte[] header,
            byte[] _textDecompressedSection,
            byte[] _rodataDecompressedSection,
            byte[] _dataDecompressedSection){
        this.header = header;
        this._textDecompressedSection = _textDecompressedSection;
        this. _rodataDecompressedSection = _rodataDecompressedSection;
        this._dataDecompressedSection = _dataDecompressedSection;
        try {
            this.headerObject = new KIP1Header(header);
        }
        catch (Exception e){ e.printStackTrace(); }
    }

    public KIP1Header getHeader() { return headerObject; }
    public byte[] getHeaderRaw() {return header;}
    public byte[] getTextRaw() {return _textDecompressedSection;}
    public byte[] getRodataRaw() {return _rodataDecompressedSection;}
    public byte[] getDataRaw() {return _dataDecompressedSection;}
}
