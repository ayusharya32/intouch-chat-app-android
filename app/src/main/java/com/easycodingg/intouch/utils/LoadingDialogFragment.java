package com.easycodingg.intouch.utils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.easycodingg.intouch.databinding.DialogLoadingBinding;

public class LoadingDialogFragment extends DialogFragment {

    private DialogLoadingBinding dialogBinding;
    private String message = "";

    public LoadingDialogFragment(String message) {
        this.message = message;
    }

    public static LoadingDialogFragment getLoadingDialogFragmentInstance(String message) {
        return new LoadingDialogFragment(message);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        dialogBinding = DialogLoadingBinding.inflate(inflater, container, false);
        return dialogBinding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setCancelable(false);
        dialogBinding.txtMessage.setText(message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        dialogBinding = null;
    }
}
