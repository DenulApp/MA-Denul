package de.velcommuta.denul.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.velcommuta.denul.R;

/**
 * Custom ArrayAdapter for Nearby Connections
 */
public class NearbyConnectionListAdapter extends ArrayAdapter<NearbyConnection> {
    /**
     * Constructor
     * @param ctx Context object
     * @param connections ArrayList backing the Adapter
     */
    public NearbyConnectionListAdapter(Context ctx, ArrayList<NearbyConnection> connections) {
        super(ctx, 0, connections);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NearbyConnection conn = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.nearby_connection_list_item, parent, false);
        }
        TextView name = (TextView) convertView.findViewById(R.id.nearby_item_text);
        name.setText(conn.getName());
        return convertView;
    }
}
