package jp.co.ohq.blesampleomron.view.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.co.ohq.ble.enumerate.OHQBloodPressureMeasurementStatus;
import jp.co.ohq.ble.enumerate.OHQDeviceCategory;
import jp.co.ohq.ble.enumerate.OHQGender;
import jp.co.ohq.ble.enumerate.OHQMeasurementRecordKey;
import jp.co.ohq.ble.enumerate.OHQUserDataKey;
import jp.co.ohq.blesampleomron.R;
import jp.co.ohq.blesampleomron.controller.util.AppLog;
import jp.co.ohq.blesampleomron.controller.util.Common;
import jp.co.ohq.blesampleomron.model.entity.HistoryData;
import jp.co.ohq.blesampleomron.model.entity.MultipleLineItem;
import jp.co.ohq.blesampleomron.model.enumerate.Protocol;
import jp.co.ohq.blesampleomron.model.service.LogViewService;
import jp.co.ohq.blesampleomron.model.system.ExportResultFileManager;
import jp.co.ohq.blesampleomron.model.system.HistoryManager;
import jp.co.ohq.blesampleomron.view.adapter.AbstractRecyclerAdapter;
import jp.co.ohq.blesampleomron.view.adapter.BloodPressureRecyclerAdapter;
import jp.co.ohq.blesampleomron.view.adapter.BodyCompositionRecyclerAdapter;
import jp.co.ohq.blesampleomron.view.adapter.PulseOximeterRecyclerAdapter;
import jp.co.ohq.blesampleomron.view.adapter.ThermometerRecyclerAdapter;
import jp.co.ohq.blesampleomron.view.adapter.WeightScaleRecyclerAdapter;
import jp.co.ohq.blesampleomron.view.dialog.MultipleLineListDialog;
import jp.co.ohq.blesampleomron.view.dialog.ProgressSpinnerDialog;
import jp.co.ohq.blesampleomron.view.widget.DividerItemDecoration;
import jp.co.ohq.utility.StringEx;
import jp.co.ohq.utility.Types;

import static jp.co.ohq.blesampleomron.model.system.ExportResultFileManager.OverlapActionEnum.OVERLAP_ACTION_OVERWRITE;

public class ResultFragment extends BaseFragment{

    private static final String ARG_HISTORY_DATA = "ARG_HISTORY_DATA";
    private static final String ARG_HISTORY_DATA_LIST = "ARG_HISTORY_DATA_LIST";

    private static final int LOADER_ID_EXPORT_RESULT = 1;
    private static final int LOADER_ID_REMOVE_EXPORTING_RESULT = 2;

    private static final int ACTIVITY_RESULT = 0;

    private static final int LOADER_ID_LOAD_HISTORY = 1;
    private static final int LOADER_ID_REMOVE_HISTORY = 2;

    private HistoryData mHistoryData;
    private List<HistoryData> mHistoryDataList;
    private String mPathExportFile;
    private String mPathExportLog;
    private String mZipFileName;
    private ProgressSpinnerDialog mProgressDialog;

    private  boolean mTapped = false;
    private AsyncLoaderStringClass mAsyncLoaderStringClass;
    private AsyncLoaderHistoryDataListClass mAsyncLoaderHistoryDataListClass;

