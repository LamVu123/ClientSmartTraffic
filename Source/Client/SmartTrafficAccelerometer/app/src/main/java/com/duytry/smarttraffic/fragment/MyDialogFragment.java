package com.duytry.smarttraffic.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.duytry.smarttraffic.R;

public class MyDialogFragment extends DialogFragment {

    String message;
    String tittle;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTittle() {
        return tittle;
    }

    public void setTittle(String tittle) {
        this.tittle = tittle;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setTitle(tittle)
                .setNeutralButton(R.string.ok_button, null);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
