package de.maxgb.vertretungsplan.util;

import java.util.HashMap;

import de.maxgb.vertretungsplan.manager.StundenplanManager;

import android.annotation.SuppressLint;

public class Stunde {
	private String kurs;
	private String raum;
	private int stunde;
	private String uhrzeit;//wenn stunde >=8
	private final String TAG = "Stunde";
	
	//F�r die Anzeige von Vertretenden/Modifizierten Stunden
	private boolean modified=false;
	private String oldKurs;
	private String oldRaum;
	private String bemerkung;
	private String klausur;
	private String art;
	private String tag;
	
	public Stunde(String kurs,String raum,int stunde){
		this.kurs=kurs.trim();
		this.raum=raum.trim();
		this.stunde=stunde;
		
	}
	
	public Stunde(String kurs,String raum,int stunde,String uhrzeit){
		this.kurs=kurs.trim();
		this.raum=raum.trim();
		this.stunde=stunde;
		if(stunde>=8){
			this.uhrzeit=uhrzeit;
		}
		else{
			Logger.w(TAG,"Uhrzeit �bergeben, obwohl Stunde 1-7");
		}
		
	}
	
	public void vertreteten(String newKurs,String newRaum,String newBemerkung,String newKlausur,String newArt,String newTag){
		modified=true;
		oldKurs=kurs;
		kurs=newKurs;
		oldRaum=raum;
		raum=newRaum;
		bemerkung=newBemerkung;
		klausur=newKlausur;
		art=newArt;
		tag=newTag;
	}
	

	public String getKurs() {
		return kurs;
	}

	public String getRaum() {
		return raum;
	}

	public int getStunde() {
		return stunde;
	}

	public String getUhrzeit() {
		if(uhrzeit==null){
			return "";
		}
		return uhrzeit;
	}
	
	public String getName(){
		return getName(getKurs());
	}
	
	public String toString(){
		return kurs+","+raum+","+stunde+","+getUhrzeit();
	}

	public boolean isModified() {
		return modified;
	}

	public String getOldKurs() {
		return oldKurs;
	}

	public String getOldRaum() {
		return oldRaum;
	}

	public String getBemerkung() {
		return bemerkung;
	}
	
	@SuppressLint("DefaultLocale")
	private static String getName(String kurs){
		kurs=removeNumbers(kurs).toLowerCase();
		HashMap<String,String> map=StundenplanManager.getInstance().getKursnamen();
		String name=map.get(kurs);
		if(name==null){
			name=kurs;
		}
		return name;
	}
	
	private static String removeNumbers(String s){
		s=s.replace("0","");
		s=s.replace("1","");
		s=s.replace("2", "");
		s=s.replace("3", "");
		s=s.replace("4", "");
		s=s.replace("5", "");
		s=s.replace("6", "");
		s=s.replace("7", "");
		s=s.replace("8", "");
		s=s.replace("9", "");
		return s;
	}

	public String getKlausur() {
		return klausur;
	}

	public String getArt() {
		return art;
	}
	public String getTag(){
		return tag;
	}
}
