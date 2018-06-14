package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.cloudanalyzer.R;

import java.text.NumberFormat;
import java.util.Locale;

public class DebugStatisticsFragment extends Fragment
{
    public DebugStatisticsFragment()
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
        View view = inflater.inflate(R.layout.fragment_debug_statistics, container, false);

        Button button = view.findViewById(R.id.btRefresh);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CaptureCentral captureCentral = CaptureCentral.getInstance();

                if (!captureCentral.isActive())
                {
                    Toast.makeText(getActivity(), getString(R.string.inactive), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    NumberFormat nf = NumberFormat.getIntegerInstance(Locale.getDefault());
                    TextView current = getActivity().findViewById(R.id.lblSizePortMapValue);
                    current.setText(nf.format(captureCentral.getAppNameManager().size()));
                    current = getActivity().findViewById(R.id.lblSizeSocketMapValue);
                    current.setText(nf.format(captureCentral.getForwarderManager().getNumberOfForwarder()));
                    current = getActivity().findViewById(R.id.lblTCPSocketCountValue);
                    current.setText(nf.format(captureCentral.getTcp_forwarders()));
                    current = getActivity().findViewById(R.id.lblUDPSocketCountValue);
                    current.setText(nf.format(captureCentral.getUdp_forwarders()));
                    current = getActivity().findViewById(R.id.lblTCPPacketCountValue);
                    current.setText(nf.format(captureCentral.getPackets_tcp_incoming() + captureCentral.getPackets_tcp_outgoing()));
                    current = getActivity().findViewById(R.id.lblUDPPacketCountValue);
                    current.setText(nf.format(captureCentral.getPackets_udp_incoming() + captureCentral.getPackets_udp_outgoing()));
                    current = getActivity().findViewById(R.id.lblTUNoutPacketCountValue);
                    current.setText(nf.format(captureCentral.getPackets_tcp_outgoing() + captureCentral.getPackets_udp_outgoing()));
                    current = getActivity().findViewById(R.id.lblTUNinPacketCountValue);
                    current.setText(nf.format(captureCentral.getPackets_tcp_incoming() + captureCentral.getPackets_udp_incoming()));
                }
            }
        });

        Button buttonClean = view.findViewById(R.id.btCleanForwarder);
        buttonClean.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CaptureCentral captureCentral = CaptureCentral.getInstance();

                if (!captureCentral.isActive())
                {
                    Toast.makeText(getActivity(), getString(R.string.inactive), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    captureCentral.getForwarderManager().cleanUpExistingForwarder(true);
                }
            }
        });

        return view;
    }
}
