package com.olympus.dmmobile.settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 *Class used to show each e-mail in different box
 * 
 * @version 1.0.1
 *
 */

public class ChipsMultiAutoCompleteTextview extends MultiAutoCompleteTextView
		implements OnItemClickListener {

	private final String TAG = "ChipsMultiAutoCompleteTextview"; 
	public static String[] tempArray;
	private String chips[];
	private Bitmap viewBmp;
	private AlertDialog.Builder alertDialog;
	private Context context1;
	private boolean has64Email = false;
	private DMApplication dmApplication=null;
	private String invalidEmail=null;
	/**
	 * Constructor 
	 * @param context
	 */
	public ChipsMultiAutoCompleteTextview(Context context) {
		super(context);
		init(context);
		context1 = context;
		dmApplication=(DMApplication)context1.getApplicationContext();
	}
	public ChipsMultiAutoCompleteTextview(Context context, String[] Chips) {
		super(context);
		init(context);
		context1 = context;
		dmApplication=(DMApplication)context1.getApplicationContext();
	}
	/* Constructor */
	public ChipsMultiAutoCompleteTextview(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
		context1 = context;
		dmApplication=(DMApplication)context1.getApplicationContext();
	}
	/* Constructor */
	public ChipsMultiAutoCompleteTextview(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(context);
		context1 = context;
		dmApplication=(DMApplication)context1.getApplicationContext();
	}
	/* set listeners for item click and text change */
	public void init(Context context) {
		setOnItemClickListener(this);
		addTextChangedListener(textWatcher);
		context1 = context;
	}
	private TextWatcher textWatcher = new TextWatcher() {
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			
			if (count >= 1) {
				if (s.charAt(start) == ',') {
					setEmailids();
				}
			}
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
//			emailNum=  getText().toString().trim().split(",");
//			Log.e("", msg)
//			if(emailNum.length==64){
//				
//				setSelection(getText().length());
//			}
		}
		@Override
		public void afterTextChanged(Editable s) {
		}
	};
	/**
	 * Method to set the mail in box
	 * 
	 * @param Passedchips array of mail 
	 * 
	 * 
	 */
	private void startLooping(String[] Passedchips) {	
		String temp = Passedchips[0] + ",";
		for (int i = 1; i < Passedchips.length; i++) {
			if (isValidEmail(Passedchips[i])) {
				String val = null;
				for (int j = 0; j < i; j++) {
					val = null;
					if (Passedchips[i].equalsIgnoreCase(Passedchips[j]))
						break;
					val = Passedchips[i];
				}
				if (val != null && Passedchips[i].length() <= 256)
					temp = temp + val + ",";
			} else {				
				showMessageAlert(getResources().getString(R.string.Settings_Invalid_Email),getResources().getString(R.string.entervalidemail));
			}
		}
		int x = 0;
		SpannableStringBuilder ssb = new SpannableStringBuilder(temp);
		Passedchips = temp.split(",");
		for (int i = 0; i < Passedchips.length; i++)
			if (i < Passedchips.length - 1)
				Passedchips[i] = Passedchips[i] + ",";
		if (!Passedchips[Passedchips.length - 1].contains(","))
			Passedchips[Passedchips.length - 1] = Passedchips[Passedchips.length - 1]
					+ ",";
		for (String c : Passedchips) {
			// inflate chips_edittext layout
			LayoutInflater lf = (LayoutInflater) getContext().getSystemService(
					Activity.LAYOUT_INFLATER_SERVICE);
			TextView textView = (TextView) lf.inflate(R.layout.chips_edittext,
					null);
			textView.setText(c); // set text 
			setFlags(textView, c); // set flag image
			// capture bitmapt of genreated textview
			int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			textView.measure(spec, spec);
			textView.layout(0, 0, textView.getMeasuredWidth(),
					textView.getMeasuredHeight());		
			Canvas canvas = new Canvas(Bitmap.createBitmap(textView.getWidth(),
					textView.getHeight(), Bitmap.Config.ARGB_8888));
			canvas.translate(-textView.getScrollX(), -textView.getScrollY());
			textView.draw(canvas);
			textView.setDrawingCacheEnabled(true);
			viewBmp = textView.getDrawingCache().copy(Bitmap.Config.ARGB_8888,
					true);
			textView.destroyDrawingCache(); 
			BitmapDrawable bmpDrawable = new BitmapDrawable(viewBmp);
			bmpDrawable.setBounds(0, 0, bmpDrawable.getIntrinsicWidth(),
					bmpDrawable.getIntrinsicHeight());
			// create and set imagespan
			ssb.setSpan(new ImageSpan(bmpDrawable), x, x + c.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			x = x + c.length();
			System.gc();
			bmpDrawable.setCallback(null);
			viewBmp=null;
			//viewBmp.recycle();
		}
		
		setText(ssb);		
		setSelection(getText().length());

		if(invalidEmail!=null&&getText().toString().trim().split(",").length!=64){
			int start = getSelectionStart();
			int end = getSelectionEnd();
			getText().replace(Math.min(end, start),
					Math.max(end, start), invalidEmail, 0, invalidEmail.length());
			invalidEmail=null;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		setEmailids();// call generate chips when user select any item from auto
						// complete
	}

	public void setFlags(TextView textView, String country) {
		country = country.trim();
	}

	public String removeFirstChar(String s) {
		return s.substring(1);
	}
/**
 * Method to set each email in a box
 * 
 * 
 */
	public void setEmailids() {
		
		/*String[] chips = {"one@m2.com","two@m2.com"};
		startLooping(chips);
		System.out.println("text : "+getText().toString());*/
		
		
		String mail;
		mail = getText().toString().trim();
	
		if (mail.length() != 1 && mail.charAt(0) != ',') {
			if (mail.contains(",")) // check comma in string
			{
				int count = mail.length() - mail.replace(",", "").length();
				if(count==1){
					String[] seprate = mail.split(",");
					if(isValidEmail(seprate[0])){
						startLooping(seprate);	
					}
					else{
						setText(seprate[0]);
						showMessageAlert(getResources().getString(R.string.Settings_Invalid_Email),getResources().getString(R.string.entervalidemail));
					}
					setSelection(getText().length());
				}
				else{
				String[] firstSeperate=mail.split(",");
				mail=null;
				for(int j=0;j<firstSeperate.length;j++){
					if(!isValidEmail(firstSeperate[j])){
						invalidEmail=firstSeperate[j];
					}
					if(mail==null&&!firstSeperate[j].equalsIgnoreCase("")){
						mail=firstSeperate[j]+",";
					}else if(mail!=null&&!firstSeperate[j].equalsIgnoreCase("")){
						mail=mail+firstSeperate[j]+",";
					}
				}
				String[] chip = mail.split(",");
				if(chip.length<65){
				for(int i=0;i<chip.length;i++){
					if(chip[i].equalsIgnoreCase("")){
						continue;
					}
					else if(!isValidEmail(chip[i])&&!chip[i].equalsIgnoreCase("")){
						showMessageAlert(getResources().getString(R.string.Settings_Invalid_Email),getResources().getString(R.string.entervalidemail));
					    invalidEmail=chip[i];
					    break;
					}
				}
				if (isValidEmail((chip[chip.length - 1])) && chip.length < 65) {
					String chipset = null;
					for (int i = 0; i < chip.length; i++) {
						if (isValidEmail(chip[i]) && chipset == null) {
							chipset = chip[i] + ",";
						} else if (isValidEmail(chip[i]) && chipset != null) {
							chipset = chipset + chip[i] + ",";
						}
					}
					String[] mainChip = chipset.split(",");
					startLooping(mainChip);
				} else {
				if(chip.length>64){  
					showMessageAlert(getResources().getString(
							R.string.Alert),getResources().getString(
									R.string.Settings_Max_Recipients));
				}
				
					if (chip.length == 1) {
						setText("");
						int start = getSelectionStart();
						int end = getSelectionEnd();
						getText().replace(Math.min(end, start),
								Math.max(end, start), invalidEmail, 0, invalidEmail.length());
						invalidEmail=null;
					} else {
						String[] tempChip = new String[chip.length - 1];
						for (int i = 0; i < chip.length - 1; i++) {
							tempChip[i] = chip[i];						    
						}
						if (tempChip.length != 0) {
							startLooping(tempChip);
						}
						if (tempChip.length == 64)
							has64Email = true;
					}
				}
			 }
				else{
					showMessageAlert(getResources().getString(
							R.string.Alert),getResources().getString(
									R.string.Settings_Max_Recipients));
					String tempchip[]=new String[chip.length-1];
					for(int i=0;i<chip.length-1;i++){
						
						tempchip[i]=chip[i];
					}
					startLooping(tempchip);
				}
			}
			}
		} else {
			//showMessageAlert(getResources().getString(R.string.Settings_Invalid_Email),getResources().getString(R.string.entervalidemail));
			 invalidEmail=getText().toString();
			   
			if (mail.length() == 1){
				setText("");
			}
			else {
				String[] chip = mail.split(",");
				String chipset = null;
				for (int i = 0; i < chip.length; i++) {
					if (isValidEmail(chip[i]) && chipset == null) {
						chipset = chip[i] + ",";
					} else if (isValidEmail(chip[i]) && chipset != null) {
						chipset = chipset + chip[i] + ",";
					}
				}
				String[] mainChip = chipset.split(",");
				startLooping(mainChip);
			}
		}
	}
	/**
	 * Method to show alert 
	 * 
	 * @param title title of the alert
	 * 
	 * @param Message message to be displayed in the alert
	 * 
	 * 
	 */
	public void showMessageAlert(String title,String Message){
		if(dmApplication.getShowAlert()==0){
		dmApplication.setShowAlert(1);
		alertDialog= new AlertDialog.Builder(context1);
		alertDialog.setTitle(title);
		alertDialog.setMessage(Message);
		alertDialog.setCancelable(false);
		alertDialog.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog,int which){
				dmApplication.setShowAlert(0);
			}
		});
		alertDialog.show(); 
		}
	}
	/**
	 * Method to check whether mail is valid or not
	 * 
	 * @param email mail need to be checked for validation
	 * 
	 * @return boolean value whether it is valid or not
	 * 
	 * 
	 */
	public boolean isValidEmail(String email)
	 {
	     boolean isValidEmail = false;

	     String emailExpression = "^(?!.*\\.{2})[A-Z0-9a-z._%+-]+@[A-Za-z0-9]+[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
	     CharSequence inputStr = email;

	     Pattern pattern = Pattern.compile(emailExpression, Pattern.CASE_INSENSITIVE);
	     Matcher matcher = pattern.matcher(inputStr);
	     if (matcher.matches())
	     {
	         isValidEmail = true;
	     }
	     return isValidEmail;
	 } 
}
