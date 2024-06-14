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

public class BloodPressureRecyclerAdapter extends AbstractRecyclerAdapter<Map<OHQMeasurementRecordKey, Object>, BloodPressureRecyclerAdapter.ViewHolder> {
    private final String mTimeStampAll0;

    public BloodPressureRecyclerAdapter(@NonNull Context context, @NonNull List<Map<OHQMeasurementRecordKey, Object>> measurements) {
        super(context, R.layout.measurement_blood_pressure, measurements);
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

        if (measurementRecord.containsKey(OHQMeasurementRecordKey.UserIndexKey)) {
            holder.userIndex.setVisibility(View.VISIBLE);
            holder.userIndex.setText(Common.getNumberString((BigDecimal) measurementRecord.get(OHQMeasurementRecordKey.UserIndexKey)));
        }

        if (measurementRecord.containsKey(OHQMeasurementRecordKey.SequenceNumberKey)) {
            holder.sequenceNumber.setVisibility(View.VISIBLE);
            holder.sequenceNumber.setText("# " + Common.getNumberString((BigDecimal) measurementRecord.get(OHQMeasurementRecordKey.SequenceNumberKey)));
        }

        holder.systolic.setText(Common.getDecimalString((BigDecimal) measurementRecord.get(OHQMeasurementRecordKey.SystolicKey), 0));
        holder.systolicUnit.setText((CharSequence) measurementRecord.get(OHQMeasurementRecordKey.BloodPressureUnitKey));
        holder.diastolic.setText(Common.getDecimalString((BigDecimal) measurementRecord.get(OHQMeasurementRecordKey.DiastolicKey), 0));
        holder.diastolicUnit.setText((CharSequence) measurementRecord.get(OHQMeasurementRecordKey.BloodPressureUnitKey));
        holder.pulseRate.setText(Common.getDecimalString((BigDecimal) measurementRecord.get(OHQMeasurementRecordKey.PulseRateKey), 0));
    }

    class ViewHolder extends AbstractRecyclerAdapter.ViewHolder {
        TextView timestamp;
        TextView userIndex;
        TextView sequenceNumber;
        TextView systolic;
        TextView systolicUnit;
        TextView diastolic;
        TextView diastolicUnit;
        TextView pulseRate;

        ViewHolder(View itemView) {
            super(itemView);
            timestamp = (TextView) itemView.findViewById(R.id.timestamp);
            userIndex = (TextView) itemView.findViewById(R.id.userIndex);
            sequenceNumber = (TextView) itemView.findViewById(R.id.sequenceNumber);
            systolic = (TextView) itemView.findViewById(R.id.systolic);
            systolicUnit = (TextView) itemView.findViewById(R.id.systolicUnit);
            diastolic = (TextView) itemView.findViewById(R.id.diastolic);
            diastolicUnit = (TextView) itemView.findViewById(R.id.diastolicUnit);
            pulseRate = (TextView) itemView.findViewById(R.id.pulse_rate);
        }
    }
}
