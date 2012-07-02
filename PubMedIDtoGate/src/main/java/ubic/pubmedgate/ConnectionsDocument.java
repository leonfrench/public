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

import gate.Annotation;
import gate.AnnotationSet;
import gate.DataStore;
import gate.Document;
import gate.DocumentContent;
import gate.FeatureMap;
import gate.LanguageResource;
import gate.Node;
import gate.Resource;
import gate.creole.ResourceInstantiationException;
import gate.event.DocumentListener;
import gate.persist.PersistenceException;
import gate.security.SecurityException;
import gate.util.InvalidOffsetException;
import gate.util.OffsetComparator;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ubic.connection.Connection;
import ubic.connection.ConnectionEditor;
import ubic.pubmedgate.organism.LinnaeusSpeciesTagger;

/*
 * A GATE document wrapper with connections and methods for converting to other formats.  These wrapped documents do not exist in the corpus, so don't add them, use getDocument(). 
 */
public class ConnectionsDocument implements Document {
    public static final String GATETOKENS = "GATETokens";
    private static final long serialVersionUID = 42L;

    private Document doc;

    /*
     * get the underlying GATE document
     */
    public Document getDocument() {
        return doc;
    }

    /*
     * Given an annotation, return the string
     */
    public String getAnnotationText( Annotation ann ) {
        DocumentContent content = getContent();
        Node startNode = ann.getStartNode();
        Node endNode = ann.getEndNode();
        long start = startNode.getOffset();
        long end = endNode.getOffset();
        try {
            return content.getContent( start, end ).toString();
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * get PMID
     */
    public String getPMID() {
        FeatureMap fMap = doc.getFeatures();
        Object PMID = fMap.get( "PMID" );
        if ( PMID != null )
            return PMID.toString();
        else
            return null;
    }

    public ConnectionsDocument( Document doc ) {
        this.doc = doc;
        // TODO Auto-generated constructor stub
    }

    public List<String> getConnectionTags() {
        return getConnectionTags( null );
    }

    public List<String> getConnectionTags( String name ) {
        List<String> result = new LinkedList<String>();
        List<Connection> connections = getConnections( name );
        if ( connections != null ) {
            for ( Connection c : getConnections( name ) ) {
                String tag = c.getRelTag();
                if ( tag != null ) result.add( tag );
            }
        }
        return result;
    }

    /*
     * Given a document return it's connections
     */
    public List<Connection> getConnections() {
        return getConnections( null );
    }

    public void removeConnections() {
        // if it has connections, then set them to null
        if ( getConnections() != null ) {
            FeatureMap features = doc.getFeatures();
            features.put( ConnectionEditor.CONFEATURENAME, null );
        }
    }

    public boolean removeConnection( Connection c ) {
        List<Connection> connections = Connection.getConnections( doc.getFeatures() );
        boolean success = connections.remove( c );
        FeatureMap features = doc.getFeatures();
        features.put( ConnectionEditor.CONFEATURENAME, connections );
        return success;
    }

    public List<Connection> getConnections( String name ) {
        List<Connection> connections = Connection.getConnections( doc.getFeatures() );
        if ( connections == null ) return null;
        List<Connection> result = new LinkedList<Connection>();

        for ( Connection c : connections ) {
            if ( name == null || c.getAuthor().equalsIgnoreCase( name ) ) {
                result.add( c );
            }
        }
        return result;
    }

    public List<Annotation> getAnnotationsByType( String annotationSet, String type ) {
        List<Annotation> result = new LinkedList<Annotation>();

        AnnotationSet annotSet = getAnnotations( annotationSet );

        // annotSet.get( type ); -refactor?
        for ( Object o : annotSet ) {
            Annotation ann = ( Annotation ) o;
            if ( ann.getType().equals( type ) ) {
                result.add( ann );
            }
        }
        return result;
    }

    public AnnotationSet getBrainRegionAnnotations( String name ) {
        AnnotationSet annotSet = doc.getAnnotations( name );
        AnnotationSet brainRegions = annotSet.get( "BrainRegion" );
        return brainRegions;
    }

    public AnnotationSet getConnectionPredicateAnnotations( String name ) {
        AnnotationSet annotSet = doc.getAnnotations( name );
        AnnotationSet connects = annotSet.get( "ConnectionPredicate" );
        return connects;
    }

    public List<Annotation> getSortedAnnotations( String annotationSetName, String annotationTypeName ) {
        AnnotationSet set = getAnnotations( annotationSetName );
        AnnotationSet annots = set.get( annotationTypeName );

        List<Annotation> sentencesSorted = new ArrayList<Annotation>( annots );
        Collections.sort( sentencesSorted, new OffsetComparator() );

        return sentencesSorted;
    }

    public List<Annotation> getGATESentences( String annotationSet ) {
        return getSortedAnnotations( annotationSet, gate.creole.ANNIEConstants.SENTENCE_ANNOTATION_TYPE );
    }

    public Annotation getAbstract() {
        return getSortedAnnotations( "Original markups", "AbstractText" ).get( 0 );
    }

    public Annotation getTitle() {
        return getSortedAnnotations( "Original markups", "ArticleTitle" ).get( 0 );
    }

    public List<Annotation> getTokens( Annotation coveringAnnotation, String annotationSet ) {
        return getTokens( coveringAnnotation, annotationSet, gate.creole.ANNIEConstants.TOKEN_ANNOTATION_TYPE );
    }

    /**
     * Gets annotations of a specific type and set that are inside of a large annotation.
     * 
     * @param sentence
     * @param annotationSetName
     * @param annotationTokenType
     * @return
     */
    public List<Annotation> getTokens( Annotation sentence, String annotationSetName, String annotationTokenType ) {
        AnnotationSet all = getAnnotations( annotationSetName );

        // get the tokens in the range of the sentence annotation
        AnnotationSet tokens = all.get( annotationTokenType, sentence.getStartNode().getOffset(), sentence.getEndNode()
                .getOffset() );
        List<Annotation> tokensSorted = new ArrayList<Annotation>( tokens );
        Collections.sort( tokensSorted, new OffsetComparator() );

        return tokensSorted;
    }

    // ////////////////////////////////////////
    // /////// Document code below
    // ////////////////////////////////////////
    public void addDocumentListener( DocumentListener l ) {
        doc.addDocumentListener( l );
        // TODO Auto-generated method stub

    }

    public void edit( Long start, Long end, DocumentContent replacement ) throws InvalidOffsetException {
        // TODO Auto-generated method stub
        doc.edit( start, end, replacement );

    }

    public Boolean getCollectRepositioningInfo() {
        // TODO Auto-generated method stub
        return doc.getCollectRepositioningInfo();
    }

    public Boolean getMarkupAware() {
        // TODO Auto-generated method stub
        return doc.getMarkupAware();
    }

    public Map getNamedAnnotationSets() {
        // TODO Auto-generated method stub
        return doc.getNamedAnnotationSets();
    }

    public Boolean getPreserveOriginalContent() {
        // TODO Auto-generated method stub
        return doc.getPreserveOriginalContent();
    }

    public Long getSourceUrlEndOffset() {
        // TODO Auto-generated method stub
        return doc.getSourceUrlEndOffset();
    }

    public Long[] getSourceUrlOffsets() {
        // TODO Auto-generated method stub
        return doc.getSourceUrlOffsets();
    }

    public Long getSourceUrlStartOffset() {
        // TODO Auto-generated method stub
        return doc.getSourceUrlStartOffset();
    }

    public void removeDocumentListener( DocumentListener l ) {
        // TODO Auto-generated method stub
        doc.removeDocumentListener( l );
    }

    public void setCollectRepositioningInfo( Boolean b ) {
        // TODO Auto-generated method stub
        doc.setCollectRepositioningInfo( b );
    }

    public void setMarkupAware( Boolean b ) {
        doc.setMarkupAware( b );
    }

    public void setPreserveOriginalContent( Boolean b ) {
        // TODO Auto-generated method stub
        doc.setPreserveOriginalContent( b );
    }

    public void setSourceUrlEndOffset( Long sourceUrlEndOffset ) {
        // TODO Auto-generated method stub
        doc.setSourceUrlEndOffset( sourceUrlEndOffset );
    }

    public void setSourceUrlStartOffset( Long sourceUrlStartOffset ) {
        // TODO Auto-generated method stub
        doc.setSourceUrlStartOffset( sourceUrlStartOffset );
    }

    public String toXml() {
        // TODO Auto-generated method stub
        return doc.toXml();
    }

    public String toXml( Set aSourceAnnotationSet, boolean includeFeatures ) {
        // TODO Auto-generated method stub
        return doc.toXml( aSourceAnnotationSet, includeFeatures );
    }

    public String toXml( Set aSourceAnnotationSet ) {
        // TODO Auto-generated method stub
        return doc.toXml( aSourceAnnotationSet );
    }

    public Set getAnnotationSetNames() {
        // TODO Auto-generated method stub
        return doc.getAnnotationSetNames();
    }

    public AnnotationSet getAnnotations() {
        // TODO Auto-generated method stub
        return doc.getAnnotations();
    }

    public AnnotationSet getAnnotations( String name ) {
        // TODO Auto-generated method stub
        return doc.getAnnotations( name );
    }

    public DocumentContent getContent() {
        // TODO Auto-generated method stub
        return doc.getContent();
    }

    public URL getSourceUrl() {
        // TODO Auto-generated method stub
        return doc.getSourceUrl();
    }

    public void removeAnnotationSet( String name ) {
        // TODO Auto-generated method stub
        doc.removeAnnotationSet( name );
    }

    public void setContent( DocumentContent newContent ) {
        // TODO Auto-generated method stub
        doc.setContent( newContent );
    }

    public void setSourceUrl( URL sourceUrl ) {
        // TODO Auto-generated method stub
        doc.setSourceUrl( sourceUrl );
    }

    public DataStore getDataStore() {
        // TODO Auto-generated method stub
        return doc.getDataStore();
    }

    public Object getLRPersistenceId() {
        // TODO Auto-generated method stub
        return doc.getLRPersistenceId();
    }

    public LanguageResource getParent() throws PersistenceException, SecurityException {
        // TODO Auto-generated method stub
        return doc.getParent();
    }

    public boolean isModified() {
        // TODO Auto-generated method stub
        return doc.isModified();
    }

    public void setDataStore( DataStore dataStore ) throws PersistenceException {
        // TODO Auto-generated method stub
        doc.setDataStore( dataStore );

    }

    public void setLRPersistenceId( Object lrID ) {
        // TODO Auto-generated method stub
        doc.setLRPersistenceId( lrID );
    }

    public void setParent( LanguageResource parentLR ) throws PersistenceException, SecurityException {
        // TODO Auto-generated method stub
        doc.setParent( parentLR );
    }

    public void sync() throws PersistenceException, SecurityException {
        // TODO Auto-generated method stub
        doc.sync();
    }

    public void cleanup() {
        // TODO Auto-generated method stub
        doc.cleanup();
    }

    public Object getParameterValue( String paramaterName ) throws ResourceInstantiationException {
        // TODO Auto-generated method stub
        return doc.getParameterValue( paramaterName );
    }

    public Resource init() throws ResourceInstantiationException {
        // TODO Auto-generated method stub
        return doc.init();
    }

    public void setParameterValue( String paramaterName, Object parameterValue ) throws ResourceInstantiationException {
        // TODO Auto-generated method stub
        doc.setParameterValue( paramaterName, parameterValue );
    }

    public void setParameterValues( FeatureMap parameters ) throws ResourceInstantiationException {
        // TODO Auto-generated method stub
        doc.setParameterValues( parameters );
    }

    public FeatureMap getFeatures() {
        // TODO Auto-generated method stub
        return doc.getFeatures();
    }

    public void setFeatures( FeatureMap features ) {
        // TODO Auto-generated method stub
        doc.setFeatures( features );
    }

    public String getName() {
        // TODO Auto-generated method stub
        return doc.getName();
    }

    public void setName( String name ) {
        // TODO Auto-generated method stub
        doc.setName( name );
    }

    public int compareTo( Object o ) {
        // TODO Auto-generated method stub
        return doc.compareTo( o );
    }

    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !( obj instanceof ConnectionsDocument ) ) return false;
        ConnectionsDocument that = ( ConnectionsDocument ) obj;
        return that.getPMID().equals( this.getPMID() );
        // return that.doc.equals( this.doc );
    }

