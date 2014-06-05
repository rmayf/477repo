package org.cs.washington.cse477;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DeviceSetupErrorDialog extends DialogFragment {
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dev_setup_error_dlg_prompt)
               .setPositiveButton(R.string.dev_setup_error_dlg_b_text, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   DeviceSetupErrorDialog.this.getDialog().cancel();
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
