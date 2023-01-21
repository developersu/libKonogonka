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
package libKonogonka.Tools;

import libKonogonka.aesctr.InFileStreamProducer;

import java.io.File;

/**
 * Any class of this type must provide streams
 * */
public interface ISuperProvider {
    InFileStreamProducer getStreamProducer(String subFileName) throws Exception;
    InFileStreamProducer getStreamProducer(int subFileNumber) throws Exception;
    boolean exportContent(String saveToLocation, String subFileName) throws Exception;
    boolean exportContent(String saveToLocation, int subFileNumber) throws Exception;
    File getFile();
    long getRawFileDataStart();
}