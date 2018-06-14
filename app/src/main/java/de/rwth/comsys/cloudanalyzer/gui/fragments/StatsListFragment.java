package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.*;
import de.rwth.comsys.cloudanalyzer.gui.util.*;

import java.util.*;

import static java.util.Calendar.DAY_OF_YEAR;

public class StatsListFragment extends Fragment
{
    public static final String TAG = "StatsListFragment";

    final Calendar maxCal = Calendar.getInstance();
    final Calendar minCal = Calendar.getInstance();
    boolean emptyData = false;
    android.util.Pair<Float, Float> totalTraffic, totalCloudTraffic;
    View view;
    private android.os.AsyncTask<String, Void, Object> getDataTask;

    public StatsListFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        minCal.setTimeInMillis(getArguments().getLong("start"));
        maxCal.setTimeInMillis(getArguments().getLong("end"));
        emptyData = getArguments().getBoolean("emptyData", true);
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_stats_list, container, false);
        ListView listView = view.findViewById(R.id.lVStatsList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id)
            {
                Intent intent;
                // start activity with name of app/service/region chosen from fragment specific to this activity
                switch (getArguments().getString("content").toLowerCase())
                {
                    case "services":
                        intent = new Intent(getActivity(), ServicesActivity.class);
                        intent.putExtra("name", (ServiceEntry) adapter.getItemAtPosition(position));
                        intent.putExtra("min", minCal.getTimeInMillis());
                        intent.putExtra("max", maxCal.getTimeInMillis());
                        startActivityForResult(intent, 2);
                        break;

                    case "regions":
                        intent = new Intent(getActivity(), RegionsActivity.class);
                        intent.putExtra("regionEntry", (RegionEntry) adapter.getItemAtPosition(position));
                        intent.putExtra("min", minCal.getTimeInMillis());
                        intent.putExtra("max", maxCal.getTimeInMillis());
                        startActivityForResult(intent, 2);
                        break;

                    case "settings":
                        intent = new Intent(getActivity(), SettingsActivity.class);
                        intent.putExtra("settingsEntry", (SettingsListAdapter.SettingsEntry) adapter.getItemAtPosition(position));
                        startActivity(intent);
                        break;

                    case "imprint":
                        break;

                    case "appservice":
                        intent = new Intent(getActivity(), AppServiceDetailActivity.class);
                        intent.putExtra("serviceEntry", ((ServiceEntry) adapter.getItemAtPosition(position)));
                        intent.putExtra("appEntry", getArguments().getSerializable("appEntry"));
                        intent.putExtra("min", minCal.getTimeInMillis());
                        intent.putExtra("max", maxCal.getTimeInMillis());
                        startActivityForResult(intent, 2);
                        break;

                    case "appserviceregion":
                        intent = new Intent(getActivity(), AppServiceRegionDetailActivity.class);
                        intent.putExtra("serviceID", ((ServiceEntry) adapter.getItemAtPosition(position)).getServiceID());
                        intent.putExtra("serviceName", ((ServiceEntry) adapter.getItemAtPosition(position)).getServiceName());
                        intent.putExtra("appEntry", getArguments().getSerializable("appEntry"));
                        intent.putExtra("region", ((ServiceEntry) adapter.getItemAtPosition(position)).getRegion());
                        intent.putExtra("min", minCal.getTimeInMillis());
                        intent.putExtra("max", maxCal.getTimeInMillis());
                        startActivityForResult(intent, 2);
                        break;

                    // removing these case results in crashes because this navigation is not supported
                    case "appservicecoherence":
                        break;

                    case "serviceapp":
                        break;

                    case "regionapp":
                        break;

                    case "regionservice":
                        break;

                    default:
                        intent = new Intent(getActivity(), AppsActivity.class);
                        intent.putExtra("name", (AppEntry) adapter.getItemAtPosition(position));
                        intent.putExtra("min", minCal.getTimeInMillis());
                        intent.putExtra("max", maxCal.getTimeInMillis());
                        startActivityForResult(intent, 2);
                        break;
                }
            }
        });
        return view;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null)
        {
            Long minCalVal = savedInstanceState.getLong("minCalVal");
            Long maxCalVal = savedInstanceState.getLong("maxCalVal");
            if (minCalVal != 0 && maxCalVal != 0)
            {
                // set global calendars to new times
                minCal.setTimeInMillis(minCalVal);
                maxCal.setTimeInMillis(maxCalVal);
            }
            if (CaptureCentral.isMainHandlerInitialized())
            {
                getDataTask = new DatabaseGetList(getActivity()).execute(getArguments().getString("content").toLowerCase(), "true");
            }
        }
        else
        {
            if (CaptureCentral.isMainHandlerInitialized())
            {
                getDataTask = new DatabaseGetList(getActivity()).execute(getArguments().getString("content").toLowerCase(), "false");
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putLong("minCalVal", minCal.getTimeInMillis());
        outState.putLong("maxCalVal", maxCal.getTimeInMillis());
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (getDataTask != null)
            getDataTask.cancel(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    public void update(long start, long end, boolean cloud)
    {
        minCal.setTimeInMillis(start);
        maxCal.setTimeInMillis(end);
        if (cloud)
        {
            switch (getArguments().getString("content").toLowerCase())
            {
                case "appservice":
                {
                    totalTraffic = CloudUsageAccessor.getAppTraffic(((AppEntry) getArguments().getSerializable("appEntry")).getId(), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    break;
                }
                case "appservicecoherence":
                {
                    totalTraffic = CloudUsageAccessor.getAppTraffic(getArguments().getInt("appID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    totalCloudTraffic = CloudUsageAccessor.getAppServiceTraffic(getArguments().getInt("appID"), getArguments().getInt("serviceID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    break;
                }
                case "appserviceregion":
                {
                    totalTraffic = CloudUsageAccessor.getAppTraffic(((AppEntry) getArguments().getSerializable("appEntry")).getId(), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    break;
                }
                case "serviceapp":
                {
                    totalTraffic = CloudUsageAccessor.getServiceTraffic(getArguments().getInt("serviceID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    break;
                }
                case "regionapp":
                {
                    totalTraffic = CloudUsageAccessor.getRegionTraffic(getArguments().getInt("regionID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    break;
                }
                case "regionservice":
                {
                    totalTraffic = CloudUsageAccessor.getRegionTraffic(getArguments().getInt("regionID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    break;
                }
                default:
                    totalTraffic = CloudUsageAccessor.getTotalTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    totalCloudTraffic = CloudUsageAccessor.getTotalCloudTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
            }
        }
        else
        {
            totalTraffic = CloudUsageAccessor.getTotalTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
            totalCloudTraffic = CloudUsageAccessor.getTotalCloudTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
        }
        getDataTask = new DatabaseGetList(getActivity()).execute(getArguments().getString("content").toLowerCase(), "true");
    }

    private class DatabaseGetList extends AsyncTaskProgressDialog<String, Void, Object>
    {
        float appMaxTraffic;
        private String content;

        private DatabaseGetList(Activity activity)
        {
            super(activity);
        }

        @Override
        protected Object doInBackground(String... table)
        {
            content = table[0];

            switch (table[0])
            {
                case "services":
                {
                    totalTraffic = CloudUsageAccessor.getTotalTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    totalCloudTraffic = CloudUsageAccessor.getTotalCloudTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    return CloudUsageAccessor.getSortedServices(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                }

                case "regions":
                {
                    totalTraffic = CloudUsageAccessor.getTotalTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    totalCloudTraffic = CloudUsageAccessor.getTotalCloudTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    return CloudUsageAccessor.getSortedRegions(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                }

                case "settings":
                {
                    List<String> list = new ArrayList<>();
                    String[] stringList = new String[]{"SettingsNotificationFragment", "SettingsMiscFragment", "SettingsDatabaseFragment", "DebugStatisticsFragment", "DebugConnectionsFragment"};
                    Collections.addAll(list, stringList);
                    return list;
                }

                case "imprint":
                {
                    List<String> list = new ArrayList<>();
                    String[] stringList = new String[]{};
                    Collections.addAll(list, stringList);
                    return list;
                }

                case "appservice":
                {
                    if (table[1].equals("true"))
                    {
                        totalTraffic = CloudUsageAccessor.getAppTraffic(((AppEntry) getArguments().getSerializable("appEntry")).getId(), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    }
                    else
                    {
                        totalTraffic = new android.util.Pair<>(getArguments().getFloat("upTraffic"), getArguments().getFloat("downTraffic"));
                    }
                    return CloudUsageAccessor.getSortedAppServices(((AppEntry) getArguments().getSerializable("appEntry")).getId(), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                }

                case "appservicecoherence":
                {
                    if (table[1].equals("true"))
                    {
                        totalTraffic = CloudUsageAccessor.getAppTraffic(getArguments().getInt("appID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                        totalCloudTraffic = CloudUsageAccessor.getAppServiceTraffic(getArguments().getInt("appID"), getArguments().getInt("serviceID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    }
                    else
                    {
                        totalTraffic = new android.util.Pair<>(getArguments().getFloat("upTraffic"), getArguments().getFloat("downTraffic"));
                        totalCloudTraffic = new android.util.Pair<>(getArguments().getFloat("serviceUpTraffic"), getArguments().getFloat("serviceDownTraffic"));
                    }
                    return CloudUsageAccessor.getSortedAppServiceCoherence(getArguments().getInt("appID"), getArguments().getInt("serviceID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                }

                case "appserviceregion":
                {
                    if (table[1].equals("true"))
                    {
                        totalTraffic = CloudUsageAccessor.getAppTraffic(((AppEntry) getArguments().getSerializable("appEntry")).getId(), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    }
                    else
                    {
                        totalTraffic = new android.util.Pair<>(getArguments().getFloat("upTraffic"), getArguments().getFloat("downTraffic"));
                    }
                    return CloudUsageAccessor.getSortedAppServicesRegion(((AppEntry) getArguments().getSerializable("appEntry")).getId(), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                }

                case "serviceapp":
                {
                    if (table[1].equals("true"))
                    {
                        totalTraffic = CloudUsageAccessor.getServiceTraffic(getArguments().getInt("serviceID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    }
                    else
                    {
                        totalTraffic = new android.util.Pair<>(getArguments().getFloat("upTraffic"), getArguments().getFloat("downTraffic"));
                    }
                    return CloudUsageAccessor.getSortedServicesApps(getArguments().getInt("serviceID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR), getContext());
                }

                case "regionapp":
                {
                    if (table[1].equals("true"))
                    {
                        totalTraffic = CloudUsageAccessor.getRegionTraffic(getArguments().getInt("regionID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    }
                    else
                    {
                        totalTraffic = new android.util.Pair<>(getArguments().getFloat("upTraffic"), getArguments().getFloat("downTraffic"));
                    }
                    return CloudUsageAccessor.getSortedRegionsApps(getArguments().getInt("regionID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR), getContext());
                }

                case "regionservice":
                {
                    if (table[1].equals("true"))
                    {
                        totalTraffic = CloudUsageAccessor.getRegionTraffic(getArguments().getInt("regionID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    }
                    else
                    {
                        totalTraffic = new android.util.Pair<>(getArguments().getFloat("upTraffic"), getArguments().getFloat("downTraffic"));
                    }
                    return CloudUsageAccessor.getSortedRegionsServices(getArguments().getInt("regionID"), minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR), getContext());
                }

                default:
                {
                    totalTraffic = CloudUsageAccessor.getTotalTraffic(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));
                    ArrayList<AppEntry> sortedApps = CloudUsageAccessor.getSortedApps(minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR), getContext());

                    List<AppEntry> list = new ArrayList<>();

                    int i = 0;
                    for (AppEntry a : sortedApps)
                    {
                        if (a != null)
                        {
                            list.add(a);
                        }
                        i++;
                    }

                    if (list.size() > 0)
                    {
                        appMaxTraffic = list.get(0).getTotalTraffic();
                    }
                    else
                    {
                        appMaxTraffic = 0;
                    }
                    return list;
                }
            }

        }

        @Override
        protected void onPostExecute(Object results)
        {
            super.onPostExecute(results);

            if (getActivity() == null)
            {
                cancel(true);
            }
            else
            {
                Locale locale = getResources().getConfiguration().locale;
                try
                {
                    if (((List<ServiceEntry>) results).isEmpty())
                    {
                        emptyData = true;
                    }
                    else
                    {
                        emptyData = false;
                    }
                    getView().findViewById(R.id.textViewCloud).setVisibility(View.GONE);
                    getView().findViewById(R.id.scrollView).setVisibility(View.GONE);
                    getView().findViewById(R.id.textView).setVisibility(View.GONE);
                    getView().findViewById(R.id.lVStatsList).setVisibility(View.VISIBLE);
                    switch (content)
                    {
                        case "services":
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new ServiceListAdapter((List<ServiceEntry>) results, locale, totalTraffic.first, totalTraffic.second, totalCloudTraffic.first, totalCloudTraffic.second, true, false));
                            if (emptyData)
                            {
                                getView().findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
                                getView().findViewById(R.id.scrollView).setPadding(0, 650, 0, 0);
                                getView().findViewById(R.id.textView).setVisibility(View.VISIBLE);
                            }
                            break;

                        case "regions":
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new RegionListAdapter((List<RegionEntry>) results, locale, totalTraffic.first, totalTraffic.second, totalCloudTraffic.first, totalCloudTraffic.second, true));
                            if (emptyData)
                            {
                                getView().findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
                                getView().findViewById(R.id.scrollView).setPadding(0, 650, 0, 0);
                                getView().findViewById(R.id.textView).setVisibility(View.VISIBLE);
                            }
                            break;

                        case "settings":
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new SettingsListAdapter((List<String>) results, getContext()));
                            break;

                        case "imprint":
                            getView().findViewById(R.id.lVStatsList).setVisibility(View.GONE);
                            getView().findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
                            TextView imprint = getView().findViewById(R.id.textView);
                            imprint.setVisibility(View.VISIBLE);
                            imprint.setText(R.string.imprint_text);
                            Linkify.addLinks(imprint, Linkify.ALL);
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new SettingsListAdapter((List<String>) results, getContext()));
                            break;

                        case "appservice":
                            if (((List<ServiceEntry>) results).isEmpty())
                            {
                                getView().findViewById(R.id.textViewCloud).setVisibility(View.VISIBLE);
                            }
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new ServiceListAdapter((List<ServiceEntry>) results, locale, totalTraffic.first, totalTraffic.second, 0, 0, false, false));
                            break;

                        case "appservicecoherence":
                            if (((List<ServiceEntry>) results).isEmpty())
                            {
                                getView().findViewById(R.id.textViewCloud).setVisibility(View.VISIBLE);
                            }
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new ServiceListAdapter((List<ServiceEntry>) results, locale, totalTraffic.first, totalTraffic.second, totalCloudTraffic.first, totalCloudTraffic.second, true, true));
                            break;

                        case "appserviceregion":
                            if (((List<ServiceEntry>) results).isEmpty())
                            {
                                getView().findViewById(R.id.textViewCloud).setVisibility(View.VISIBLE);
                            }
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new ServiceListAdapter((List<ServiceEntry>) results, locale, totalTraffic.first, totalTraffic.second, 0, 0, false, false));
                            break;

                        case "serviceapp":
                            if (((List<ServiceEntry>) results).isEmpty())
                            {
                                getView().findViewById(R.id.textViewCloud).setVisibility(View.VISIBLE);
                            }
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new CloudAppListAdapter((List<CloudAppEntry>) results, locale, totalTraffic.first, totalTraffic.second));
                            break;

                        case "regionapp":
                            if (((List<ServiceEntry>) results).isEmpty())
                            {
                                getView().findViewById(R.id.textViewCloud).setVisibility(View.VISIBLE);
                            }
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new CloudAppListAdapter((List<CloudAppEntry>) results, locale, totalTraffic.first, totalTraffic.second));
                            break;

                        case "regionservice":
                            if (((List<ServiceEntry>) results).isEmpty())
                            {
                                getView().findViewById(R.id.textViewCloud).setVisibility(View.VISIBLE);
                            }
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new ServiceListAdapter((List<ServiceEntry>) results, locale, totalTraffic.first, totalTraffic.second, 0, 0, false, true));
                            break;


                        default:
                            ((ListView) getView().findViewById(R.id.lVStatsList)).setAdapter(new AppListAdapter((List<AppEntry>) results, locale, appMaxTraffic));
                            if (emptyData)
                            {
                                getView().findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
                                getView().findViewById(R.id.scrollView).setPadding(0, 650, 0, 0);
                                getView().findViewById(R.id.textView).setVisibility(View.VISIBLE);
                            }
                            break;
                    }
                }
                catch (NullPointerException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }
}
