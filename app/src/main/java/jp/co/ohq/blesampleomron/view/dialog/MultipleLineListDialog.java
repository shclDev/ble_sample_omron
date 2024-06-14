package jp.co.ohq.blesampleomron.view.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.ListView;

import jp.co.ohq.ble.enumerate.OHQDeviceCategory;
import jp.co.ohq.blesampleomron.R;
import jp.co.ohq.blesampleomron.model.entity.MultipleLineItem;
import jp.co.ohq.blesampleomron.view.adapter.MultipleLineListAdapter;

public class MultipleLineListDialog extends DialogFragment {

    private static final String ARG_TITLE = "ARG_TITLE";
    private static final String ARG_ITEMS = "ARG_ITEMS";
    private static final String ARG_DEVICE_CATEGORY = "ARG_CATEGORY";

    private String mTitle;
    private MultipleLineListAdapter mAdapter;
    private String mDeviceCategory;

    public static MultipleLineListDialog newInstance(@NonNull String title, @NonNull MultipleLineItem[] items, @NonNull OHQDeviceCategory deviceCategory) {
        MultipleLineListDialog fragment = new MultipleLineListDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putParcelableArray(ARG_ITEMS, items);
        args.putString(ARG_DEVICE_CATEGORY, String.valueOf(deviceCategory));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mTitle = args.getString(ARG_TITLE);
        mDeviceCategory = args.getString(ARG_DEVICE_CATEGORY);
        if (mTitle == null) {
            if (mDeviceCategory.equals(String.valueOf(OHQDeviceCategory.PulseOximeter))) {
                mTitle = getString(R.string.time_stamp_all_0);
            }else if (mDeviceCategory.equals(String.valueOf(OHQDeviceCategory.HealthThermometer))) {
                mTitle = getString(R.string.time_stamp_all_0);
            }else{
                throw new IllegalArgumentException("Argument '" + ARG_TITLE + "' must not be null.");
            }
        }

        MultipleLineItem[] items = (MultipleLineItem[]) args.getParcelableArray(ARG_ITEMS);
        if (items == null) {
            throw new IllegalArgumentException("Argument '" + ARG_ITEMS + "' must not be null.");
        }

        mAdapter = new MultipleLineListAdapter(getContext(), items);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.multiple_line_list, null);
        ListView listView = (ListView) view.findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        return new AlertDialog.Builder(getActivity())
                .setTitle(mTitle)
                .setView(view)
                .setNegativeButton(getString(R.string.close), null)
                .create();
    }
}
