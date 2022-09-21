package com.easycodingg.intouch.ui.imageviewer;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.easycodingg.intouch.databinding.FragmentImageViewerBinding;
import com.easycodingg.intouch.models.Message;

public class ImageViewerFragment extends Fragment {
    private static final String TAG = "ImageViewerFragmentyy";

    private FragmentImageViewerBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImageViewerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



//        Message message = ImageViewerFragmentArgs.fromBundle(getArguments()).getMessage();
        Message message = (Message) getArguments().getSerializable("message");
        binding.imgMessage.setImageURI(Uri.parse(message.localFileUriString));

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
