package org.cs.washington.cse477;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class AddAudioSampleDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.audio_upload_dlg_title)
               .setNegativeButton(R.string.audio_upload_dlg_cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                	   // do nothing
                	   return;
                   }
               })
               .setPositiveButton(R.string.audio_upload_dlg_continue, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {

                	   return;
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}