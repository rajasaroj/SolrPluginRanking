package solrplugin.solrplugin;

public class ScoreAd {
	 int luceneDocId;
	 String docId;
	 float score;
	 
	 
	/*
	 * public static void main(String[] args) { String str = "hi raja how are you";
	 * String str2 = "\""+"ABC"+"\""; String sttr3 =
	 * str2.toLowerCase().replaceAll("^\"|\"$", ""); System.out.println(sttr3); }
	 */
	 
	public int getLuceneDocId() {
		return luceneDocId;
	}


	public void setLuceneDocId(int luceneDocId) {
		this.luceneDocId = luceneDocId;
	}


	public String getDocId() {
		return docId;
	}


	public void setDocId(String docId) {
		this.docId = docId;
	}


	public float getScore() {
		return score;
	}


	public void setScore(float score) {
		this.score = score;
	}


	public ScoreAd(int luceneDocId, String docId, float score) {
		super();
		this.luceneDocId = luceneDocId;
		this.docId = docId;
		this.score = score;
	}
	 
	 
}
