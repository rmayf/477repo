package org.cs.washington.cse477;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

public class DeviceSetupErrorDialog extends DialogFragment {
	
	 /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
	public interface DeviceSetupErrorDialogListener {
		public void onErrorDialogPositiveClick(DialogFragment dialog);
	}
	
    // Use this instance of the interface to deliver action events
	DeviceSetupErrorDialogListener mListener;
	
	  // Override the Fragment.onAttach() method to instantiate the ConfirmDeleteListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the ConfirmDeleteListener so we can send events to the host
            mListener = (DeviceSetupErrorDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ConfirmDeleteListener");
        }
    }
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.setup_error_dialog, null))
               .setPositiveButton(R.string.dev_setup_error_dlg_b_text, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
//                	   DeviceSetupErrorDialog.this.getDialog().cancel();
                	   mListener.onErrorDialogPositiveClick(DeviceSetupErrorDialog.this);
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