    public Set<String> getLinnaeusSpecies() {
        Set<String> species = new HashSet<String>();
        AnnotationSet aSet = getAnnotations( LinnaeusSpeciesTagger.ANNOTATIONSET );
        aSet = aSet.get( LinnaeusSpeciesTagger.SPECIESTAG );
        for ( Annotation annot : aSet ) {
            String speciesID = annot.getFeatures().get( LinnaeusSpeciesTagger.NCBIFEATURE ).toString();
            // if it's not filtered??
            // if (getAnnotationText( annot ).contains( "SHR" )) {
            // System.out.println(getAnnotationText( annot ));
            // System.out.println(getPMID());
            // }
            species.add( speciesID );
        }
        return species;
    }

    public Set<String> getBagOfWords() {
        Set<String> result = new HashSet<String>();
        Set<Annotation> resultAnn = new HashSet<Annotation>();
        Annotation titleAnn = getTitle();
        Annotation abstractAnn = getAbstract();
        resultAnn.addAll( getTokens( titleAnn, GATETOKENS ) );
        resultAnn.addAll( getTokens( abstractAnn, GATETOKENS ) );
        // nothing fancy, doesnt do frequency - would be easy though
        for ( Annotation ann : resultAnn ) {
            result.add( getAnnotationText( ann ).toLowerCase() );
        }
        return result;
    }

    public Date getPubDate() {
        return ( Date ) getFeatures().get( "PublicationDate" );
    }

}
