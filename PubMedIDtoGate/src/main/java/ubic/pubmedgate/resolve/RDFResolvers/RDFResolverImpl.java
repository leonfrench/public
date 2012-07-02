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

package ubic.pubmedgate.resolve.RDFResolvers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ubic.BAMSandAllen.Vocabulary;
import ubic.pubmedgate.resolve.MakeLexiconRDFModel;
import ubic.pubmedgate.resolve.ResolutionRDFModel;
import ubic.pubmedgate.resolve.mentionEditors.LowerTrimMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.MentionEditor;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public abstract class RDFResolverImpl implements RDFResolver {
    List<MentionEditor> editors;

    public RDFResolverImpl() {
        editors = new LinkedList<MentionEditor>();
        editors.add( new LowerTrimMentionEditor() );
    }

    public RDFResolverImpl( List<MentionEditor> editors ) {
        this.editors = editors;
    }

    public void addMentionEditor( MentionEditor me ) {
        editors.add( me );
    }

    /* run the input string through all of the mention editors to clean and edit it */
    public Set<String> processMentionString( String mention ) {
        Set<String> result = new HashSet<String>();
        result.add( mention );
        for ( MentionEditor editor : editors ) {
            Set<String> newResult = new HashSet<String>();
            for ( String mentionEdit : result ) {
                newResult.addAll( editor.editMention( mentionEdit ) );
            }
            result.addAll( newResult );
        }
        return result;
    }

    public Model resolve( Set<Resource> mentions ) {
        Model model = ModelFactory.createDefaultModel();
        for ( Resource mention : mentions ) {
            model = model.union( resolve( mention ) );
        }
        return model;
    }


}
