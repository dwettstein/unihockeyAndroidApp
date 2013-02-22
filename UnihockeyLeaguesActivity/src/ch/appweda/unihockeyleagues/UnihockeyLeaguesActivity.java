package ch.appweda.unihockeyleagues;

import ch.appweda.swissunihockey.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Picture;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * This app shows you the table, results and next rounds of the chosen
 * swiss-floorball-group.
 * 
 * @author David Wettstein
 */
@SuppressWarnings("deprecation")
public class UnihockeyLeaguesActivity extends Activity {

	private Spinner spinnerLeague;
	private Spinner spinnerGroup;
	private Spinner spinnerSeason;
	private WebView webView;
	private ConnectivityManager connectMan;

	private String season;
	private String league;
	private String group;
	private String button;

	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	/** Implements the function of the back-button for the WebView. */
	@Override 
	public void onBackPressed() { 
		if(webView.canGoBack()) 
			webView.goBack(); 
		else 
			super.onBackPressed();
	}
	
	protected void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
		icicle.putString("season", season);
		icicle.putString("league", league);
		icicle.putString("group", group);
		icicle.putString("button", button);
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			season = savedInstanceState.getString("season");
			league = savedInstanceState.getString("league");
			group = savedInstanceState.getString("group");
		}
		this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setTitle("");
		setContentView(R.layout.main);
		this.getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
				Window.PROGRESS_VISIBILITY_ON);

		this.webView = (WebView) findViewById(R.id.webView1);
		this.webView.getSettings().setSupportZoom(true);
		this.webView.getSettings().setBuiltInZoomControls(true);
		this.webView.getSettings().setLoadWithOverviewMode(true);
		this.webView.getSettings().setUseWideViewPort(true);
		//this.webView.setInitialScale(getScale());

		this.webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return false;
			}

			/** Checks if the url contains the string "resultat" and if so it scrolls the page to the bottom after completely loading it.*/
			@Override
			public void onPageFinished(WebView view, String url) {
				webView.setPictureListener(new PictureListener() {
					public void onNewPicture(WebView view, Picture picture) {
						if (view.getUrl().contains("resultat"))
							view.pageDown(true);
					}
				});
			}
		});
		
		createProgressBar();
		checkConnection();
		checkSavings();
		if (season != null || league != null || group != null)
			openButton(0);
	}
	
	/**
	 * Sets the chrome client and defines the method onProgressChanged. This
	 * makes the progress bar be updated.
	 */
	private void createProgressBar() {
		final Activity activity = this;
		this.webView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				activity.setProgress(progress * 100);
			}
		});
	}

	/**
	 * Checks if an Internet connection exists and loads the information from
	 * cache if not.
	 *
	 * This method needs the permissions for network and wifi state:
	 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
	 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
	 */
	private void checkConnection() {
		this.connectMan = (ConnectivityManager) this
				.getSystemService(Activity.CONNECTIVITY_SERVICE);

		if (this.connectMan != null
				&& this.connectMan.getActiveNetworkInfo() != null
				&& this.connectMan.getActiveNetworkInfo().isConnected()) {
			this.webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
		} else {
			this.webView.getSettings().setCacheMode(
					WebSettings.LOAD_CACHE_ELSE_NETWORK);
		}
	}

	/** Checks if there exists previously saved information. */
	private void checkSavings() {
		this.settings = getSharedPreferences("SwissUnihockeyPrefs",
				MODE_PRIVATE);
		this.editor = settings.edit();

		if ((settings.contains("season") || settings.contains("league") || settings
				.contains("group")) == false) {
			if (season == null || league == null || group == null)
				createChooseLeagueDialog();
		} else {
			season = settings.getString("season", season);
			league = settings.getString("league", league);
			group = settings.getString("group", group);
		}
	}

	/** Computes the optimal scale for webView. */
	private int getScale() {
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		int width = display.getWidth();
		Double val = Double.valueOf(width) / Double.valueOf(550);
		val = val * 100d;
		return val.intValue();
	}

	/** Inflates the menu-xml for the app-menu. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/** Defines what the app should do, when an option is chose. */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// app icon in action bar clicked; go home
				Intent intent = new Intent(this, UnihockeyLeaguesActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			case R.id.table:
				openButton(R.id.table);
				return true;
			case R.id.results:
				openButton(R.id.results);
				return true;
			case R.id.round:
				openButton(R.id.round);
				return true;
			case R.id.change_league:
				createChooseLeagueDialog();
				return true;
			case R.id.favorites:
				return true;
			case R.id.info:
				createInfoDialog();
				return true;
			case R.id.exit:
				UnihockeyLeaguesActivity.this.finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Creates an AlertDialog with the given layout, to choose the season,
	 * league and group.
	 */
	public void createChooseLeagueDialog() {
		final LayoutInflater factory = getLayoutInflater();
		final View dialog = factory.inflate(R.layout.dialog, null);

		spinnerSeason = (Spinner) dialog.findViewById(R.id.season);
		ArrayAdapter<CharSequence> adapterSeason = ArrayAdapter
				.createFromResource(this, R.array.season_array,
						android.R.layout.simple_spinner_item);
		adapterSeason
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerSeason.setAdapter(adapterSeason);

		spinnerLeague = (Spinner) dialog.findViewById(R.id.league);
		ArrayAdapter<CharSequence> adapterLeague = ArrayAdapter
				.createFromResource(this, R.array.league_array,
						android.R.layout.simple_spinner_item);
		adapterLeague
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerLeague.setAdapter(adapterLeague);

		spinnerGroup = (Spinner) dialog.findViewById(R.id.group);
		ArrayAdapter<CharSequence> adapterGroup = ArrayAdapter
				.createFromResource(this, R.array.group_array,
						android.R.layout.simple_spinner_item);
		adapterGroup
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerGroup.setAdapter(adapterGroup);

		final CheckBox storeInfo = (CheckBox) dialog
				.findViewById(R.id.saveInfo);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Bitte waehlen Sie:")
				.setView(dialog)
				.setPositiveButton("Los!",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								season = spinnerSeason.getSelectedItem()
										.toString();
								league = spinnerLeague.getSelectedItem()
										.toString();
								group = spinnerGroup.getSelectedItem()
										.toString();

								if (storeInfo.isChecked()) {
									editor.clear();
									editor.commit();
									editor.putString("season", season);
									editor.putString("league", league);
									editor.putString("group", group);
									editor.commit();

									Toast.makeText(getApplicationContext(),
											"Ihre Auswahl wurde gespeichert!",
											Toast.LENGTH_SHORT).show();
								}

								Toast.makeText(
										getApplicationContext(),
										"Season: " + season + ", Liga: "
												+ league + " und Gruppe: "
												+ group, Toast.LENGTH_SHORT)
										.show();
								openButton(R.id.table);
								removeDialog(0);
							}
						})
				.setNegativeButton("Abbrechen",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								removeDialog(0);
							}
						});

		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Creates the information pop-up window with text based on a file in assets
	 * folder.
	 */
	public void createInfoDialog() {
		final LayoutInflater factory = getLayoutInflater();
		final View dialog = factory.inflate(R.layout.info, null);
		final WebView info = (WebView) dialog.findViewById(R.id.viewInfo);
		info.loadUrl("file:///android_asset/info.html");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.info)).setView(dialog)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						removeDialog(0);
					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}

	/** Opens the clicked button of the GUI and opens the created URL.
	 * 
	 * Needs the permission to connect to the internet:
	 * <uses-permission android:name="android.permission.INTERNET"/>
	 */
	public void openButton(int id) {
		if (button == null || id == R.id.table)
			button = "tabelle";
		if (id == R.id.results)
			button = "resultat";
		if (id == R.id.round)
			button = "spiel";
		
		if (season == null || league == null || group == null || button == null) {
			Toast.makeText(getApplicationContext(),
						   "Sie muessen zuerst eine Liga waehlen.", Toast.LENGTH_SHORT)
				.show();
		} else {
			String url = createURL(button, season, league, group);
			this.webView.loadUrl(url);
		}
	}
	
	/** Creates the URL relative to the button clicked by the user. */
	public String createURL(String button, String season, String league,
			String group) {
		if (league.equals("1"))
			league = "3";
		else if (league.equals("2"))
			league = "4";
		else if (league.equals("3"))
			league = "5";
		else if (league.equals("4"))
			league = "6";
		else if (league.equals("5"))
			league = "7";

		// Code=12.6.04 (KF 4. Liga, Gruppe 4), where
		// 1<GF:1/KF:2>.<League>.<Group>
		// League KF: 3 - 1.Liga,..., 7 - 5.Liga
		// http://www.swissunihockey.ch/spielbetrieb/tabelle?season=2011&shortcode=12.6.04&layout=club&css=media.swissunihockey.ch/www-201012/css/content.css
		String createdURL = "http://www.swissunihockey.ch/spielbetrieb/";
		String cssURL = "media.swissunihockey.ch/www-201012/css/content.css";
		createdURL = createdURL
				+ button
				+ "?season="
				+ season
				+ "&shortcode=12."
				+ league
				+ "."
				+ group
				+ "&layout=club&css="
				+ cssURL;

		return createdURL;
	}

}
