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
package libKonogonka.Tools.NSO;

public class NSO0Raw {
    private NSO0Header headerObject;
    private final byte[] header;
    private final byte[] _textDecompressedSection;
    private final byte[] _rodataDecompressedSection;
    private final byte[] _dataDecompressedSection;

    NSO0Raw(byte[] header,
            byte[] _textDecompressedSection,
            byte[] _rodataDecompressedSection,
            byte[] _dataDecompressedSection){
        this.header = header;
        this._textDecompressedSection = _textDecompressedSection;
        this. _rodataDecompressedSection = _rodataDecompressedSection;
        this._dataDecompressedSection = _dataDecompressedSection;
        try {
            this.headerObject = new NSO0Header(header);
        }
        catch (Exception e){ e.printStackTrace(); } //never happens
    }

    public NSO0Header getHeader() { return headerObject; }
    public byte[] getHeaderRaw() {return header;}
    public byte[] getTextRaw() {return _textDecompressedSection;}
    public byte[] getRodataRaw() {return _rodataDecompressedSection;}
    public byte[] getDataRaw() {return _dataDecompressedSection;}
}
