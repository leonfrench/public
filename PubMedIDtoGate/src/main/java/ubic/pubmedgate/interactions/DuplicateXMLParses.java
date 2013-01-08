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

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;

import ubic.pubmedgate.Config;

/**
 * Bit of a hack to create additional XML entries for other parsers.
 * 
 * @author leon
 */
public class DuplicateXMLParses {
    protected static Log log = LogFactory.getLog( DuplicateXMLParses.class );

    public static void main( String[] args ) throws Exception {
        DuplicateXMLParses parser;
        // parser = new DuplicateXMLParses( "WhiteTextUnseen" );
        parser = new DuplicateXMLParses( "WhiteTextNegFixFullCountCheck" );

        // parser = new DuplicateXMLParses( "WhiteTextNegFixTrain" );

    }

    public DuplicateXMLParses( String baseName ) throws Exception {
        String filename = Config.config.getString( "whitetext.iteractions.ppiBaseFolder" )
                + "Corpora/Syntax-Tree-Learning-Format/" + baseName + ".xml";
        if ( !( new File( filename ) ).exists() ) {
            log.info( "Cant find file at :" + filename );
            System.exit( 1 );
        }
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read( filename );
        String path;
        Map<String, String> attributes;

        // only run once
        path = "//corpus/document/sentence/sentenceanalyses/parses/parse";
        attributes = new HashMap<String, String>();
        attributes.put( "tokenizer", "split" );
        attributes.put( "parser", "split-parse" );

        fixXML( document, path, attributes );

        // path = "//corpus/document/sentence/sentenceanalyses/parses/parse";
        // attributes = new HashMap<String, String>();
        // // might dupicate
        // attributes.put( "parser", "Charniak-Lease" );
        //
        // fixXML( document, path, attributes );

        path = "//corpus/document/sentence/sentenceanalyses/tokenizations/tokenization";
        attributes = new HashMap<String, String>();
        attributes.put( "tokenizer", "split" );

        fixXML( document, path, attributes );

        // what about bracketings?

        // open xml file
        // iterate documents
        // iterate sentences
        // duplicate senteence parses
        FileWriter fout = new FileWriter( filename );
        document.write( fout );
        fout.close();
    }

    private void fixXML( Document document, String path, Map<String, String> attributes ) {
        List list = document.selectNodes( path );
        Iterator iter = list.iterator();
        while ( iter.hasNext() ) {
            String attribute = attributes.keySet().iterator().next();
            Element el = ( Element ) iter.next();

            if ( el.attribute( "parser" ) != null
                    && el.attribute( "parser" ).getValue().equals( "Charniak-Johnson-McClosky" ) ) {
                el.addAttribute( "parser", "Charniak-Lease" );
            }

            String tokenizer = el.attribute( attribute ).getValue();
            String replacement = attributes.get( attribute );
            if ( !tokenizer.equals( replacement ) ) {
                log.info( tokenizer + " -> " + replacement );
                Element newElement = el.createCopy();
                for ( String attributeFix : attributes.keySet() ) {
                    newElement.addAttribute( new QName( attributeFix ), attributes.get( attributeFix ) );
                }
                el.getParent().add( newElement );
            }

        }
    }
}
