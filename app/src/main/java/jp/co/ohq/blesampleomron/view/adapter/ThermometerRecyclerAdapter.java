package jp.co.ohq.blesampleomron.view.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import jp.co.ohq.ble.enumerate.OHQMeasurementRecordKey;
import jp.co.ohq.blesampleomron.R;
import jp.co.ohq.blesampleomron.controller.util.Common;


public class ThermometerRecyclerAdapter extends AbstractRecyclerAdapter<Map<OHQMeasurementRecordKey, Object>, ThermometerRecyclerAdapter.ViewHolder> {

    private final String mTimeStampAllZero;

    public ThermometerRecyclerAdapter(@NonNull Context context, @NonNull List<Map<OHQMeasurementRecordKey, Object>> objects) {
        super(context, R.layout.measurement_thermometer, objects);
        mTimeStampAllZero = context.getResources().getString(R.string.time_stamp_all_0);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(mResourceId, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Map<OHQMeasurementRecordKey, Object> measurementRecord = mObjects.get(position);
        if (measurementRecord.containsKey(OHQMeasurementRecordKey.TimeStampKey)) {
            holder.timestamp.setText((CharSequence) measurementRecord.get(OHQMeasurementRecordKey.TimeStampKey));
        }else{
            holder.timestamp.setText(mTimeStampAllZero);
        }

        holder.bodyTemperature.setText((CharSequence) measurementRecord.get(OHQMeasurementRecordKey.BodyTemperatureKey).toString());
        holder.temperatureUnit.setText((CharSequence) measurementRecord.get(OHQMeasurementRecordKey.BodyTemperatureUnitKey));
    }

    class ViewHolder extends AbstractRecyclerAdapter.ViewHolder {
        TextView timestamp;
        TextView bodyTemperature;
        TextView temperatureUnit;

        ViewHolder(View itemView) {
            super(itemView);
            timestamp = (TextView) itemView.findViewById(R.id.timestamp);
            bodyTemperature = (TextView) itemView.findViewById(R.id.bodyTemperature);
            temperatureUnit = (TextView) itemView.findViewById(R.id.temperatureUnit);
        }
    }
}
