package de.rwth.comsys.cloudanalyzer.gui.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//import android.graphics.drawable.Drawable;
//import android.os.Build;
//import android.widget.ImageView;

public class SettingsListAdapter extends BaseAdapter implements Serializable
{

    List<SettingsEntry> elementList;
    private String enableDebugging;

    public SettingsListAdapter(List<String> list, Context context)
    {
        enableDebugging = MainHandler.getProperties().getProperty("ca.enableDebugging");
        this.elementList = new ArrayList<>();

        Iterator<String> it = list.iterator();
        String[] screenName = context.getResources().getStringArray(R.array.settings_menu_array);

        int i = 0;
        while (it.hasNext() && screenName.length > i)
        {
            SettingsEntry entry = new SettingsEntry(it.next(), screenName[i++]);
            // this is currently hardcoded
            elementList.add(entry);
        }

        if (elementList.size() == 0)
        {
            return;
        }
        if (enableDebugging.equalsIgnoreCase("false"))
        {
            elementList.remove(elementList.size() - 1);
            elementList.remove(elementList.size() - 1);
        }
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount()
    {
        return elementList.size();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public SettingsEntry getItem(int position)
    {
        return elementList.get(position);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position)
    {
        return position;
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final View result;

        if (convertView == null)
        {
            result = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagetext_list_adapter_item, parent, false);
        }
        else
        {
            result = convertView;
        }

        SettingsEntry item = getItem(position);

        ((TextView) result.findViewById(R.id.tvLabel)).setText(item.getScreenName());
        ((ImageView) result.findViewById(R.id.ivLogo)).setImageResource(R.drawable.ic_build);

        return result;
    }

    public class SettingsEntry implements Serializable
    {
        private String fragmentName;
        private String screenName;

        public SettingsEntry(String name, String screenName)
        {
            this.fragmentName = name;
            this.screenName = screenName;
        }

        public String getFragmentName()
        {
            return fragmentName;
        }

        public String getScreenName()
        {
            return screenName;
        }

    }
}

