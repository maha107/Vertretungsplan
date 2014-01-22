package de.maxgb.vertretungsplan.fragments.stundenplan;

import java.util.ArrayList;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import de.maxgb.vertretungsplan.NormalUebersichtActivity;
import de.maxgb.vertretungsplan.manager.StundenplanManager;
import de.maxgb.vertretungsplan.util.Stunde;

public class NormalStundenplanFragment extends StundenplanFragment {

	@Override
	protected void anzeigen(ScrollView s) {
		s.removeAllViews();
		ArrayList<Stunde[]> stundenplan = StundenplanManager.getInstance().getStundenplan();
		if (stundenplan == null) {
			s.addView(newTextViewCentered("Stundenplan noch nicht heruntergeladen bitte �ffne das entsprechende Optionsmenu: ("+StundenplanManager.getInstance().getLastResult()+")"));
			return;
		}
		
		//Neues TableLayout f�r 1. Stundenplan ScrollView 2. Button f�r Gesamt�bersicht
		TableLayout t = new TableLayout(getActivity());
		ScrollView s2 = new ScrollView(getActivity());
		t.addView(s2);
		super.anzeigen(stundenplan, s2);
		
		addUebersichtButton(t);

		
		//Table zum HauptScrollView hinzuf�gen
		s.addView(t);

	}
	
	protected void addUebersichtButton(TableLayout t){
		Button uebersicht=new Button(getActivity());
		uebersicht.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), NormalUebersichtActivity.class);
				startActivity(i);
				
			}
			
		});
		uebersicht.setText("�bersicht");
		uebersicht.setHeight(15);
		
		t.addView(uebersicht);
	}
}
