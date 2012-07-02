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

package ubic.pubmedgate;

import gate.Corpus;
import gate.corpora.CorpusImpl;
import gate.util.FMeasure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SubSetUtil {

    /*
     * return a random subset of a list
     */
    static public List getSubset( List items, int size, int seed ) {
        Random rand = new Random( seed ); // would make this static to the class

        List subsetList = new ArrayList( size );
        for ( int i = 0; i < size; i++ ) {
            // be sure to use Vector.remove() or you may get the same item twice
            subsetList.add( items.remove( rand.nextInt( items.size() ) ) );
        }
        return subsetList;
    }

    static public Corpus getCorpusSubset( Corpus corpOld, int size, int seed ) {
        Corpus newCorp = new CorpusImpl();
        List<ConnectionsDocument> corpDocs = GateInterface.getDocuments( corpOld );

        corpDocs = getSubset( corpDocs, size, seed );
        for ( ConnectionsDocument doc : corpDocs ) {
            newCorp.add( doc.getDocument() );
        }
        return newCorp;
    }
}
