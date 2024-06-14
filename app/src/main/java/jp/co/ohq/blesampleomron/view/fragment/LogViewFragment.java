package jp.co.ohq.blesampleomron.view.fragment;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.ohq.blesampleomron.R;
import jp.co.ohq.blesampleomron.controller.util.AppLog;
import jp.co.ohq.blesampleomron.model.entity.HistoryData;
import jp.co.ohq.blesampleomron.model.service.LogViewService;
import jp.co.ohq.blesampleomron.model.system.ExportResultFileManager;
import jp.co.ohq.blesampleomron.model.system.HistoryManager;
import jp.co.ohq.utility.Handler;

import static jp.co.ohq.blesampleomron.model.system.ExportResultFileManager.OverlapActionEnum.OVERLAP_ACTION_OVERWRITE;

public class LogViewFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<HistoryData>>{
    private static final int LOADER_ID_LOAD_HISTORY = 1;
    private static final int ACTIVITY_RESULT = 0;

    private int mCurrentScrollPosition = 0;
    private String mLogPath;
    private String mPathExportFile;
    private String mZipFileName;
    private List<HistoryData> mHistoryDataList;
    private LogViewService mLogViewService;
    private Timer timer;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            AppLog.vMethodIn();
            mLogViewService = ((LogViewService.LocalBinder) service).getService();
            updateLogView();
        }
        public void onServiceDisconnected(ComponentName name) {
            AppLog.vMethodIn();
            mLogViewService = null;
        }
    };
    private TimerTask task = new TimerTask() {
        public void run() {
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateLogView();
                }
            });
        }
    };

    public static LogViewFragment newInstance() {
        AppLog.vMethodIn();
        return new LogViewFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AppLog.vMethodIn();
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        bindLogService();
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_LOAD_HISTORY, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        AppLog.vMethodIn();
        return inflater.inflate(R.layout.fragment_log_view, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_log_view, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.export) {
            saveLog();
            mPathExportFile = ExportResultFileManager.sharedInstance().ExportCompressedFile(getContext(), null, OVERLAP_ACTION_OVERWRITE, mHistoryDataList, mLogPath);
            sendMail(mPathExportFile);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        AppLog.vMethodIn();
        super.onViewCreated(view, savedInstanceState);

        ListView listView = getListView();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView,
                                 int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mCurrentScrollPosition = firstVisibleItem;
            }
        });
        startTimer();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(getString(R.string.log).toUpperCase());
    }

    @Override
    public void onDestroy() {
        AppLog.vMethodIn();
        stopTimer();
        super.onDestroy();
        unbindLogService();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String item = (String) getListAdapter().getItem(position);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage(item);
        alertDialogBuilder.setPositiveButton("OK", null);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void bindLogService() {
        getContext().bindService(new Intent(getActivity(), LogViewService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindLogService() {
        if (null == mLogViewService) {
            return;
        }
        getContext().unbindService(mServiceConnection);
        mLogViewService = null;
    }

    private void updateLogView() {
        if(mLogViewService == null) {
            return;
        }
        List<String> logList = mLogViewService.getLog();
        ArrayAdapter<String> logListAdapter = new ArrayAdapter<>(getActivity(), R.layout.activity_logview_list_row, logList);
        setListAdapter(logListAdapter);
        getListView().setSelection(mCurrentScrollPosition);
    }

    private String saveLog() {
        AppLog.vMethodIn();
        List<String> logList = mLogViewService.getLog();
        File logDir = new File(HistoryManager.sharedInstance().getDirectoryPath(getContext()) + "/temp/" + createDateTimeString());
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                AppLog.e("Failed to make directory " + logDir);
                return null;
            }
        }
        String fileName = createLogFileName();
        String fullPath = logDir.getPath() + "/" + fileName;
        mLogPath = fullPath;
        AppLog.d("Log file:" + fullPath);

        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullPath), "UTF-8"));
        } catch (IOException ioe) {
            Toast.makeText(getActivity(), R.string.save_failed, Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            for (String line : logList) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getActivity(), R.string.save_failed, Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.save_failed, Toast.LENGTH_SHORT).show();
            return null;
        }
        Toast.makeText(getActivity(), R.string.save_complete, Toast.LENGTH_SHORT).show();
        Toast.makeText(getActivity(), fileName, Toast.LENGTH_SHORT).show();
        return fullPath;
    }

    private void sendMail(String fileName) {
        AppLog.vMethodIn("fileName:" + fileName);
        if (null == fileName) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, getActivity().getPackageName() + " " + mZipFileName);
        intent.putExtra(Intent.EXTRA_TEXT, "Attached.\r\n");
        Uri uri = FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".provider", new File(fileName));
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("text/plain");

        Intent chooser = Intent.createChooser(intent, "Selected E-mail application");
        List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(chooser, getContext().getPackageManager().MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            getContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivityForResult(chooser, ACTIVITY_RESULT);
    }

    private String createLogFileName() {
        String fileName = "log_";
        fileName += createDateTimeString();
        fileName += ".txt";
        return fileName;
    }

    private String createDateTimeString() {
        Calendar cal = Calendar.getInstance();
        String dateTime = String.format(Locale.US, "%1$04d", cal.get(Calendar.YEAR));
        dateTime += String.format(Locale.US, "%1$02d", cal.get(Calendar.MONTH) + 1);
        dateTime += String.format(Locale.US, "%1$02d", cal.get(Calendar.DATE));
        dateTime += String.format(Locale.US, "%1$02d", cal.get(Calendar.HOUR_OF_DAY));
        dateTime += String.format(Locale.US, "%1$02d", cal.get(Calendar.MINUTE));
        dateTime += String.format(Locale.US, "%1$02d", cal.get(Calendar.SECOND));
        mZipFileName = dateTime;
        return dateTime;
    }

    public void startTimer() {
        this.timer = new Timer();
        timer.schedule(task, 0, 2000);
    }

    public void stopTimer() {
        task.cancel();
        task = null;
    }

    @NonNull
    @Override
    public Loader<List<HistoryData>> onCreateLoader(int id, @Nullable Bundle args) {
        Loader<List<HistoryData>> loader = null;
        switch (id) {
            case LOADER_ID_LOAD_HISTORY:
                loader = new HistoryManager.HistoryLoader(getContext(), HistoryManager.HistoryLoader.Operation.Load);
                break;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<HistoryData>> loader, @NonNull final List<HistoryData> historyDataList) {
        mHistoryDataList = historyDataList;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<HistoryData>> loader) {
    }
}
