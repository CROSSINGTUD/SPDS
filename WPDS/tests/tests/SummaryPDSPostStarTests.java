package tests;

import org.junit.Before;

import wpds.impl.PostStar;

public class SummaryPDSPostStarTests extends PDSPoststarTests {
	@Before
	public void init() {
		PostStar.SUMMARIES = true;
		super.init();
	}
}