    private LogViewService mLogViewService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            AppLog.vMethodIn();
            mLogViewService = ((LogViewService.LocalBinder) service).getService();
        }
        public void onServiceDisconnected(ComponentName name) {
            AppLog.vMethodIn();
            mLogViewService = null;
        }
    };

    public static ResultFragment newInstance(@NonNull HistoryData historyData) {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_HISTORY_DATA, historyData);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAsyncLoaderStringClass = new AsyncLoaderStringClass();
        mAsyncLoaderHistoryDataListClass = new AsyncLoaderHistoryDataListClass();
        setHasOptionsMenu(true);
        bindLogService();
    }

    @Override
    public void onDestroy() {
        AppLog.vMethodIn();
        super.onDestroy();
        unbindLogService();
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

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AppLog.vMethodIn();
        View rootView = inflater.inflate(R.layout.fragment_result, container, false);

        Bundle args = getArguments();
        final HistoryData historyData = args.getParcelable(ARG_HISTORY_DATA);
        if (null == historyData) {
            throw new IllegalArgumentException("null == resultData");
        }
        mHistoryData = historyData;
        mProgressDialog = new ProgressSpinnerDialog();
        AppLog.d(historyData.toString());

        TextView category = (TextView) rootView.findViewById(R.id.deviceCategory);
        category.setText(historyData.getDeviceCategory().name());
        category.setTextColor(ContextCompat.getColor(getContext(), Common.getDeviceCategoryColorResource(historyData.getDeviceCategory())));
        if (Protocol.OmronExtension == historyData.getProtocol()) {
            rootView.findViewById(R.id.standardLabel).setVisibility(View.GONE);
            rootView.findViewById(R.id.omronExLabel).setVisibility(View.VISIBLE);
        }

        ((TextView) rootView.findViewById(R.id.model_name)).setText(null != historyData.getModelName() ?
                historyData.getModelName() : getString(R.string.hyphen));
        ((TextView) rootView.findViewById(R.id.localName)).setText(historyData.getLocalName());

        ((TextView) rootView.findViewById(R.id.current_time)).setText(null != historyData.getCurrentTime() ?
                historyData.getCurrentTime() : getString(R.string.hyphen));
        ((TextView) rootView.findViewById(R.id.battery_level)).setText(null != historyData.getBatteryLevel() ?
                String.format(Locale.US, "%d %%", historyData.getBatteryLevel()) : getString(R.string.hyphen));

        ((TextView) rootView.findViewById(R.id.comType)).setText(historyData.getComType().name());
        ((TextView) rootView.findViewById(R.id.receivedDate)).setText(StringEx.toDateString(historyData.getReceivedDate(), StringEx.FormatType.Form1));
        ((TextView) rootView.findViewById(R.id.result)).setText(getString(historyData.getResultType().stringResId()));

        int measurementLength = addAllMeasurementView(
                (RecyclerView) rootView.findViewById(R.id.measurements), historyData.getDeviceCategory(), historyData.getMeasurementRecords());
        rootView.findViewById(R.id.measurement_layout).setVisibility(View.VISIBLE);
        ((TextView) rootView.findViewById(R.id.measurement_count)).setText(getString(R.string.measurement_size, measurementLength));
        if (0 < measurementLength) {
            rootView.findViewById(R.id.measurements).setVisibility(View.VISIBLE);
        }

        if (null != historyData.getUserIndex()) {
            LinearLayout userInformationLayout = (LinearLayout) rootView.findViewById(R.id.user_information_layout);
            userInformationLayout.setVisibility(View.VISIBLE);

            ((TextView) rootView.findViewById(R.id.userIndex)).setText(String.format(Locale.US, "%d", historyData.getUserIndex()));

            if (null != historyData.getConsentCode()) {
                rootView.findViewById(R.id.consent_code_row).setVisibility(View.VISIBLE);
                ((TextView) rootView.findViewById(R.id.consent_code)).setText(String.format(Locale.US, "0x%04X", historyData.getConsentCode()));
            }

            Map<OHQUserDataKey, Object> userData = historyData.getUserData();
            if (!userData.isEmpty()) {
                if (null != userData.get(OHQUserDataKey.DateOfBirthKey)) {
                    rootView.findViewById(R.id.date_of_birth_row).setVisibility(View.VISIBLE);
                    ((TextView) rootView.findViewById(R.id.date_of_birth)).setText((String) userData.get(OHQUserDataKey.DateOfBirthKey));
                }
                if (null != userData.get(OHQUserDataKey.HeightKey)) {
                    rootView.findViewById(R.id.height_row).setVisibility(View.VISIBLE);
                    ((TextView) rootView.findViewById(R.id.height)).setText(Common.getDecimalStringWithUnit((BigDecimal) userData.get(OHQUserDataKey.HeightKey), 0, "cm"));
                }
                if (null != userData.get(OHQUserDataKey.GenderKey)) {
                    rootView.findViewById(R.id.gender_row).setVisibility(View.VISIBLE);
                    ((TextView) rootView.findViewById(R.id.gender)).setText(((OHQGender) userData.get(OHQUserDataKey.GenderKey)).name());
                }
            }
        }

        AppLog.vMethodOut();
        return rootView;
    }

    private int addAllMeasurementView(
            @NonNull RecyclerView parent,
            @NonNull final OHQDeviceCategory deviceCategory,
            @NonNull List<Map<OHQMeasurementRecordKey, Object>> measurements) {

        if (measurements.isEmpty()) {
            return 0;
        }

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                ContextCompat.getDrawable(getContext(), R.drawable.horizontal_divider_transparent));
        parent.addItemDecoration(dividerItemDecoration);

        final AbstractRecyclerAdapter adapter;
        switch (deviceCategory) {
            case BloodPressureMonitor:
                adapter = new BloodPressureRecyclerAdapter(getContext(), measurements);
                break;
            case WeightScale:
                adapter = new WeightScaleRecyclerAdapter(getContext(), measurements);
                break;
            case BodyCompositionMonitor:
                adapter = new BodyCompositionRecyclerAdapter(getContext(), measurements);
                break;
            case PulseOximeter:
                adapter = new PulseOximeterRecyclerAdapter(getContext(), measurements);
                break;
            case HealthThermometer:
                adapter = new ThermometerRecyclerAdapter(getContext(), measurements);
                break;
            default:
                throw new IllegalArgumentException("Invalid device category.");
        }
        adapter.setOnItemClickListener(new AbstractRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View item, int position) {
                Map<OHQMeasurementRecordKey, Object> record = Types.autoCast(adapter.getItem(position));
                showMeasurementDialog((String) record.get(OHQMeasurementRecordKey.TimeStampKey), recordToItems(record), deviceCategory);
            }
        });
        parent.setAdapter(adapter);

        return adapter.getItemCount();
    }

    private void showMeasurementDialog(@NonNull String title, @NonNull List<MultipleLineItem> items, @NonNull OHQDeviceCategory deviceCategory) {
        MultipleLineListDialog.newInstance(title, items.toArray(new MultipleLineItem[0]), deviceCategory).show(
                getChildFragmentManager(), MultipleLineListDialog.class.getSimpleName());
    }

    @NonNull
    private List<MultipleLineItem> recordToItems(@NonNull Map<OHQMeasurementRecordKey, Object> record) {
        List<MultipleLineItem> items = new LinkedList<>();

        if (record.containsKey(OHQMeasurementRecordKey.SequenceNumberKey)) {
            items.add(new MultipleLineItem("Sequence Number"));
            items.add(new MultipleLineItem(Common.getNumberString((BigDecimal) record.get(OHQMeasurementRecordKey.SequenceNumberKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.SystolicKey)) {
            items.add(new MultipleLineItem("Systolic"));
            items.add((new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.SystolicKey), 0, (String) record.get(OHQMeasurementRecordKey.BloodPressureUnitKey)), null)));
        }
        if (record.containsKey(OHQMeasurementRecordKey.DiastolicKey)) {
            items.add(new MultipleLineItem("Diastolic"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.DiastolicKey), 0, (String) record.get(OHQMeasurementRecordKey.BloodPressureUnitKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.MeanArterialPressureKey)) {
            items.add(new MultipleLineItem("Mean Arterial Pressure"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.MeanArterialPressureKey), 0, (String) record.get(OHQMeasurementRecordKey.BloodPressureUnitKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.PulseOximeterSpo2Key)) {
            items.add(new MultipleLineItem("Pulse Oximeter Spo2"));
            items.add(new MultipleLineItem(Common.getNumberStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.PulseOximeterSpo2Key), "%"), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.PulseRateKey)) {
            items.add(new MultipleLineItem("Pulse Rate"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.PulseRateKey), 0, "bpm"), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.UserIndexKey)) {
            items.add(new MultipleLineItem("User Index"));
            items.add(new MultipleLineItem(Common.getNumberString((BigDecimal) record.get(OHQMeasurementRecordKey.UserIndexKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.BloodPressureMeasurementStatusKey)) {
            EnumSet<OHQBloodPressureMeasurementStatus> measurementStatus = Types.autoCast(record.get(OHQMeasurementRecordKey.BloodPressureMeasurementStatusKey));
            if (!measurementStatus.isEmpty()) {
                items.add(new MultipleLineItem("Blood Pressure Measurement Status"));
                for (OHQBloodPressureMeasurementStatus status : measurementStatus) {
                    items.add(new MultipleLineItem(status.description(), null));
                }
            }
        }
        if (record.containsKey(OHQMeasurementRecordKey.BMIKey)) {
            items.add(new MultipleLineItem("BMI"));
            items.add(new MultipleLineItem(Common.getDecimalString((BigDecimal) record.get(OHQMeasurementRecordKey.BMIKey), 0), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.BodyFatPercentageKey)) {
            items.add(new MultipleLineItem("Body Fat Percentage"));
            items.add(new MultipleLineItem(Common.getPercentStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.BodyFatPercentageKey), 0), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.BasalMetabolismKey)) {
            items.add(new MultipleLineItem("Basal Metabolism"));
            items.add(new MultipleLineItem(Common.getNumberStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.BasalMetabolismKey), "kJ"), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.MusclePercentageKey)) {
            items.add(new MultipleLineItem("Muscle Percentage"));
            items.add(new MultipleLineItem(Common.getPercentStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.MusclePercentageKey), 0), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.MuscleMassKey)) {
            items.add(new MultipleLineItem("Muscle Mass"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.MuscleMassKey), 0, (String) record.get(OHQMeasurementRecordKey.WeightUnitKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.FatFreeMassKey)) {
            items.add(new MultipleLineItem("Fat Free Mass"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.FatFreeMassKey), 0, (String) record.get(OHQMeasurementRecordKey.WeightUnitKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.SoftLeanMassKey)) {
            items.add(new MultipleLineItem("Soft Free Mass"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.SoftLeanMassKey), 0, (String) record.get(OHQMeasurementRecordKey.WeightUnitKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.BodyWaterMassKey)) {
            items.add(new MultipleLineItem("Body Water Mass"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.BodyWaterMassKey), 0, (String) record.get(OHQMeasurementRecordKey.WeightUnitKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.ImpedanceKey)) {
            items.add(new MultipleLineItem("Impedance"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.ImpedanceKey), 0, "??"), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.SkeletalMusclePercentageKey)) {
            items.add(new MultipleLineItem("Skeleton Muscle Percentage"));
            items.add(new MultipleLineItem(Common.getPercentStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.SkeletalMusclePercentageKey), 0), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.VisceralFatLevelKey)) {
            items.add(new MultipleLineItem("Visceral Fat Level"));
            items.add(new MultipleLineItem(Common.getDecimalString((BigDecimal) record.get(OHQMeasurementRecordKey.VisceralFatLevelKey), 0), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.BodyAgeKey)) {
            items.add(new MultipleLineItem("Body Age"));
            items.add(new MultipleLineItem(Common.getNumberString((BigDecimal) record.get(OHQMeasurementRecordKey.BodyAgeKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.BodyFatPercentageStageEvaluationKey)) {
            items.add(new MultipleLineItem("Body Fat Percentage Stage Evaluation"));
            items.add(new MultipleLineItem(Common.getNumberString((BigDecimal) record.get(OHQMeasurementRecordKey.BodyFatPercentageStageEvaluationKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.SkeletalMusclePercentageStageEvaluationKey)) {
            items.add(new MultipleLineItem("Skeletal Muscle Percentage Stage Evaluation"));
            items.add(new MultipleLineItem(Common.getNumberString((BigDecimal) record.get(OHQMeasurementRecordKey.SkeletalMusclePercentageStageEvaluationKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.VisceralFatLevelStageEvaluationKey)) {
            items.add(new MultipleLineItem("Visceral Fat Level Stage Evaluation"));
            items.add(new MultipleLineItem(Common.getNumberString((BigDecimal) record.get(OHQMeasurementRecordKey.VisceralFatLevelStageEvaluationKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.WeightKey)) {
            items.add(new MultipleLineItem("Weight"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.WeightKey), 0, (String) record.get(OHQMeasurementRecordKey.WeightUnitKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.HeightKey)) {
            items.add(new MultipleLineItem("Height"));
            items.add(new MultipleLineItem(Common.getDecimalStringWithUnit((BigDecimal) record.get(OHQMeasurementRecordKey.HeightKey), 0, (String) record.get(OHQMeasurementRecordKey.HeightUnitKey)), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.BodyTemperatureKey)) {
            items.add(new MultipleLineItem("Body Temperature"));
            items.add(new MultipleLineItem((String)record.get(OHQMeasurementRecordKey.BodyTemperatureKey).toString() + (String) record.get(OHQMeasurementRecordKey.BodyTemperatureUnitKey), null));
        }
        if (record.containsKey(OHQMeasurementRecordKey.BodyTemperatureTypeKey)) {
            items.add(new MultipleLineItem("Measurement Site"));
            items.add(new MultipleLineItem((String)record.get(OHQMeasurementRecordKey.BodyTemperatureTypeKey), null));
        }
        return items;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_result, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.export) {
            mTapped = true;

            //save logcat
            Calendar cal = Calendar.getInstance();
            mZipFileName = String.format(Locale.US, "%1$04d", cal.get(Calendar.YEAR));
            mZipFileName += String.format(Locale.US, "%1$02d", cal.get(Calendar.MONTH) + 1);
            mZipFileName += String.format(Locale.US, "%1$02d", cal.get(Calendar.DATE));
            mZipFileName += String.format(Locale.US, "%1$02d", cal.get(Calendar.HOUR_OF_DAY));
            mZipFileName += String.format(Locale.US, "%1$02d", cal.get(Calendar.MINUTE));
            mZipFileName += String.format(Locale.US, "%1$02d", cal.get(Calendar.SECOND));

            String tempDateTimePath = HistoryManager.sharedInstance().getDirectoryPath(getContext()) + "/temp/" + mZipFileName;
            File tempDir = new File(tempDateTimePath);
            tempDir.mkdirs();

            String fileName = "log_" + mZipFileName + ".txt";
            mPathExportLog = tempDateTimePath + "/" + fileName;
            saveLog(getContext(), mPathExportLog);

            LoaderManager.getInstance(this).restartLoader(LOADER_ID_EXPORT_RESULT, null, mAsyncLoaderStringClass);
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendMail(String fileName) {
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

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_LOAD_HISTORY, null, mAsyncLoaderHistoryDataListClass);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID_REMOVE_EXPORTING_RESULT, null, mAsyncLoaderStringClass);
    }

    @NonNull
    @Override
    protected String onGetTitle() {
        return getString(R.string.result).toUpperCase();
    }



    private void saveLog(@NonNull Context context, String fullPath) {
        AppLog.vMethodIn();
        List<String> logList = mLogViewService.getLog();

        AppLog.d("Log file:" + fullPath);

        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullPath), "UTF-8"));
        } catch (IOException ioe) {
            return;
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
            return;
        }
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private static class ExportResultFileLoader extends AsyncTaskLoader<String> {
        private Operation mOperation;
        private HistoryData mHistoryData;
        private List<HistoryData> mHistoryDataList;
        private String mFilePath;
        private String mLogPath;
        ExportResultFileLoader(Context context, Operation operation, HistoryData historyData, String filePath,
                               List<HistoryData> historyDataList, String logPath) {
            super(context);
            mOperation = operation;
            mHistoryData = historyData;
            mHistoryDataList = historyDataList;
            mFilePath = filePath;
            mLogPath = logPath;
        }

        @Override
        public String loadInBackground() {
            String resultDataPath = null;

            AppLog.vMethodIn();
            switch (mOperation) {
                case Compress:
                    if (mFilePath != null) {
                        ExportResultFileManager.sharedInstance().deleteTempFile(mFilePath);
                    }
                    resultDataPath = ExportResultFileManager.sharedInstance().ExportCompressedFile(getContext(),
                                                                                                   mHistoryData,
                                                                                                   OVERLAP_ACTION_OVERWRITE,
                                                                                                   mHistoryDataList,
                                                                                                   mLogPath);
                    break;
                case Delete:
                    ExportResultFileManager.sharedInstance().deleteTempFile(mFilePath);
                    break;
            }
            AppLog.vMethodOut();

            return resultDataPath;
        }

        @Override
        public void onCanceled(String data) {
            super.onCanceled(data);
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            super.onStopLoading();
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
        }

        enum Operation {
            Compress, Delete
        }
    }

    class AsyncLoaderStringClass implements LoaderManager.LoaderCallbacks<String> {
        public AsyncLoaderStringClass() {
        }

        @Override
        public Loader<String> onCreateLoader(int id, Bundle args) {
            AppLog.vMethodIn();
            Loader<String> loader = null;
            switch (id) {
                case LOADER_ID_EXPORT_RESULT:
                    if (mHistoryData != null) {
                        mProgressDialog.show(getFragmentManager());
                        loader = new ExportResultFileLoader(getContext(),
                                                            ExportResultFileLoader.Operation.Compress,
                                                            mHistoryData,
                                                            mPathExportFile,
                                                            mHistoryDataList,
                                                            mPathExportLog);
                    }
                    break;
                case LOADER_ID_REMOVE_EXPORTING_RESULT:
                    if (mPathExportFile != null) {
                        loader = new ExportResultFileLoader(getContext(),
                                                            ExportResultFileLoader.Operation.Delete,
                                                            null,
                                                            mPathExportFile,
                                                            mHistoryDataList,
                                                            mPathExportLog);
                    }
                    break;
            }
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String data) {
            AppLog.vMethodIn();
            mPathExportFile = data;
            mProgressDialog.dismissAllowingStateLoss();
            //onLoadFinished() will be called twice when Fragment is re-created. This is a bug of OS.
            //This code prevents sendMail() from being called except when the button is tapped
            if (mPathExportFile != null && mTapped) {
                sendMail(mPathExportFile);
                mTapped = false;
            }
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
        }
    }

    class AsyncLoaderHistoryDataListClass implements LoaderManager.LoaderCallbacks<List<HistoryData>> {
        public AsyncLoaderHistoryDataListClass() {
        }

        @Override
        public Loader<List<HistoryData>> onCreateLoader(int id, Bundle args) {
            Loader<List<HistoryData>> loader = null;
            switch (id) {
                case LOADER_ID_LOAD_HISTORY:
                    loader = new HistoryManager.HistoryLoader(getContext(), HistoryManager.HistoryLoader.Operation.Load);
                    break;
                case LOADER_ID_REMOVE_HISTORY:
                    loader = new HistoryManager.HistoryLoader(getContext(), HistoryManager.HistoryLoader.Operation.Remove);
                    break;
            }
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<HistoryData>> loader, List<HistoryData> data) {
            mHistoryDataList = data;
            Bundle args = new Bundle();
            args.putSerializable(ARG_HISTORY_DATA_LIST, (Serializable) mHistoryDataList);
        }

        @Override
        public void onLoaderReset(Loader<List<HistoryData>> loader) {
        }
    }
}
