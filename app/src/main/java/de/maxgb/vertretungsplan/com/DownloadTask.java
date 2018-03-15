package de.maxgb.vertretungsplan.com;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import com.squareup.okhttp.*;
import de.maxgb.android.util.Logger;
import de.maxgb.vertretungsplan.util.Constants;
import okio.Buffer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Async Download Task Lädt den Vertretungsplan herunter
 * 
 * @author Max Becker
 * 
 */
public class DownloadTask extends AsyncTask<Void, Void, Integer> {
	private final String TAG = "DownloadTask";
	private final int SUCCESS = 0;
	private final int MISSINGLOGININFO = 1;
	private final int NOCONNECTION = 2;
	private final int OTHEREXCEPTION = -1;
	private SharedPreferences pref;
	private DownloadFinishedListener listener;
	private Context context;
	private String error="";

	public DownloadTask(SharedPreferences pref, DownloadFinishedListener listener, Context context) {
		this.pref = pref;
		this.listener = listener;
		this.context = context;
	}

	/**
	 * Returns a SSL context that trusts {@code certificates} and none other. HTTPS services whose
	 * certificates have not been signed by these certificates will fail with a {@code
	 * SSLHandshakeException}.
	 * <p>
	 * <p>This can be used to replace the host platform's built-in trusted certificates with a custom
	 * set. This is useful in development where certificate authority-trusted certificates aren't
	 * available. Or in production, to avoid reliance on third-party certificate authorities.
	 * <p>
	 * <p>See also {@link CertificatePinner}, which can limit trusted certificates while still using
	 * the host platform's built-in trust store.
	 * <p>
	 * <h3>Warning: Customizing Trusted Certificates is Dangerous!</h3>
	 * Relying on your own trusted certificates limits your server team's ability to update their TLS
	 * certificates. By installing a specific set of trusted certificates, you take on additional
	 * operational complexity and limit your ability to migrate between certificate authorities. Do
	 * not use custom trusted certificates in production without the blessing of your server's TLS
	 * administrator.
	 * <p>
	 * Source: https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/com/squareup/okhttp/recipes/CustomTrust.java
	 */
	public SSLContext sslContextForTrustedCertificates(InputStream in) {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
			if (certificates.isEmpty()) {
				throw new IllegalArgumentException("expected non-empty set of trusted certificates");
			}

			// Put the certificates a key store.
			char[] password = "password".toCharArray(); // Any password will work.
			KeyStore keyStore = newEmptyKeyStore(password);
			int index = 0;
			for (Certificate certificate : certificates) {
				String certificateAlias = Integer.toString(index++);
				keyStore.setCertificateEntry(certificateAlias, certificate);
			}

			// Wrap it up in an SSL context.
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
					KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, password);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
					TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),
					new SecureRandom());
			return sslContext;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}


	}

	/*
	 * Downloads the Plan in background
	 * @return 0=Success,1=Missing login info,2=No Internetconnection,-1=Other Exception
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Integer doInBackground(Void... voids) {
		// Laden der Einstellungen
		String username = pref.getString(Constants.USERNAME_KEY, "");
		String password = pref.getString(Constants.PASSWORD_KEY, "");
		boolean schuelerplan = pref.getBoolean(Constants.SCHUELER_KEY, false);
		boolean lehrerplan = pref.getBoolean(Constants.LEHRER_KEY, false);

		// Überprüfen der Einstellungen
		if (username.equals("") || password.equals("")) {
			Logger.w(TAG, "Kein Nutzername oder Passwort eingestellt");
			return MISSINGLOGININFO;
		}
		// Überprüfen der Internetverbindung
		if (!isOnline()) {
			Logger.w(TAG, "No InternetConnection");
			return NOCONNECTION;
		}
		// Abrufen des Plans



		Logger.i(TAG, "Anfrage gestartet");

		OkHttpClient httpsclient=new OkHttpClient();
		httpsclient.setConnectTimeout(Constants.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
		httpsclient.setReadTimeout(Constants.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		httpsclient.setCookieHandler(cookieManager);
		//SSLContext sslContext=sslContextForTrustedCertificates(trustedCertificatesInputStream());
		//httpsclient.setSslSocketFactory(sslContext.getSocketFactory());

		try {
			String cookie = getLoginCookie(httpsclient);
			login(httpsclient, cookie, username, password);
			planSpeichern(httpsclient, schuelerplan, lehrerplan);
			Logger.i(TAG, "Anfrage abgeschlossen");
			return SUCCESS;
		} catch (Exception e) {
			Logger.e(TAG, "Abrufen des Plans fehlgeschlagen", e);
			error=e.getMessage();
			return OTHEREXCEPTION;
		}
	}

	protected String getLoginCookie(OkHttpClient client) throws IOException {

		Logger.i(TAG, "Abrufen der Loginseite gestartet");
		Request request=new Request.Builder().url(Constants.LOGIN_SEITE_URL).build();
		Response response=client.newCall(request).execute();// Abruf der Seite
		if (response.isSuccessful()) { // Erfolg
																			// der
																			// Anfrage
			Logger.i(TAG, "Erfolgreicher Loginseiten Abruf");

			String responseString = response.body().string();

			if (Logger.getDebugMode()) {
				try {
					save(responseString, "debug_login.html");
				} catch (IOException e) {
					Logger.e(TAG, "Debug Login Speicherung fehlgeschlagen", e);
				}
			}// Speichern der Seite zum Debuggen



			// Cookie mittels REGEX auslesen
			Pattern cookiePattern = Pattern.compile(Constants.COOKIE_SUCH_REGEX);
			Matcher cookieMatcher = cookiePattern.matcher(responseString);
			if(cookieMatcher.find()&&cookieMatcher.groupCount()>=1){
				String cookie=cookieMatcher.group(1);
				Logger.i(TAG, "Cookie ausgelesen. Wert: " + cookie);

				return cookie;
			}
			else{
				Logger.i(TAG,cookieMatcher.toString()+" - "+cookieMatcher.group());

				Logger.w(TAG,"Loginseite enthält keinen Cookie");
				throw new IOException("Login Seite enthält keinen Cookie");
			}

		} else {

			throw new IOException("Abfrage Fehlgeschlagen: " + response.message());// Fehlermeldung
			// werfen
			// wenn
			// Abfrage
			// fehlgeschlagen
		}
	}

	protected void login(OkHttpClient client, String cookie, String username, String password) throws IOException {
		Logger.i(TAG, "Loginvorgang gestartet");
		// Erstellen des Postrequest
		RequestBody requestBody=new FormEncodingBuilder().add("username",username).add("password",password)
				.add("return","L2luZGV4LnBocC9pbnRlcm4v")//Unbekannte Funktion
				.add(cookie,"1").build();
		Request request=new Request.Builder().url(Constants.LOGIN_URL).post(requestBody).build();
		Logger.i(TAG,"Body: "+requestBody.contentType()+" "+requestBody.contentLength());
		Logger.i(TAG,"Request: "+request.toString());
		// -----------------------------------------------
		Response response=client.newCall(request).execute();
		// Siehe abrufen()
		if (response.isSuccessful()) {
			Logger.i(TAG, "Loginvorgang erfolgreich abgeschloßen. Status aber unbekannt");
			if (Logger.getDebugMode()) {
				try {
					String responseString = response.body().string();
					save(responseString, "debug_login.html");
				} catch (IOException e) {
					Logger.e(TAG, "Debug Login Speicherung fehlgeschlagen", e);
				}
			}// Speichern der Seite zum Debuggen
			return;
		} else {
			throw new IOException("Login fehlsgeschlagen " + response.message());
		}

	}

	@Override
	protected void onPostExecute(Integer result) {

		switch (result) {
		case SUCCESS:
			SharedPreferences.Editor editor = pref.edit();
			editor.putLong(Constants.REFRESH_TIME_KEY, System.currentTimeMillis());
			editor.commit();
			listener.onDownloadSuccesfullyFinished();
			break;
		case MISSINGLOGININFO:
			listener.onDownloadFailed("Kein Nutzername oder Passwort eingestellt");
			break;
		case NOCONNECTION:
			listener.onDownloadFailed("Keine Internetverbindung");
			break;
		case OTHEREXCEPTION:
			listener.onDownloadFailed("Fehler beim Aktualisieren ("+error+")");
			break;
		}

	}

	protected void planSpeichern(OkHttpClient client, boolean schuelerplan, boolean lehrerplan)
			throws  IOException {
		Logger.i(TAG, "Abrufen...");
		if (schuelerplan) {
			Logger.i(TAG, "Abrufen des Schueler Plans gestartet");
			Request request=new Request.Builder().url(Constants.SCHUELER_PLAN_URL).build();
			Response response=client.newCall(request).execute();// Planseite
			// abrufen
			if (response.isSuccessful()) {
				Logger.i(TAG, "Abrufen des Plans erfolgreich abgeschloßen");
				String responseString = response.body().string();

				save(responseString.replace('&','+'), Constants.SCHUELER_PLAN_FILE_NAME);

			} else {
				throw new IOException(response.message());
			}
		}
		if (lehrerplan) {
			Logger.i(TAG, "Abrufen des Lehrer Plans gestartet");
			Request request=new Request.Builder().url(Constants.LEHRER_PLAN_URL).build();
			Response response=client.newCall(request).execute();// Planseite
			// abrufen
			if (response.isSuccessful()) {
				Logger.i(TAG, "Abrufen des Plans erfolgreich abgeschloï¿½en");
				String responseString = response.body().string();

				save(responseString.replace('&','+'), Constants.LEHRER_PLAN_FILE_NAME);

			} else {
				throw new IOException(response.message());
			}
		}
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			InputStream in = null; // By convention, 'null' creates an empty key store.
			keyStore.load(in, password);
			return keyStore;
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private void save(String s, String file) throws IOException {
		Logger.i(TAG, "Speichern der Datei: " + file + " gestartet");
		FileOutputStream fos = this.context.openFileOutput(file, Context.MODE_PRIVATE);
		fos.write(s.getBytes());
		fos.close();
		Logger.i(TAG, "Speichern der Datei: " + file + " abgeschloï¿½en");

	}

	private InputStream trustedCertificatesInputStream(){
		String rats_certificate=Constants.getCertificate();
		return new Buffer()
				.writeUtf8(rats_certificate)
				.writeUtf8(Constants.getDebugFiddlerCertificate())
				.inputStream();
	}

	public interface DownloadFinishedListener {
		void onDownloadFailed(String errortext);

		void onDownloadSuccesfullyFinished();
	}

}