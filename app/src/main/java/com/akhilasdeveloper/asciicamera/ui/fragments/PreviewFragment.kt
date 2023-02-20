package com.akhilasdeveloper.asciicamera.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.akhilasdeveloper.asciicamera.databinding.FragmentPreviewBinding

class PreviewFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder:AlertDialog.Builder = AlertDialog.Builder(activity)

        val view = FragmentPreviewBinding.inflate(layoutInflater, null, false).root
        builder.setView(view)

        return builder.create()
    }

}