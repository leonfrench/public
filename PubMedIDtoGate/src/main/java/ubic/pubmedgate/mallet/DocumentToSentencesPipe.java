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

package ubic.pubmedgate.mallet;

import gate.Annotation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ubic.pubmedgate.ConnectionsDocument;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;

public class DocumentToSentencesPipe extends Pipe {
    String annotationSet;
    int docCount = 0;

    public DocumentToSentencesPipe( String annotationSet ) {
        this.annotationSet = annotationSet;
    }

    public Iterator<Instance> newIteratorFrom( Iterator<Instance> source ) {
        List<Instance> output = new LinkedList<Instance>();
        // getData is a ConnectionDocument
        for ( ; source.hasNext(); ) {
            docCount++;
            ConnectionsDocument doc = ( ConnectionsDocument ) source.next().getData();

            // List<Annotation> sentences = new LinkedList<Annotation>();
            // sentences.add( doc.getTitle() );
            // sentences.add( doc.getAbstract() );
            // make a new instance for all the sentences
            List<Annotation> sentences = doc.getGATESentences( annotationSet );
            String PMID = doc.getPMID();
            if ( PMID == null ) PMID = "" + docCount;
            int i = 0;
            for ( Annotation sentence : sentences ) {
//                System.out.println("Sentence:"+ doc.getAnnotationText( sentence ));
//                System.out.println("PMID:" + PMID);
//                System.out.println();
                
                if (doc.getAnnotationText( sentence ).equals( PMID )) {
                    System.out.println("Filtering PMID:" + PMID);
                    continue;
                }
                // System.out.println( doc.getAnnotationText( sentence ) );
                // form instance
                // null target as it's stored in the sentence, the TargetFromGATE pipe will fix that
                Instance instance = new Instance( sentence, null, "PM" + PMID + ".Sentence." + i, doc );
                output.add( instance );
                i++;
            }
        }

        return output.iterator();
    }
}
