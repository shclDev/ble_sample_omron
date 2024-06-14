package jp.co.ohq.blesampleomron.view.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import jp.co.ohq.blesampleomron.R;

abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(onGetTitle().toUpperCase());
    }

    @NonNull
    protected String onGetTitle() {
        return getString(R.string.app_name);
    }
}
