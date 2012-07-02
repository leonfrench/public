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

package ubic.pubmedgate.metaMap;


/**
 * This class requires the GEOMMTx project code to run. 
 * 
 * @author leon
 *
 */

@Deprecated
public class MakeMMTxAnnotations {
    // MMTxRunner mmtx;
     public static final String ANNOTATIONSET = "MMTx";
    public static final String ANNOTATIONNAME = "MMTx";
    // GetUMLSCodes codes;
    // int scoreThreshold = 499;
    //
    // public MakeMMTxAnnotations() {
    // mmtx = new MMTxRunner( null, scoreThreshold, new String[] { "--an_derivational_variants", "--no_acros_abbrs" } );
    // codes = new GetUMLSCodes( Config.config.getString( "whitetext.annotator.cui_code_loc" ) );
    // }
    //
    // // annotate sentences?
    //
    // public void annotateDocument( ConnectionsDocument doc ) throws Exception {
    // AnnotationSet anotSet = doc.getAnnotations( ANNOTATIONSET );
    //
    // for ( Phrase p : mmtx.getPhrases( doc.getContent().toString() ) ) {
    // long start = p.getSpan().getBeginCharacter();
    // long end = p.getSpan().getEndCharacter() + 1;
    //
    // Collection<Candidate> candidates = mmtx.getConcepts( p );
    // if ( candidates.isEmpty() ) continue;
    //
    // FeatureMap fMap = new SimpleFeatureMapImpl();
    // FeatureMap fMapHumanReadable = new SimpleFeatureMapImpl();
    // boolean neuronames = false;
    // // new
    // fMap.put( "MMTxPhraseType=" + p.getTypeOfPhraseString(), 1 );
    // // several candidates will be found in each phrase
    // // we don't use key/value pairs because of the one to many setup
    // for ( Candidate c : candidates ) {
    // fMapHumanReadable.put( "MMTxConcept=" + c.getConcept(), 1 );
    // fMap.put( "MMTxCUI=" + c.getCUI(), 1 );
    // fMap.put( "MMTxScore=" + c.getFinalScore(), 1 );
    // UMLS_SemanticTypePointer[] types = c.getSemanticTypes();
    // for ( UMLS_SemanticTypePointer type : types ) {
    // fMap.put( "MMTxSemType=" + type.getTUI(), 1 );
    // fMapHumanReadable.put( "MMTxSemName=" + type.getName(), 1 );
    //
    // }
    // Set<UMLSSourceCode> codeSet = codes.getUMLSCodeMap().get( c.getCUI() );
    // if ( codeSet != null ) {
    // for ( UMLSSourceCode code : codeSet ) {
    // fMap.put( "MMTxSource=" + code.getSource(), 1 );
    // if ( code.getSource().startsWith( "NEU" ) ) {
    // neuronames = true;
    // }
    // }
    // }
    // }
    // anotSet.add( start, end, ANNOTATIONNAME, fMap );
    // anotSet.add( start, end, ANNOTATIONNAME + "Readable", fMapHumanReadable );
    // // add it as a brain region
    // if ( neuronames ) anotSet.add( start, end, "BrainRegion", new SimpleFeatureMapImpl() );
    // }
    // System.out.println( doc.getContent().toString() );
    // }
    //
    // public void annotateDocumentTokens( ConnectionsDocument doc, String annotationSet ) throws Exception {
    // for ( Annotation sentence : doc.getGATESentences( annotationSet ) ) {
    // for ( Annotation token : doc.getTokens( sentence, annotationSet ) ) {
    // // what if we do this more than once?
    // FeatureMap fMap = token.getFeatures();
    // String text = doc.getAnnotationText( token );
    // System.out.println( text );
    // for ( Candidate c : mmtx.getConcepts( text ) ) {
    // fMap.put( "MMTxCUI=" + c.getCUI(), 1 );
    // fMap.put( "MMTxScore=" + c.getFinalScore(), 1 );
    // UMLS_SemanticTypePointer[] types = c.getSemanticTypes();
    // for ( UMLS_SemanticTypePointer type : types ) {
    // fMap.put( "MMTxSemType=" + type.getTUI(), 1 );
    // }
    // Set<UMLSSourceCode> codeSet = codes.getUMLSCodeMap().get( c.getCUI() );
    // if ( codeSet != null ) {
    // for ( UMLSSourceCode code : codeSet ) {
    // fMap.put( "MMTxSource=" + code.getSource(), 1 );
    // }
    // }
    // }
    // }
    // }
    // }
    //
    // /**
    // * @param args
    // */
    // public static void main( String[] args ) throws Exception {
    // StopWatch watch = new StopWatch();
    // watch.start();
    // GateInterface p2g = new GateInterface();
    // // Corpus corp = p2g.getTrainingCorp();
    // // Corpus corp = p2g.getUnseenCorp();
    // Corpus corp = p2g.getCorp();
    //
    // // MMTxReset reseter = new MMTxReset( p2g.getTrainingCorp() );
    // MMTxReset reseter = new MMTxReset( corp );
    // reseter.reset();
    //
    // MakeMMTxAnnotations maker = new MakeMMTxAnnotations();
    // System.out.println( "Time:" + watch.toString() );
    // // ConnectionsDocument doc = p2g.getByPMID( "9831048" );
    // System.out.println( "Time:" + watch.toString() );
    // int i = 0;
    // for ( ConnectionsDocument doc : p2g.getDocuments( corp ) ) {
    // maker.annotateDocument( doc );
    // maker.annotateDocumentTokens( doc, "TreeTaggerGATETokens" );
    // doc.sync();
    // System.out.println( "i:" + i++ + " Time:" + watch.toString() );
    // }
    // }
}
