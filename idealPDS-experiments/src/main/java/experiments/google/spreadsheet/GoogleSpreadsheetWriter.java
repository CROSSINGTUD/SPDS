package experiments.google.spreadsheet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Lists;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GoogleSpreadsheetWriter {
	private static final String APPLICATION_NAME = "SPDS-Performancewriter";
	private static final String SPREADSHEET_ID = "1B_VNQW2JAvK0exMFWOVfGnbhCuvA4Qpe0rGO0SqphgQ";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
	private static boolean onlyOnce;

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT
	 *            The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException
	 *             If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = GoogleSpreadsheetWriter.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	/**
	 * Prints the names and majors of students in a sample spreadsheet:
	 * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	 * @throws GeneralSecurityException 
	 * @throws IOException 
	 */
	public static void write(List<Object> data) throws IOException, GeneralSecurityException  {
		Sheets service = getService();
		String sheetID = getGitRepositoryState().commitId;
		ArrayList<List<Object>> rows = Lists.newArrayList();
		rows.add(data);
		ValueRange body = new ValueRange().setValues(rows);
		service.spreadsheets().values().append(SPREADSHEET_ID, sheetID, body).setValueInputOption("USER_ENTERED")
				.execute();
	}

	public static void createSheet(List<Object> headers) throws IOException, GeneralSecurityException{
		if(onlyOnce)
			return;
		onlyOnce = true;	
		Sheets service = getService();
		String sheetID = getGitRepositoryState().commitId;
		List<Request> requests = new ArrayList<>(); 
		AddSheetRequest addSheet = new AddSheetRequest();
		addSheet.setProperties(new SheetProperties().setTitle(sheetID));
		requests.add(new Request().setAddSheet(addSheet));
		BatchUpdateSpreadsheetRequest requestBody = new BatchUpdateSpreadsheetRequest();
		requestBody.setRequests(requests);
		service.spreadsheets().batchUpdate(SPREADSHEET_ID, requestBody).execute();
		
		ArrayList<List<Object>> rows = Lists.newArrayList();
		rows.add(headers);
		ValueRange body = new ValueRange().setValues(Arrays.asList(headers));
		service.spreadsheets().values().append(SPREADSHEET_ID, sheetID, body).setValueInputOption("USER_ENTERED")
				.execute();
	}
	
	private static Sheets getService() throws IOException, GeneralSecurityException {
		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,  getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
	}

	public static GitRepositoryState getGitRepositoryState() throws IOException {
		Properties properties = new Properties();
		String userDir = System.getProperty("user.dir");
		properties.load(new FileReader(new File(userDir+File.separator +"git.properties")));
		return new GitRepositoryState(properties);
	}

	public static void computeMetrics() throws IOException, GeneralSecurityException {
		Sheets service = getService();
		String sheetID = getGitRepositoryState().commitId;
		ArrayList<List<Object>> rows = Lists.newArrayList();
		ArrayList<Object> content = Lists.newArrayList();
		content.add(getGitRepositoryState().buildHost);
		content.add(getGitRepositoryState().buildTime);
		content.add(getGitRepositoryState().branch);
		content.add(sheetID);
		content.add("=GEOMITTEL('"+sheetID+"'!J2:J1004)");
		rows.add(content);
		ValueRange body = new ValueRange().setValues(rows);
		service.spreadsheets().values().append(SPREADSHEET_ID, "history", body).setValueInputOption("USER_ENTERED")
				.execute();
	}
}