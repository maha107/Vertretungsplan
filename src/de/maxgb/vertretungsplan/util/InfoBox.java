package de.maxgb.vertretungsplan.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.webkit.WebView;
/**
 * Infobox f�r Infotexte und Anleitungen
 * @author Max Becker
 *
 */
public class InfoBox {
	public enum Anleitungen {
		KURSWAHL(
				"Kurswahl",
				14,
				"<html><body>F�ge hier deine Kurse in Form ihrer Abk�rzung, wie sie auch auf dem Vertretungsplan erscheinen hinzu (z.B. 'PH1').<br>Durch langes Dr�cken auf einen Kurs entfernst du ihn</body></html>"), OPTIONSLEHRER(
				"Options_Lehrer",
				14,
				"<html><body>Geben Sie hier Nutzernamen und Passwort aus der Schule und ihr K�rzel wie es auf dem Vertretungsplan erscheint ein.<p>Unter 'Anzeige Tabs festlegen' k�nnen Sie entscheiden welche Tabs angezeigt werden sollen. Unterschiedliche Tabs filtern die Vertretungen jeweils anders.</body></html>"), OPTIONSSCHUELEROHNEKURSE(
				"Options_Schueler",
				14,
				"<html><body>Gebe hier Nutzername und Passwort aus der Schule und deine Stufe/Klasse (z.B. OI oder VIa) ein.<p>Unter 'Anzeige Tabs festlegen' kannst du entscheiden welche Tabs angezeigt werden sollen. Unterschiedliche Tabs filtern die Vertretungen  anders.</body></html>"), OPTIONSSCHUELERMITKURSE(
				"Options_Schueler",
				14,
				"<html><body>Gebe hier Nutzername und Passwort aus der Schule und deine Stufe/Klasse (z.B. OI oder VIa) ein.<p>Unter 'Kurswahl' kannst du deine eigenen Kurse einstellen, damit nur Vertretungen f�r deine eigenen Kurse angezeigt werden.<br>Unter 'Anzeige Tabs festlegen' kannst du entscheiden welche Tabs angezeigt werden sollen. Unterschiedliche Tabs filtern die Vertretungen anders.</body></html>"), ANZEIGEINFO(
				"Anzeige",
				14,
				"<html><body>Je nach Display Gr��e werden Bemerkungen und/oder Klasurmarkierungen in einem gesonderten Fenster angezeigt. Wenn dies der Fall ist, wird hinter der Vertretung ein X angezeigt. Zum Anzeigen der Informationen, einfach auf die Vertretung klicken.<p>Sollten die angezeigten Vertretungen auf ihrem Ger�t zu klein oder nicht mehr lesbar sein, dann sagen sie mir bitte Bescheid und teilen mir die Bildschirmgr��e und Aufl�sung mit (app@maxgb.de).<br>Danke</body></html>");

		public String activity_name;
		public int letzte_aenderung;
		public String text;

		Anleitungen(String name, int letzte_aenderung, String text) {
			this.activity_name = name;
			this.letzte_aenderung = letzte_aenderung;
			this.text = text;
		}

	}
	private static final String PREFS_NAME = "InfoBox";
	private static final String TITLE = "Erkl�rung";

	public static final int ANLEITUNG_KURSWAHL_AENDERUNG = 14;

	public static void showAnleitungBox(Context context, Anleitungen anleitung) {

		SharedPreferences settings = context
				.getSharedPreferences(PREFS_NAME, 0);
		if (settings.getInt(anleitung.activity_name, 0) < anleitung.letzte_aenderung) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(TITLE);
			builder.setPositiveButton("Ok", new OnClickListener() {
				@Override
				public void onClick(DialogInterface i, int a) {
				}

			});
			WebView v = new WebView(context);
			v.loadData(anleitung.text, "text/html; charset=UTF-8", null);
			builder.setView(v);

			AlertDialog dialog = builder.create();
			dialog.show();

			SharedPreferences.Editor editor = settings.edit();
			editor.putInt(anleitung.activity_name, anleitung.letzte_aenderung);
			editor.commit();
		}
	}

	public static void showInfoBox(Context context, String title, String text) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setPositiveButton("Ok", new OnClickListener() {
			@Override
			public void onClick(DialogInterface i, int a) {
			}

		});
		WebView v = new WebView(context);
		v.loadData(text, "text/html; charset=UTF-8", null);
		builder.setView(v);

		AlertDialog dialog = builder.create();
		dialog.show();
	}

}