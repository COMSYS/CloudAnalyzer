package de.rwth.comsys.cloudanalyzer.gui.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import de.rwth.comsys.cloudanalyzer.R;

import java.util.ArrayList;
import java.util.Map;

public class MapAdapter<S, T> extends BaseAdapter
{
    private final ArrayList mData;

    public MapAdapter(Map<S, T> map)
    {
        mData = new ArrayList();
        mData.addAll(map.entrySet());
    }

    @Override
    public int getCount()
    {
        return mData.size();
    }

    @Override
    public Map.Entry<S, T> getItem(int position)
    {
        return (Map.Entry) mData.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final View result;

        if (convertView == null)
        {
            result = LayoutInflater.from(parent.getContext()).inflate(R.layout.map_adapter_item, parent, false);
        }
        else
        {
            result = convertView;
        }

        Map.Entry<S, T> item = getItem(position);

        String key = item.getKey().toString();
        String value = item.getValue().toString();
        ((TextView) result.findViewById(R.id.lblKey)).setText(key);
        ((TextView) result.findViewById(R.id.lblValue)).setText(value);

        return result;
    }
}