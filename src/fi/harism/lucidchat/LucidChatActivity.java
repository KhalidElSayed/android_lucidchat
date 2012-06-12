package fi.harism.lucidchat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

public class LucidChatActivity extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        ViewGroup chat = (ViewGroup)findViewById(R.id.chat);
        for (int i=0; i<20; ++i) {
        	View view = getLayoutInflater().inflate(R.layout.item, null);
        	TextView tv = (TextView)view.findViewById(R.id.text);
        	
        	SpannableString span = new SpannableString("01:03 TEST: ASKDSA ASDKSAOKD SADKSAODKO SADKOASDKO SDOSAKOKO SADOKASD SADKO");
        	
        	span.setSpan(new ForegroundColorSpan(0xFF4CBAED), 0, 5, 0);
        	span.setSpan(new ForegroundColorSpan(0xFF17AEF4), 6, 11, 0);
        	span.setSpan(new ForegroundColorSpan(0xFFD0D0D0), 12, 20, 0);
        	span.setSpan(new ForegroundColorSpan(0xFF63CB63), 20, 28, 0);
        	
        	span.setSpan(new URLSpan("tel:+358-123 123 123"), 29, 35, 0);        	
        	span.setSpan(new URLSpan("http://www.hs.fi"), 37, 45, 0);
        	span.setSpan(new URLSpan("mailto:harism@gmail.com"), 47, 55, 0);
        	span.setSpan(new ForegroundColorSpan(0xFF6AD46A), 47, 55, 0);
        	
        	tv.setMovementMethod(LinkMovementMethod.getInstance());        	
        	tv.setText(span, BufferType.SPANNABLE);
        	
        	chat.addView(view);
        }
        
        
        Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.main);
        dlg.show();
        
    }
}