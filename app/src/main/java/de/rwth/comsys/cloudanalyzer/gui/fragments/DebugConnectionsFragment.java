package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.util.MapAdapter;

public class DebugConnectionsFragment extends Fragment
{

    public DebugConnectionsFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_debug_connections, container, false);

        Button button = view.findViewById(R.id.btRefresh);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!CaptureCentral.getInstance().isActive())
                {
                    Toast.makeText(getActivity(), getString(R.string.inactive), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    ListView lVConnections = getActivity().findViewById(R.id.lVConnections);
                    lVConnections.setAdapter(new MapAdapter<>(CaptureCentral.getInstance().getForwarderManager().getForwarder()));
                }
            }
        });

        return view;
    }
}
