package com.miz.mizuu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TvShowActorSearchActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		Intent i = new Intent(this, Main.class);
		if (extras != null)
			i.putExtras(extras);
		i.putExtra("startup", "2");
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		finish();
	}

}
