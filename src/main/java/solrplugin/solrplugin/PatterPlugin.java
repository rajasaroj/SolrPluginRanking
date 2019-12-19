package solrplugin.solrplugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.RankQuery;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SortSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;

public class PatterPlugin extends SearchComponent{

	private static final Logger LOG = LoggerFactory.getLogger(PatterPlugin.class);
	volatile long numRequests;
	volatile long numErrors;
	volatile long totalRequestsTime;
	volatile String lastnewSearcher;
	volatile String lastOptimizerEvent;
	protected String defaultField;
	private List<String> words;
	
	  @Override
	  public void init( NamedList args )
	  {
	    super.init(args);
	    defaultField = (String) args.get("field");
	    
	    if(defaultField == null) {
	    	throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Need to specify the default for analysis" );
	    }
	    
	    words = ((NamedList)args.get("words")).getAll("word");
	    
	    if(words.isEmpty()) {
	    	throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Need to specify atleast one word in searchComponent config");
	    }
	    
	  }
	
	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		// TODO Auto-generated method stub
		LOG.info("prepare method of PatterPlugin");
		
		
		final SortSpec sortSpec = rb.getSortSpec();
		final int offset = sortSpec.getOffset();
	    final SolrParams params = rb.req.getParams();
		if(params.get("score") == null)
		rb.setFieldFlags(SolrIndexSearcher.GET_SCORES);
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		// TODO Auto-generated method stub
		LOG.info("pocess method of PatterPlugin");
		numRequests++;
		
		SolrParams params = rb.req.getParams();
		String input  = params.get("base_key");
		String q = params.get("q");
		LOG.info("Input q------------------------------------------ "+q);
		String key = q.split(":")[1];
		LOG.info("Input key------------------------------------------ "+key);
		LOG.info("Input Basekey------------------------------------------ "+input);
		long lstartTime = System.currentTimeMillis();
		SolrIndexSearcher searcher = rb.req.getSearcher();
		
		NamedList response = new SimpleOrderedMap();
		
		String queryField = params.get("field");
		String field= null;
		
		if(defaultField!=null) {
			field=defaultField;
			LOG.info("default field: "+ field);
		}
		
		if(queryField != null) {
			field = queryField;
			LOG.info("queryField field: "+ field);
		}
		
		if(field == null) {
			LOG.error("fields aren't defiend , not performing counting");
			return;
		}
		
		DocList docsList1 = rb.getResults().docList;
		if(docsList1 == null || docsList1.size() == 0) {
		LOG.debug("NO results");
		}
		
		LOG.debug("Doing This many docs:\t" + docsList1.size());
		
		Set<String> fieldSet = new HashSet<String>();
		
		SchemaField keyField = rb.req.getCore().getLatestSchema().getUniqueKeyField();
		
		if(null != keyField) {
			fieldSet.add(keyField.getName());
		}
		fieldSet.add(field);
		LOG.info("fieldSet field (line no 104): "+ fieldSet);
		
		
		
		DocListAndSet results = documentSorter(docsList1,key.toLowerCase().replaceAll("^\"|\"$", ""), input.toLowerCase(),  searcher, field);
		LOG.info("DocListAndSet results = documentSorter: "+ results.toString());
		rb.setResults(results);
		BasicResultContext resultContext = new BasicResultContext(rb);
		SolrQueryResponse rsp = rb.rsp;
		rsp.getValues().removeAll("response");
		rsp.addResponse(resultContext);	
	}

	public DocListAndSet documentSorter(DocList docsList, String key, String word, SolrIndexSearcher searcher, String field) throws IOException {
		
		LOG.info("documentSorter(method 140) key: "+ key + "word:  "+ "field: "+ field);
		
		SortedMap<Float, Integer > scoreMap = new TreeMap<Float, Integer>();
		List<ScoreAd> results = new ArrayList();
		DocIterator iterator = docsList.iterator();
		float maxScore = docsList.maxScore() * 100;
		for (int i=0; i< docsList.size(); i++) {
			int docId = iterator.nextDoc();
			
			
			Document doc = searcher.doc(docId);
			LOG.info("Document object: " + doc.toString());
			IndexableField[] muiltfield = doc.getFields(field);
			
			for(IndexableField single: muiltfield) {
				String sentenceValue = single.stringValue().toLowerCase();
				String TransSentenceValue = (sentenceValue.contains(",")) ? sentenceValue.replaceAll(",", "") : sentenceValue;
				
				LOG.info("---------########------------TransSentenceValue "+ TransSentenceValue + " key: "+ key);
				
				String solrDocId = null;
				if(TransSentenceValue.contains(word)) {
					float ScoreCal =  maxScore / Math.abs(TransSentenceValue.indexOf(key) - TransSentenceValue.indexOf(word)) ;
					solrDocId = doc.getField("id").stringValue();
					results.add(new ScoreAd(docId, solrDocId , ScoreCal) );
					//LOG.info("For Containig word luceneid:" + docId+ " solrdocid:"+ solrDocId + " ScoreCal: " +  ScoreCal );
					LOG.info("---------########------------For Containig word luceneid:" + docId+ " keyIndex:"+ TransSentenceValue.indexOf(key) + " wordIndex: " +  TransSentenceValue.indexOf(word) );
				}
				else {
					solrDocId = doc.getField("id").stringValue();
					results.add(new ScoreAd(docId,solrDocId , iterator.score()) );
					LOG.info("For non Containig word luceneid:" + docId+ " solrdocid:"+ solrDocId + " ScoreCal: " +  iterator.score() );
				}
			}				
		}

		results.sort(new Comparator<ScoreAd>() {
			public int compare(ScoreAd r1, ScoreAd r2) {
				return Float.compare(r2.getScore(), r1.getScore());
			}
		});
		
		LOG.info("documentSorter List<ScoreAd> results: "+ results.toString());
		
		int offset = 0;
		int len = docsList.size();
		
		List<Integer> DocIdArray = new ArrayList();
		List<Float> DocScore = new ArrayList();
		for(ScoreAd x : results) {
			DocIdArray.add(x.getLuceneDocId());
			DocScore.add(x.getScore());
		}
		
		
		LOG.info("documentSorter List<Integer> DocIdArray: "+ DocIdArray.toString());
		LOG.info("documentSorter List<Float> DocScore: "+ DocScore.toString());
		
		int[] IntDocArray = Ints.toArray(DocIdArray);
		float[] FloatDocScore = Floats.toArray(DocScore);
		
		long matches = docsList.matches();
		float  maxxscore = docsList.maxScore();
		
		
		DocListAndSet docListAndSet = new DocListAndSet();
		
		LOG.info("offset: "+offset+"len: "+len+"IntDocArray: "+ IntDocArray.toString()+ "FloatDocScore: "+FloatDocScore.toString() + "matches: "+matches+ "maxxscore: "+ maxxscore );
		docListAndSet.docList =  new DocSlice(offset, len, IntDocArray, FloatDocScore, matches, maxxscore);
		
		return docListAndSet;
		
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "PatterPlugin";
	}
	
	
}
