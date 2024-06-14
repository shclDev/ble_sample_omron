package jp.co.ohq.blesampleomron.model.system;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import jp.co.ohq.blesampleomron.controller.util.AppLog;
import jp.co.ohq.blesampleomron.model.entity.HistoryData;
import jp.co.ohq.utility.StringEx;

public class HistoryManager {

    private static final String RESULT_DIR_NAME = "Result";
    private static final String RESULT_DATA_JSON_FILE_NAME = "resultData.json";
    private final static String LOGCAT_LOG_FILE_NAME = "logcat.log";

    @Nullable
    private static HistoryManager sInstance;

    private HistoryManager(@NonNull Context context) {
    }

    public static HistoryManager init(@NonNull Context context) {
        if (null != sInstance) {
            throw new IllegalStateException("An instance has already been created.");
        }
        return sInstance = new HistoryManager(context);
    }

    public static HistoryManager sharedInstance() {
        if (null == sInstance) {
            throw new IllegalStateException("Instance has not been created.");
        }
        return sInstance;
    }

    public void add(@NonNull Context context, @NonNull HistoryData historyData) {
        add(context, historyData, null);
    }

    public void add(@NonNull Context context, @NonNull HistoryData historyData, @Nullable List<String> logcatLogBuffer) {
        File saveDir = new File(getDirectoryPath(context) + "/" + StringEx.toDateString(historyData.getReceivedDate(), StringEx.FormatType.Form3));
        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new IllegalAccessError("Failed to make directory '" + saveDir + "'");
            }
        }
        saveHistoryData(saveDir.getAbsolutePath(), historyData);
        if (null != logcatLogBuffer) {
            saveLogcatLog(saveDir.getAbsolutePath(), logcatLogBuffer);
        }
    }

    @NonNull
    public List<HistoryData> getAll(@NonNull Context context) {
        AppLog.vMethodIn();
        deleteTempDirectory(context);
        File resultDir = new File(getDirectoryPath(context));
        if (!resultDir.isDirectory()) {
            throw new RuntimeException("!resultDir.isDirectory() '" + resultDir + "'");
        }

        File[] receivedTimeDirs = resultDir.listFiles();
        if (null == receivedTimeDirs) {
            throw new RuntimeException("null == receivedTimeDirs");
        }

        List<HistoryData> historyDataList = new LinkedList<>();
        for (File receivedTimeDir : receivedTimeDirs) {
            if (receivedTimeDir.isDirectory()) {
                historyDataList.add(get(context, receivedTimeDir));
            }
        }
        return historyDataList;
    }

    public void removeAll(@NonNull Context context) {

        File resultDir = new File(getDirectoryPath(context));
        if (!resultDir.isDirectory()) {
            throw new RuntimeException("!resultDir.isDirectory() '" + resultDir + "'");
        }

        delete(resultDir);
    }

    private void delete(@NonNull File f) {
        if (!f.exists()) {
            return;
        }
        if (f.isFile()) {
            if (!f.delete()) {
                AppLog.e("Delete file failed.");
            }
        } else if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File file : files) {
                delete(file);
            }
            if (!f.delete()) {
                AppLog.e("Delete file failed.");
            }
        }
    }

    @NonNull
    private HistoryData get(@NonNull Context context, @NonNull File receivedTimeDir) {
        deleteTempDirectory(context);
        if (!receivedTimeDir.isDirectory()) {
            throw new RuntimeException("!receivedTimeDir.isDirectory()");
        }
        File[] resultDataJsonFiles = receivedTimeDir.listFiles(new ResultDataJsonFileFilter());
        if (1 < resultDataJsonFiles.length) {
            throw new RuntimeException("1 < resultDataJsonFiles.length");
        }
        File resultDataJsonFile = resultDataJsonFiles[0];
        if (!resultDataJsonFile.isFile()) {
            throw new RuntimeException("!resultDataJsonFiles.isFile()");
        }

        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(resultDataJsonFile));
            String line;
            while (null != (line = reader.readLine())) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new Gson().fromJson(sb.toString(), HistoryData.class);
    }

    @NonNull
    public String getDirectoryPath(@NonNull Context context) {
        File resultDir = new File(AppConfig.sharedInstance().getExternalApplicationDirectoryPath(context) + "/" + RESULT_DIR_NAME);
        if (!resultDir.exists()) {
            if (!resultDir.mkdirs()) {
                throw new IllegalAccessError("Failed to make directory '" + resultDir + "'");
            }
        }
        return resultDir.getAbsolutePath();
    }

    private static class ResultDataJsonFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return RESULT_DATA_JSON_FILE_NAME.equals(name);
        }
    }

    private void saveHistoryData(@NonNull String saveDirPath, @NonNull HistoryData historyData) {
        File filePath = new File(saveDirPath + "/" + RESULT_DATA_JSON_FILE_NAME);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(new Gson().toJson(historyData));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveLogcatLog(@NonNull String saveDirPath, @NonNull List<String> log) {
        File filePath = new File(saveDirPath + "/" + LOGCAT_LOG_FILE_NAME);
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(filePath, "UTF-8");
            for (String line : log) {
                pw.println(line);
            }
            pw.flush();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    public void deleteTempDirectory(@NonNull Context context){
        File directory = new File(getDirectoryPath(context) + "/temp");
        if (directory.exists()) {
            try {
                recursiveDeleteFile(directory);
            } catch (Exception e) {
                throw new RuntimeException("recursiveDeleteFile(directory);");
            }
        }

    }

    private static void recursiveDeleteFile(final File file) throws Exception {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                recursiveDeleteFile(child);
            }
        }
        file.delete();
    }

    public static class HistoryLoader extends AsyncTaskLoader<List<HistoryData>> {

        private Operation mOperation;
        private boolean mCancelRequest;
        public HistoryLoader(@NonNull Context context, Operation operation) {
            super(context);
            mOperation = operation;
            mCancelRequest = false;
        }

        @Override
        @NonNull
        public List<HistoryData> loadInBackground() {
            AppLog.vMethodIn();
            if (Operation.Remove == mOperation) {
                HistoryManager.sharedInstance().removeAll(getContext());
            }
            AppLog.vMethodOut();
            return HistoryManager.sharedInstance().getAll(getContext());
        }

        @Override
        public void onCanceled(List<HistoryData> data) {
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
            mCancelRequest = true;
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
        }

        public enum Operation {
            Load, Remove
        }
    }

    public static class ResultDataComparator implements Comparator<HistoryData> {
        @Override
        public int compare(HistoryData data1, HistoryData data2) {
            return data1.getReceivedDate() > data2.getReceivedDate() ? -1 : 1;
        }
    }
}
