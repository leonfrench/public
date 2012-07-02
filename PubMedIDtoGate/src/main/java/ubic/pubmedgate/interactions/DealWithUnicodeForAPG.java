/*
 * The WhiteText project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.pubmedgate.interactions;

import java.nio.charset.Charset;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ubic.basecode.util.FileTools;

public class DealWithUnicodeForAPG {

    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception {
        String filename = "/home/leon/ppi-benchmark/Corpora/Original-Modified/WhiteText.xml";
        List<String> lines = FileTools.getLines( filename );
        List<String> fixedLines = new LinkedList<String>();
        Set<String> badChars = new HashSet<String>();

        for ( String line : lines ) {
            // Charset z;
            line = removeDiacriticalMarks( line );

            boolean bad = false;
            for ( Byte b : line.getBytes( "UTF8" ) ) {
                char c = ( char ) b.byteValue();
                if ( !isAscii( c ) ) {
                    System.out.println( b );
                    badChars.add( new String( b + "" ) );
                    System.out.println( b );
                    System.out.println( c );
                    bad = true;
                }
            }
            if ( bad ) {
                String newString = "";

                char previous = ' ';
                for ( Byte b : line.getBytes( "UTF8" ) ) {
                    char c = ( char ) b.byteValue();
                    if ( !isAscii( c ) ) {
                        // replace with space to preserve spans
                        if ( !isAscii( previous ) ) newString = newString + " ";
                    } else {
                        newString = newString + c;
                    }
                    previous = c;
                }
                System.out.println( "befor:" + line );
                line = newString;
                System.out.println( "After:" + line );

            }
            fixedLines.add( line );
        }
        System.out.println( badChars );
        System.out.println( badChars.size() );
        FileTools.stringsToFile( fixedLines, filename + ".charFix" );
    }

    public static boolean isAscii( char ch ) {
        return ch < 128;
    }

    public static String removeDiacriticalMarks( String string ) {
        return Normalizer.normalize( string, Form.NFD ).replaceAll( "\\p{InCombiningDiacriticalMarks}+", "" );
    }

}
