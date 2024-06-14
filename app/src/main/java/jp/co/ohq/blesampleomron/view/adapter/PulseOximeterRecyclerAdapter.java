package jp.co.ohq.blesampleomron.view.adapter;

import android.content.Context;
import androidx.annotation.NonNull;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jp.co.ohq.ble.enumerate.OHQMeasurementRecordKey;
import jp.co.ohq.blesampleomron.R;
import jp.co.ohq.blesampleomron.controller.util.Common;
import jp.co.ohq.blesampleomron.controller.util.AppLog;

public class PulseOximeterRecyclerAdapter extends AbstractRecyclerAdapter<Map<OHQMeasurementRecordKey, Object>, PulseOximeterRecyclerAdapter.ViewHolder> {
    private final String mTimeStampAll0;

    public PulseOximeterRecyclerAdapter(@NonNull Context context, @NonNull List<Map<OHQMeasurementRecordKey, Object>> measurements) {
        super(context, R.layout.measurement_pulse_oximeter, measurements);
        mTimeStampAll0 = context.getResources().getString(R.string.time_stamp_all_0);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(mResourceId, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Map<OHQMeasurementRecordKey, Object> measurementRecord = mObjects.get(position);
        AppLog.i(measurementRecord.toString());
        if (measurementRecord.containsKey(OHQMeasurementRecordKey.TimeStampKey)) {
            holder.timestamp.setText((String) measurementRecord.get(OHQMeasurementRecordKey.TimeStampKey));
        }else{
            holder.timestamp.setText(mTimeStampAll0);
        }

        holder.spo2.setText(Common.getDecimalString((BigDecimal) measurementRecord.get(OHQMeasurementRecordKey.PulseOximeterSpo2Key), 0));
        holder.pulseRate.setText(Common.getDecimalString((BigDecimal) measurementRecord.get(OHQMeasurementRecordKey.PulseRateKey), 0));
    }

    class ViewHolder extends AbstractRecyclerAdapter.ViewHolder {
        TextView timestamp;
        TextView spo2;
        TextView pulseRate;

        ViewHolder(View itemView) {
            super(itemView);
            timestamp               = (TextView) itemView.findViewById(R.id.timestamp);
            spo2                    = (TextView) itemView.findViewById(R.id.spo2);
            pulseRate               = (TextView) itemView.findViewById(R.id.pulse_rate);
        }
    }
}